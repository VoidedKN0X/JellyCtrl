package com.vknxworks.jellyctrl

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.vknxworks.jellyctrl.data.api.JellyfinClient
import com.vknxworks.jellyctrl.data.api.ServerAuthInterceptor
import com.vknxworks.jellyctrl.data.auth.SessionManager
import com.vknxworks.jellyctrl.data.crypto.DataKeyStore
import com.vknxworks.jellyctrl.data.crypto.KeystoreWrapper
import com.vknxworks.jellyctrl.data.download.DownloadRepository
import com.vknxworks.jellyctrl.data.lock.LockVault
import com.vknxworks.jellyctrl.data.settings.SettingsRepository
import com.vknxworks.jellyctrl.music.MusicController
import com.vknxworks.jellyctrl.music.MusicService
import com.vknxworks.jellyctrl.player.PlayerActivity
import com.vknxworks.jellyctrl.ui.lock.AppLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Manual dependency container; one per process. */
class AppContainer(val context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Made first on purpose: it starts reading the settings file on a background thread straight away, so that
     * read runs while the main thread builds the HTTP client below, and [lockAtStart] (which still waits for
     * the stored settings, so nothing sees defaults) usually has nothing left to wait for.
     */
    val settings = SettingsRepository(context, appScope)

    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", "JellyCtrl/${BuildConfig.VERSION_NAME} (Android)")
                    .build()
            )
        }
        // Media, image and trickplay requests to the signed-in server get the token as a header, never in the URL.
        // (runCatching: a request can start while this container is still being constructed.)
        .addInterceptor(
            ServerAuthInterceptor(
                { runCatching { sessions.active.value?.client }.getOrNull() },
                { url -> runCatching { sessions.isCleartextAllowed(url) }.getOrDefault(false) },
            )
        )
        // The same rules on every redirect hop (images, video, downloads and sign-in all use this client).
        .addNetworkInterceptor(
            com.vknxworks.jellyctrl.data.api.CredentialRedirectGuard { url -> runCatching { sessions.isCleartextAllowed(url) }.getOrDefault(false) }
        )
        .build()

    /** The app lock's PIN, fingerprint key and the key that seals the saved sign-ins while locked. */
    val vault = LockVault(context, "jellyctrl")

    /** Read once, synchronously, so the app never shows unlocked for a moment at start. */
    val lockAtStart: Boolean = vault.enabled || runBlocking { runCatching { settings.stored.first().appLock }.getOrDefault(false) }

    val sessions = SessionManager(context, http, vault, appScope)

    /**
     * The key that encrypts the app's own files at rest (the download list and the offline positions). Making
     * this reads nothing: the Keystore is first asked on a background thread, when those files are loaded.
     */
    val dataKeys = DataKeyStore(context.filesDir, KeystoreWrapper("jellyctrl_data_at_rest"))
    val downloads = DownloadRepository(context, appScope, dataKeys)

    /** Positions of downloads watched offline, sent to the server once online. */
    val offlineProgress = com.vknxworks.jellyctrl.data.download.OfflineProgressStore(context, appScope, dataKeys)

    /**
     * An encrypted file of the app (the download list or the offline positions) can't be opened. Nothing is
     * deleted or written over; the app shows what happened, with "Try again" and "Start empty".
     */
    val dataUnreadable: StateFlow<Boolean> =
        combine(downloads.unreadable, offlineProgress.unreadable) { a, b -> a || b }.stateIn(appScope, SharingStarted.Eagerly, false)

    /** "Try again": asks the Keystore for the data key once more and reads the files again. */
    suspend fun retryUnreadableData() = withContext(Dispatchers.IO) {
        dataKeys.reset()
        downloads.reload()
        offlineProgress.reload()
    }

    /**
     * "Start empty": the files that can't be opened are renamed (`.unreadable`), not deleted, and the lists start
     * empty. A data key that's gone makes way for a new one (its file is kept as `data.key.unreadable`); a key
     * that still works stays.
     */
    suspend fun startEmptyData() = withContext(Dispatchers.IO) {
        downloads.setAside()
        offlineProgress.setAside()
        if (!downloads.unreadable.value && !offlineProgress.unreadable.value && dataKeys.get(create = false) == null) {
            dataKeys.discard(keep = true)
        }
        downloads.reload()
        offlineProgress.reload()
    }
    val music by lazy { MusicController(context, this) }

    /** The app lock is on, either with a PIN or still the old one waiting for a PIN. */
    val lockEnabled: Boolean get() = vault.enabled || settings.settings.value.appLock

    /** Turns the app lock on with [pin]: the PIN seals the saved sign-ins from now on. */
    suspend fun enableLock(pin: String) {
        // Unlocked first, so the lock screen doesn't flash up while the PIN is being saved.
        AppLock.markUnlocked()
        withContext(Dispatchers.Default) { vault.setUp(pin) }
        settings.update { it.copy(appLock = true) }
        sessions.sealTokens()
    }

    /** Turns the app lock off (after the PIN was confirmed): sign-ins go back to Keystore-only encryption. */
    suspend fun disableLock() {
        sessions.unsealTokens()
        settings.update { it.copy(appLock = false) }
        vault.disable()
    }

    /** "Forgot PIN?": the sealed sign-ins can't be read any more, so forget every account and the downloads. */
    suspend fun forgetEverything() {
        if (MusicService.running) runCatching { music.stop() }
        downloads.deleteAll()
        offlineProgress.forget(null)
        sessions.forgetAllAccounts()
        AppLock.markUnlocked()
        settings.update { it.copy(appLock = false) }
        vault.disable()
    }

    /** Locked: wipe the key from memory and drop the session, unless music, a video or downloads need it. */
    fun onLocked() {
        vault.close()
        sessions.onLocked()
    }

    fun onUnlocked() {
        AppLock.markUnlocked()
        sessions.onUnlocked()
    }

    /** A link from another app (ArrCtrl's "Play in JellyCtrl") waiting to be opened once the UI is up. */
    val pendingLink = kotlinx.coroutines.flow.MutableStateFlow<com.vknxworks.jellyctrl.ui.nav.ExternalLink?>(null)

    /** Client for the signed-in account. Screens are only shown while signed in. */
    val api: JellyfinClient
        get() = sessions.active.value?.client ?: error("Not signed in")

    init {
        AppLock.onLocked = { onLocked() }
        sessions.mediaBusy = { MusicService.running || PlayerActivity.visible > 0 || downloads.busy() }
        // The last download finished while the app is locked: nothing needs the sign-in any more.
        appScope.launch {
            downloads.entries.map { downloads.busy() }.distinctUntilChanged().collect { busy -> if (!busy) sessions.mediaStopped() }
        }
    }

    fun isOnCellular(): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) &&
            !caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    fun isOnline(): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

val Context.container: AppContainer
    get() = (applicationContext as JellyCtrlApp).container

package com.vknxworks.jellyctrl

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.vknxworks.jellyctrl.data.api.JellyfinClient
import com.vknxworks.jellyctrl.data.api.ServerAuthInterceptor
import com.vknxworks.jellyctrl.data.auth.SessionManager
import com.vknxworks.jellyctrl.data.download.DownloadRepository
import com.vknxworks.jellyctrl.data.settings.SettingsRepository
import com.vknxworks.jellyctrl.data.telemetry.Telemetry
import com.vknxworks.jellyctrl.music.MusicController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Manual dependency container; one per process. */
class AppContainer(val context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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

    val sessions = SessionManager(context, http, appScope)
    val settings = SettingsRepository(context, appScope)
    val telemetry = Telemetry(context, settings, appScope)
    val downloads = DownloadRepository(context, appScope)
    val music by lazy { MusicController(context, this) }

    /** Client for the signed-in account. Screens are only shown while signed in. */
    val api: JellyfinClient
        get() = sessions.active.value?.client ?: error("Not signed in")

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

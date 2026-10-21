package com.vknxworks.jellyctrl.data.auth

import android.content.Context
import android.os.Build
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.vknxworks.jellyctrl.data.api.AuthenticationResult
import com.vknxworks.jellyctrl.data.api.JellyfinClient
import com.vknxworks.jellyctrl.data.api.PlainJson
import com.vknxworks.jellyctrl.data.api.logout
import com.vknxworks.jellyctrl.data.api.postCapabilities
import com.vknxworks.jellyctrl.data.appDataStore
import com.vknxworks.jellyctrl.data.lock.LockVault
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import java.util.UUID
import com.vknxworks.jellyctrl.R
import com.vknxworks.jellyctrl.str

@Serializable
data class Account(
    val serverId: String,
    val serverName: String,
    val serverUrl: String,
    val userId: String,
    val userName: String,
    val userImageTag: String? = null,
    val tokenEnc: String,
    val deviceId: String,
    val isAdmin: Boolean = false,
) {
    val key: String get() = "$serverId:$userId"
}

data class ActiveSession(val account: Account, val client: JellyfinClient)

/**
 * Saved servers/accounts and the currently active one. Tokens are stored encrypted with the Keystore and,
 * with the app lock on, also sealed with the app lock's key, so they can only be read while it's unlocked.
 */
class SessionManager(
    private val context: Context,
    private val http: OkHttpClient,
    private val vault: LockVault,
    private val scope: CoroutineScope,
) {
    private val accountsKey = stringPreferencesKey("accounts")
    private val activeKey = stringPreferencesKey("active_account")
    private val installIdKey = stringPreferencesKey("install_id")

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    private val _active = MutableStateFlow<ActiveSession?>(null)
    val active: StateFlow<ActiveSession?> = _active.asStateFlow()

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    /** The saved accounts have been read (their tokens may still be sealed while the app is locked). */
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    private val mutex = Mutex()

    /** The app is locked (only ever true with the app lock on). */
    @Volatile private var locked = false

    /**
     * Set by the app: whether music or a video is playing or downloads are running. Those keep the
     * signed-in session (and so its token) in memory while the app is locked, so playback, media
     * controls and downloads carry on. Everything else drops it until the next unlock.
     */
    @Volatile var mediaBusy: () -> Boolean = { false }

    private var installId: String = ""

    /** Servers (scheme://host:port) the user agreed to reach over plain HTTP during this run of the app. */
    private val cleartextApproved = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    private fun originOf(u: HttpUrl): String = "${u.scheme}://${u.host.lowercase()}:${u.port}"

    /** Lets the sign-in flow reach an http:// server once the user has confirmed the unencrypted connection. */
    fun approveCleartext(serverUrl: String) {
        JellyfinClient.normalizeBaseUrl(serverUrl).toHttpUrlOrNull()?.let { cleartextApproved += originOf(it) }
    }

    /**
     * Plain-HTTP requests that carry credentials are only allowed to a server the user explicitly chose to use
     * without encryption: a saved http:// account, or one approved in the sign-in flow.
     */
    fun isCleartextAllowed(url: HttpUrl): Boolean {
        if (url.isHttps) return true
        val origin = originOf(url)
        if (origin in cleartextApproved) return true
        return _accounts.value.any { a -> a.serverUrl.toHttpUrlOrNull()?.let { originOf(it) } == origin }
    }

    val deviceName: String = (Build.MODEL ?: "Android").trim()

    init {
        scope.launch { load() }
    }

    private suspend fun load() = mutex.withLock {
        val prefs = context.appDataStore.data.first()
        installId = prefs[installIdKey] ?: UUID.randomUUID().toString().also { id ->
            context.appDataStore.edit { it[installIdKey] = id }
        }
        val list = prefs[accountsKey]?.let {
            runCatching { PlainJson.decodeFromString(ListSerializer(Account.serializer()), it) }.getOrNull()
        }.orEmpty()
        _accounts.value = list
        _loaded.value = true
        // With the app lock on, the token can only be decrypted after unlocking; onUnlocked() continues.
        if (vault.enabled && !vault.isOpen) {
            locked = true
            return@withLock
        }
        runCatching { sealUnsealed() }
        activateSaved()
        _ready.value = true
    }

    private suspend fun activateSaved() {
        val activeId = context.appDataStore.data.first()[activeKey]
        val list = _accounts.value
        val acc = list.firstOrNull { it.key == activeId } ?: list.firstOrNull()
        if (acc != null) activate(acc) else _active.value = null
    }

    /** The token of [acc], or null while it's sealed by the app lock and the app is locked. */
    private fun tokenOf(acc: Account): String? {
        val plain = KeystoreCrypto.decrypt(acc.tokenEnc) ?: return null
        if (!LockVault.isSealed(plain)) return plain
        return vault.open(plain) ?: _active.value?.takeIf { it.account.key == acc.key }?.client?.accessToken
    }

    /** Encrypts a token for saving: with the Keystore, and also with the app lock's key when it's on. */
    private fun storeToken(token: String): String {
        val inner = if (vault.enabled) vault.seal(token) ?: error(str(R.string.unlock_jellyctrl_first)) else token
        return KeystoreCrypto.encrypt(inner)
    }

    /** The app locked: forget the session and its token until the next unlock, unless media still needs it. */
    fun onLocked() {
        if (!vault.enabled) return
        locked = true
        if (!mediaBusy()) dropSession()
    }

    /** Music, a video or the downloads stopped: if the app is locked, nothing needs the session any more. */
    fun mediaStopped() {
        if (locked && vault.enabled && !mediaBusy()) dropSession()
    }

    private fun dropSession() {
        scope.launch {
            mutex.withLock {
                if (!locked || mediaBusy()) return@withLock
                _ready.value = false
                _active.value = null
            }
        }
    }

    /** The app unlocked: decrypt the token and open the session again. */
    fun onUnlocked() {
        locked = false
        scope.launch {
            mutex.withLock {
                if (!_loaded.value) return@withLock // load() is still running and will activate it
                // If the app stopped between turning the lock on and sealing the sign-ins, they are still only
                // Keystore-encrypted: seal them now.
                runCatching { sealUnsealed() }
                if (_active.value == null) activateSaved()
                _ready.value = true
            }
        }
    }

    /** With the app lock on and unlocked, seals any saved token that is still Keystore-only. Call with [mutex] held. */
    private suspend fun sealUnsealed() {
        if (!vault.enabled || !vault.isOpen) return
        var changed = false
        val list = _accounts.value.map { acc ->
            val plain = KeystoreCrypto.decrypt(acc.tokenEnc) ?: return@map acc
            if (LockVault.isSealed(plain)) return@map acc
            val sealed = vault.seal(plain) ?: return@map acc
            changed = true
            acc.copy(tokenEnc = KeystoreCrypto.encrypt(sealed))
        }
        if (!changed) return
        persist(list, context.appDataStore.data.first()[activeKey])
        _active.value?.let { s -> list.firstOrNull { it.key == s.account.key }?.let { _active.value = s.copy(account = it) } }
    }

    /** The app lock was turned on: seal every saved token with its key too. */
    suspend fun sealTokens() = reencryptTokens(seal = true)

    /** The app lock is being turned off (while unlocked): back to Keystore-only encryption. */
    suspend fun unsealTokens() = reencryptTokens(seal = false)

    private suspend fun reencryptTokens(seal: Boolean) = mutex.withLock {
        val list = _accounts.value.map { acc ->
            val token = tokenOf(acc) ?: return@map acc
            acc.copy(tokenEnc = if (seal) storeToken(token) else KeystoreCrypto.encrypt(token))
        }
        persist(list, context.appDataStore.data.first()[activeKey])
        _active.value?.let { s -> list.firstOrNull { it.key == s.account.key }?.let { _active.value = s.copy(account = it) } }
    }

    /** "Forgot PIN?": the tokens can't be decrypted any more, so forget every account (they can't be revoked). */
    suspend fun forgetAllAccounts() = mutex.withLock {
        _active.value = null
        persist(emptyList(), null)
        clearCachedImages()
        locked = false
        _ready.value = true
    }

    /** A client with no user, for probing a server and signing in. */
    fun anonymousClient(serverUrl: String, deviceId: String = provisionalDeviceId()): JellyfinClient =
        JellyfinClient(serverUrl, null, null, deviceId, deviceName, http)

    /**
     * Jellyfin keeps one session per DeviceId, so each account gets its own id derived from the install id.
     * Otherwise signing into a second account would sign the first one out.
     */
    fun deviceIdFor(serverUrl: String, userName: String): String =
        UUID.nameUUIDFromBytes("$installId|${JellyfinClient.normalizeBaseUrl(serverUrl)}|${userName.lowercase()}".toByteArray()).toString()

    fun provisionalDeviceId(): String = UUID.nameUUIDFromBytes("$installId|probe".toByteArray()).toString()

    suspend fun addAccount(
        serverUrl: String,
        serverName: String,
        auth: AuthenticationResult,
        deviceId: String,
    ): Account {
        val user = auth.user ?: error(str(R.string.server_returned_no_user))
        val token = auth.accessToken ?: error(str(R.string.server_returned_no_access_token))
        val account = Account(
            serverId = auth.serverId ?: user.serverId ?: serverUrl,
            serverName = serverName,
            serverUrl = JellyfinClient.normalizeBaseUrl(serverUrl),
            userId = user.id,
            userName = user.name ?: str(R.string.user),
            userImageTag = user.primaryImageTag,
            tokenEnc = storeToken(token),
            deviceId = deviceId,
            isAdmin = user.policy?.isAdministrator == true,
        )
        val updated = _accounts.value.filterNot { it.key == account.key } + account
        persist(updated, account.key)
        activate(account)
        return account
    }

    suspend fun switchTo(key: String) {
        val acc = _accounts.value.firstOrNull { it.key == key } ?: return
        context.appDataStore.edit { it[activeKey] = key }
        activate(acc)
    }

    /** Signs out of the server (revoking the token) and forgets the account. */
    suspend fun signOut(key: String) {
        val acc = _accounts.value.firstOrNull { it.key == key } ?: return
        runCatching { clientFor(acc)?.logout() }
        val remaining = _accounts.value.filterNot { it.key == key }
        val nextActive = if (_active.value?.account?.key == key) remaining.firstOrNull() else _active.value?.account
        persist(remaining, nextActive?.key)
        if (nextActive != null) activate(nextActive) else _active.value = null
        clearCachedImages()
    }

    /** Posters, backdrops, profile pictures and trickplay frames of the account that was signed out. */
    private suspend fun clearCachedImages() = withContext(Dispatchers.IO) {
        runCatching {
            val loader = coil3.SingletonImageLoader.get(context)
            loader.memoryCache?.clear()
            loader.diskCache?.clear()
        }
        Unit
    }

    /** Called when the server rejects the token: drop the account so the user signs in again. */
    suspend fun invalidateActive() {
        val acc = _active.value?.account ?: return
        val remaining = _accounts.value.filterNot { it.key == acc.key }
        persist(remaining, remaining.firstOrNull()?.key)
        _active.value = remaining.firstOrNull()?.let { a -> clientFor(a)?.let { ActiveSession(a, it) } }
        clearCachedImages()
    }

    suspend fun updateAdminFlag(isAdmin: Boolean) {
        val acc = _active.value?.account ?: return
        if (acc.isAdmin == isAdmin) return
        val updated = acc.copy(isAdmin = isAdmin)
        persist(_accounts.value.map { if (it.key == acc.key) updated else it }, acc.key)
        _active.value = _active.value?.copy(account = updated)
    }

    /** A client for [acc], or null while its token is sealed by the app lock and nothing has it in memory. */
    fun clientFor(acc: Account): JellyfinClient? {
        val token = tokenOf(acc) ?: return null
        return JellyfinClient(acc.serverUrl, token, acc.userId, acc.deviceId, deviceName, http)
    }

    /** The app is locked and the token of [acc] isn't available (a download has to wait for the unlock). */
    fun isSealedNow(acc: Account): Boolean = vault.enabled && !vault.isOpen && clientFor(acc) == null

    private fun activate(acc: Account) {
        val client = clientFor(acc)
        if (client == null) {
            _active.value = null
            return
        }
        _active.value = ActiveSession(acc, client)
        scope.launch { client.postCapabilities() }
    }

    private suspend fun persist(list: List<Account>, activeId: String?) {
        _accounts.value = list
        context.appDataStore.edit { prefs ->
            prefs[accountsKey] = PlainJson.encodeToString(ListSerializer(Account.serializer()), list)
            if (activeId != null) {
                prefs[activeKey] = activeId
            } else {
                prefs.remove(activeKey)
            }
        }
    }
}

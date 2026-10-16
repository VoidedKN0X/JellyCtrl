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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import java.util.UUID

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

/** Saved servers/accounts and the currently active one. Tokens are stored encrypted with the Keystore. */
class SessionManager(
    private val context: Context,
    private val http: OkHttpClient,
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

    private suspend fun load() {
        val prefs = context.appDataStore.data.first()
        installId = prefs[installIdKey] ?: UUID.randomUUID().toString().also { id ->
            context.appDataStore.edit { it[installIdKey] = id }
        }
        val list = prefs[accountsKey]?.let {
            runCatching { PlainJson.decodeFromString(ListSerializer(Account.serializer()), it) }.getOrNull()
        }.orEmpty()
        _accounts.value = list
        val activeId = prefs[activeKey]
        val acc = list.firstOrNull { it.key == activeId } ?: list.firstOrNull()
        if (acc != null) activate(acc)
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
        val user = auth.user ?: error("Server returned no user")
        val token = auth.accessToken ?: error("Server returned no access token")
        val account = Account(
            serverId = auth.serverId ?: user.serverId ?: serverUrl,
            serverName = serverName,
            serverUrl = JellyfinClient.normalizeBaseUrl(serverUrl),
            userId = user.id,
            userName = user.name ?: "User",
            userImageTag = user.primaryImageTag,
            tokenEnc = KeystoreCrypto.encrypt(token),
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
    }

    /** Called when the server rejects the token: drop the account so the user signs in again. */
    suspend fun invalidateActive() {
        val acc = _active.value?.account ?: return
        val remaining = _accounts.value.filterNot { it.key == acc.key }
        persist(remaining, remaining.firstOrNull()?.key)
        _active.value = remaining.firstOrNull()?.let { a -> clientFor(a)?.let { ActiveSession(a, it) } }
    }

    suspend fun updateAdminFlag(isAdmin: Boolean) {
        val acc = _active.value?.account ?: return
        if (acc.isAdmin == isAdmin) return
        val updated = acc.copy(isAdmin = isAdmin)
        persist(_accounts.value.map { if (it.key == acc.key) updated else it }, acc.key)
        _active.value = _active.value?.copy(account = updated)
    }

    fun clientFor(acc: Account): JellyfinClient? {
        val token = KeystoreCrypto.decrypt(acc.tokenEnc) ?: return null
        return JellyfinClient(acc.serverUrl, token, acc.userId, acc.deviceId, deviceName, http)
    }

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

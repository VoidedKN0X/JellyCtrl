package com.vknxworks.jellyctrl.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vknxworks.jellyctrl.AppContainer
import com.vknxworks.jellyctrl.data.api.JellyfinClient
import com.vknxworks.jellyctrl.data.api.PublicSystemInfo
import com.vknxworks.jellyctrl.data.api.UserDto
import com.vknxworks.jellyctrl.data.api.authenticateByName
import com.vknxworks.jellyctrl.data.api.authenticateWithQuickConnect
import com.vknxworks.jellyctrl.data.api.publicSystemInfo
import com.vknxworks.jellyctrl.data.api.publicUsers
import com.vknxworks.jellyctrl.data.api.quickConnectEnabled
import com.vknxworks.jellyctrl.data.api.quickConnectInitiate
import com.vknxworks.jellyctrl.data.api.quickConnectState
import com.vknxworks.jellyctrl.data.auth.DiscoveredServer
import com.vknxworks.jellyctrl.data.auth.ServerDiscovery
import com.vknxworks.jellyctrl.ui.components.friendlyMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

class ServerSetupViewModel(private val c: AppContainer) : ViewModel() {

    data class Server(val url: String, val info: PublicSystemInfo)

    var address by mutableStateOf("")
    var checking by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var discovered by mutableStateOf<List<DiscoveredServer>>(emptyList())
        private set
    var discovering by mutableStateOf(false)
        private set

    var server by mutableStateOf<Server?>(null)
        private set
    var publicUsers by mutableStateOf<List<UserDto>>(emptyList())
        private set
    var quickConnectAvailable by mutableStateOf(false)
        private set

    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var signingIn by mutableStateOf(false)
        private set
    var quickConnectCode by mutableStateOf<String?>(null)
        private set

    private var qcJob: Job? = null

    init {
        discover()
    }

    fun discover() {
        if (discovering) return
        discovering = true
        viewModelScope.launch {
            discovered = ServerDiscovery.discover(c.context)
            discovering = false
        }
    }

    /** Set when https failed to connect and the server might only speak plain HTTP; the UI asks before using it. */
    var httpFallbackFor by mutableStateOf<String?>(null)
        private set

    /**
     * Try the address as typed. With no scheme, only https is tried automatically; if that can't connect
     * (and it wasn't a certificate problem) the user is asked before falling back to unencrypted http.
     */
    fun connect(input: String = address) {
        val raw = input.trim()
        if (raw.isEmpty()) return
        error = null
        httpFallbackFor = null
        val explicit = raw.startsWith("http://", true) || raw.startsWith("https://", true)
        if (explicit) {
            // An http:// address the user typed (or picked from discovery) is their choice; the sign-in
            // step shows the "Not encrypted" warning before any password is sent.
            if (raw.startsWith("http://", true)) c.sessions.approveCleartext(raw)
            probe(listOf(raw))
        } else {
            probe(listOf("https://$raw"), offerHttpFor = raw)
        }
    }

    /** The user confirmed they want to use the server over plain HTTP. */
    fun acceptHttpFallback() {
        val raw = httpFallbackFor ?: return
        httpFallbackFor = null
        val candidates = buildList {
            add("http://$raw")
            if (!raw.substringAfter("]").contains(':')) add("http://$raw:8096")
        }
        candidates.forEach { c.sessions.approveCleartext(it) }
        probe(candidates)
    }

    fun dismissHttpFallback() {
        httpFallbackFor = null
        error = "Couldn't connect securely. Check the address, or type http:// in front of it to use an unencrypted connection."
    }

    private fun probe(candidates: List<String>, offerHttpFor: String? = null) {
        error = null
        checking = true
        viewModelScope.launch {
            var lastError: Throwable? = null
            for (url in candidates) {
                try {
                    val client = c.sessions.anonymousClient(url)
                    val info = client.publicSystemInfo()
                    if (info.id.isNullOrBlank()) throw IllegalStateException("That address isn't a Jellyfin server")
                    server = Server(client.baseUrl, info)
                    publicUsers = client.publicUsers()
                    quickConnectAvailable = client.quickConnectEnabled()
                    checking = false
                    return@launch
                } catch (e: Throwable) {
                    lastError = e
                }
            }
            checking = false
            val err = lastError
            // Never downgrade after a certificate/TLS failure: that's exactly what an attacker would cause.
            val tlsProblem = generateSequence(err) { it.cause }.any { it is javax.net.ssl.SSLException || it is java.security.cert.CertificateException }
            if (offerHttpFor != null && !tlsProblem && err is java.io.IOException && err !is com.vknxworks.jellyctrl.data.api.ApiException && err !is java.net.UnknownHostException) {
                httpFallbackFor = offerHttpFor
            } else {
                error = err?.friendlyMessage() ?: "Couldn't connect"
            }
        }
    }

    fun backToServer() {
        cancelQuickConnect()
        server = null
        password = ""
        error = null
    }

    fun signIn(onDone: () -> Unit) {
        val s = server ?: return
        if (username.isBlank()) {
            error = "Enter your username"
            return
        }
        error = null
        signingIn = true
        viewModelScope.launch {
            try {
                val deviceId = c.sessions.deviceIdFor(s.url, username)
                val client = c.sessions.anonymousClient(s.url, deviceId)
                val auth = client.authenticateByName(username.trim(), password)
                c.sessions.addAccount(s.url, s.info.serverName ?: "Jellyfin", auth, deviceId)
                password = ""
                onDone()
            } catch (e: Throwable) {
                error = if ((e as? com.vknxworks.jellyctrl.data.api.ApiException)?.code == 401) {
                    "Wrong username or password"
                } else e.friendlyMessage()
            } finally {
                signingIn = false
            }
        }
    }

    fun startQuickConnect(onDone: () -> Unit) {
        val s = server ?: return
        cancelQuickConnect()
        error = null
        qcJob = viewModelScope.launch {
            try {
                val deviceId = UUID.randomUUID().toString()
                val client: JellyfinClient = c.sessions.anonymousClient(s.url, deviceId)
                val start = client.quickConnectInitiate()
                quickConnectCode = start.code
                // Stop asking after the code's lifetime instead of polling (and holding the secret) forever.
                val deadline = System.currentTimeMillis() + QUICK_CONNECT_TIMEOUT_MS
                while (isActive) {
                    delay(3000)
                    if (System.currentTimeMillis() > deadline) {
                        quickConnectCode = null
                        error = "The Quick Connect code expired. Start again to get a new one."
                        break
                    }
                    val st = client.quickConnectState(start.secret)
                    if (st.authenticated) {
                        val auth = client.authenticateWithQuickConnect(start.secret)
                        c.sessions.addAccount(s.url, s.info.serverName ?: "Jellyfin", auth, deviceId)
                        quickConnectCode = null
                        onDone()
                        break
                    }
                }
            } catch (e: Throwable) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    error = e.friendlyMessage()
                    quickConnectCode = null
                }
            }
        }
    }

    private companion object {
        const val QUICK_CONNECT_TIMEOUT_MS = 10 * 60 * 1000L
    }

    fun cancelQuickConnect() {
        qcJob?.cancel()
        qcJob = null
        quickConnectCode = null
    }
}

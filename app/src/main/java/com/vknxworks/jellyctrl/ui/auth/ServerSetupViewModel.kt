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

    /** Try the address as typed, and if it has no scheme also try http and the default port. */
    fun connect(input: String = address) {
        val raw = input.trim()
        if (raw.isEmpty()) return
        error = null
        checking = true
        viewModelScope.launch {
            val candidates = buildList {
                if (raw.startsWith("http://", true) || raw.startsWith("https://", true)) {
                    add(raw)
                } else {
                    add("https://$raw")
                    add("http://$raw")
                    if (!raw.substringAfter("]").contains(':')) add("http://$raw:8096")
                }
            }
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
            error = lastError?.friendlyMessage() ?: "Couldn't connect"
            checking = false
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
                while (isActive) {
                    delay(3000)
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

    fun cancelQuickConnect() {
        qcJob?.cancel()
        qcJob = null
        quickConnectCode = null
    }
}

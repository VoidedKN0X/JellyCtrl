package com.vknxworks.jellyctrl.data.realtime

import com.vknxworks.jellyctrl.data.api.JellyfinClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

/** One message from the server's WebSocket: its `MessageType` and its `Data`, as sent. */
data class SocketMessage(val type: String, val data: JsonElement?)

/**
 * The live connection to the signed-in Jellyfin server (`/socket`), over which the server sends remote-control
 * commands and SyncPlay updates. The access token goes in the Authorization header like every other request,
 * never in the address, and the same rules apply (no token over plain HTTP unless that was accepted for the
 * server). Reconnects by itself with growing pauses while a connection is wanted.
 */
class ServerSocket(http: OkHttpClient, private val scope: CoroutineScope) {
    // A ping every 30 seconds notices a connection that went dead without closing.
    private val client = http.newBuilder().pingInterval(30, TimeUnit.SECONDS).build()

    private val _messages = MutableSharedFlow<SocketMessage>(extraBufferCapacity = 64)
    val messages: SharedFlow<SocketMessage> = _messages.asSharedFlow()

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private var job: Job? = null
    private var target: JellyfinClient? = null

    /** Keeps a connection to [to]'s server open; null closes it. Calling it again with the same sign-in changes nothing. */
    @Synchronized
    fun connectTo(to: JellyfinClient?) {
        val same = to != null && target != null && to.baseUrl == target?.baseUrl && to.accessToken == target?.accessToken &&
            to.deviceId == target?.deviceId
        if (same && job?.isActive == true) return
        if (to == null && target == null) return
        job?.cancel()
        target = to
        _connected.value = false
        job = if (to == null || to.accessToken.isNullOrBlank()) null else scope.launch { keepOpen(to) }
    }

    private suspend fun keepOpen(to: JellyfinClient) {
        var failures = 0
        while (scope.isActive) {
            val opened = runSession(to)
            _connected.value = false
            failures = if (opened) 0 else failures + 1
            delay(RETRY_MS[failures.coerceAtMost(RETRY_MS.lastIndex)])
        }
    }

    /** One connection, from opening to its end. True when it was open at some point. */
    private suspend fun runSession(to: JellyfinClient): Boolean {
        val ended = CompletableDeferred<Boolean>()
        var opened = false
        var keepAlive: Job? = null
        val request = Request.Builder().url(to.url("socket")).header("Authorization", to.authHeader).build()
        val socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                opened = true
                _connected.value = true
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val root = runCatching { Json.parseToJsonElement(text) as? JsonObject }.getOrNull() ?: return
                val type = (root["MessageType"] as? JsonPrimitive)?.takeIf { it.isString }?.content ?: return
                val data = root["Data"]
                when (type) {
                    // The server says how long it waits before it gives up on a silent connection (seconds):
                    // answer well within that.
                    "ForceKeepAlive" -> {
                        val seconds = (data as? JsonPrimitive)?.intOrNull?.coerceIn(10, 600) ?: 60
                        keepAlive?.cancel()
                        keepAlive = scope.launch {
                            while (isActive) {
                                if (!webSocket.send(KEEP_ALIVE)) break
                                delay(seconds * 500L)
                            }
                        }
                    }
                    "KeepAlive" -> Unit
                    else -> _messages.tryEmit(SocketMessage(type, data))
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                ended.complete(opened)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                ended.complete(opened)
            }
        })
        return try {
            ended.await()
        } finally {
            keepAlive?.cancel()
            // Also when this coroutine is cancelled (signed out, locked, app left): drop the connection now.
            socket.cancel()
        }
    }

    private companion object {
        const val KEEP_ALIVE = "{\"MessageType\":\"KeepAlive\"}"
        val RETRY_MS = longArrayOf(1_000, 3_000, 10_000, 30_000, 60_000)
    }
}

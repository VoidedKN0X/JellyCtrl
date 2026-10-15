package com.vknxworks.jellyctrl.data.auth

import android.content.Context
import android.net.wifi.WifiManager
import com.vknxworks.jellyctrl.data.api.JellyfinJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

@Serializable
data class DiscoveredServer(
    val address: String = "",
    val id: String = "",
    val name: String = "",
    val endpointAddress: String? = null,
)

/** Finds Jellyfin servers on the local network using Jellyfin's UDP broadcast on port 7359. */
object ServerDiscovery {
    private const val PORT = 7359
    private const val MESSAGE = "who is JellyfinServer?"

    suspend fun discover(context: Context, timeoutMs: Int = 3000): List<DiscoveredServer> = withContext(Dispatchers.IO) {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val lock = wifi?.createMulticastLock("jellyctrl-discovery")?.apply {
            setReferenceCounted(false)
            runCatching { acquire() }
        }
        val found = linkedMapOf<String, DiscoveredServer>()
        try {
            DatagramSocket().use { socket ->
                socket.broadcast = true
                socket.soTimeout = 600
                val bytes = MESSAGE.toByteArray()
                socket.send(DatagramPacket(bytes, bytes.size, InetAddress.getByName("255.255.255.255"), PORT))
                val deadline = System.currentTimeMillis() + timeoutMs
                val buf = ByteArray(4096)
                while (System.currentTimeMillis() < deadline) {
                    try {
                        val packet = DatagramPacket(buf, buf.size)
                        socket.receive(packet)
                        val text = String(packet.data, 0, packet.length)
                        runCatching { JellyfinJson.decodeFromString(DiscoveredServer.serializer(), text) }
                            .getOrNull()
                            ?.takeIf { it.address.isNotBlank() }
                            ?.let { found[it.id.ifBlank { it.address }] = it }
                    } catch (_: SocketTimeoutException) {
                        // keep listening until the deadline
                    }
                }
            }
        } catch (_: Exception) {
            // Network without broadcast support; discovery just finds nothing.
        } finally {
            runCatching { lock?.release() }
        }
        found.values.toList()
    }
}

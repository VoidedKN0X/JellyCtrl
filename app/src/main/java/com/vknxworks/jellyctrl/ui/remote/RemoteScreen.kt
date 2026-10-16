package com.vknxworks.jellyctrl.ui.remote

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vknxworks.jellyctrl.container
import com.vknxworks.jellyctrl.data.api.JellyfinClient
import com.vknxworks.jellyctrl.data.api.SessionInfo
import com.vknxworks.jellyctrl.data.api.sessionMessage
import com.vknxworks.jellyctrl.data.api.sessionPlaystate
import com.vknxworks.jellyctrl.data.api.sessions
import com.vknxworks.jellyctrl.data.api.ticksToMs
import com.vknxworks.jellyctrl.ui.components.ArtImage
import com.vknxworks.jellyctrl.ui.components.LoadContent
import com.vknxworks.jellyctrl.ui.components.MessageView
import com.vknxworks.jellyctrl.ui.components.ScreenTopBar
import com.vknxworks.jellyctrl.ui.components.formatClock
import com.vknxworks.jellyctrl.ui.components.friendlyMessage
import com.vknxworks.jellyctrl.ui.components.landscapeImage
import com.vknxworks.jellyctrl.ui.components.rememberLoader
import com.vknxworks.jellyctrl.ui.nav.LocalNavigator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Control playback on other Jellyfin clients (TV apps, browsers) signed in to this server. */
@Composable
fun RemoteScreen(adminView: Boolean = false) {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val client = c.api
    val loader = rememberLoader("sessions-$adminView") {
        client.sessions(controllableOnly = !adminView).filter { it.deviceId != client.deviceId }
    }
    LaunchedEffect(loader) {
        while (true) {
            delay(5000)
            loader.reload(silent = true)
        }
    }
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ScreenTopBar(if (adminView) "Active devices" else "Control another device") },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            LoadContent(loader) { sessions ->
                if (sessions.isEmpty()) {
                    MessageView(Icons.Filled.Devices, "No other devices", "Open Jellyfin on your TV or in a browser with this account and it shows up here.")
                } else {
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(sessions, key = { it.id }) { s -> SessionCard(s, client, adminView) { loader.reload(silent = true) } }
                    }
                }
            }
        }
    }
}

@Composable
fun SessionCard(s: SessionInfo, client: JellyfinClient, showUser: Boolean, onChanged: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var messageOpen by remember { mutableStateOf(false) }
    val np = s.nowPlayingItem

    fun cmd(block: suspend () -> Unit) {
        scope.launch {
            runCatching { block() }
                .onSuccess { onChanged() }
                .onFailure { Toast.makeText(context, it.friendlyMessage(), Toast.LENGTH_SHORT).show() }
        }
    }

    Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Devices, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.deviceName ?: "Device", fontWeight = FontWeight.SemiBold)
                    Text(
                        listOfNotNull(s.client, s.applicationVersion, if (showUser) s.userName else null, s.remoteEndPoint).joinToString("  ·  "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (s.supportsRemoteControl) {
                    IconButton(onClick = { messageOpen = true }) { Icon(Icons.AutoMirrored.Filled.Message, "Send message") }
                }
            }
            if (np != null) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(width = 96.dp, height = 54.dp).clip(RoundedCornerShape(6.dp))) {
                        ArtImage(client.landscapeImage(np, 300), Modifier.fillMaxSize())
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (np.type == "Episode") "${np.seriesName} · ${np.name}" else np.name.orEmpty(),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        val pos = s.playState?.positionTicks?.ticksToMs() ?: 0
                        val dur = np.runTimeTicks?.ticksToMs() ?: 0
                        Text(
                            "${formatClock(pos)} / ${formatClock(dur)}" + (s.playState?.playMethod?.let { "  ·  $it" } ?: "") +
                                (s.transcodingInfo?.let { t -> "  ·  ${listOfNotNull(t.videoCodec, t.audioCodec).joinToString("/")}" } ?: ""),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (dur > 0) {
                            LinearProgressIndicator(progress = { (pos.toFloat() / dur).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp), drawStopIndicator = {})
                        }
                    }
                }
                if (s.supportsMediaControl) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        IconButton(onClick = { cmd { client.sessionPlaystate(s.id, "PreviousTrack") } }) { Icon(Icons.Filled.SkipPrevious, "Previous") }
                        val paused = s.playState?.isPaused == true
                        IconButton(onClick = { cmd { client.sessionPlaystate(s.id, if (paused) "Unpause" else "Pause") } }) {
                            Icon(if (paused) Icons.Filled.PlayArrow else Icons.Filled.Pause, if (paused) "Play" else "Pause")
                        }
                        IconButton(onClick = { cmd { client.sessionPlaystate(s.id, "Stop") } }) { Icon(Icons.Filled.Stop, "Stop") }
                        IconButton(onClick = { cmd { client.sessionPlaystate(s.id, "NextTrack") } }) { Icon(Icons.Filled.SkipNext, "Next") }
                    }
                }
            }
        }
    }

    if (messageOpen) {
        var text by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { messageOpen = false },
            title = { Text("Message ${s.deviceName ?: "device"}") },
            text = { OutlinedTextField(text, { text = it }, label = { Text("Message") }) },
            confirmButton = {
                TextButton(onClick = {
                    messageOpen = false
                    cmd { client.sessionMessage(s.id, "JellyCtrl", text) }
                }, enabled = text.isNotBlank()) { Text("Send") }
            },
            dismissButton = { TextButton(onClick = { messageOpen = false }) { Text("Cancel") } },
        )
    }
}

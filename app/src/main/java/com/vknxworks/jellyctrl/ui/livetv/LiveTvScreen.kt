package com.vknxworks.jellyctrl.ui.livetv

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vknxworks.jellyctrl.container
import com.vknxworks.jellyctrl.data.api.BaseItemDto
import com.vknxworks.jellyctrl.data.api.cancelTimer
import com.vknxworks.jellyctrl.data.api.liveTvChannels
import com.vknxworks.jellyctrl.data.api.liveTvPrograms
import com.vknxworks.jellyctrl.data.api.liveTvRecordings
import com.vknxworks.jellyctrl.data.api.liveTvTimers
import com.vknxworks.jellyctrl.data.api.recordProgram
import com.vknxworks.jellyctrl.player.PlayerActivity
import com.vknxworks.jellyctrl.ui.components.ArtImage
import com.vknxworks.jellyctrl.ui.components.ItemGrid
import com.vknxworks.jellyctrl.ui.components.LoadContent
import com.vknxworks.jellyctrl.ui.components.MessageView
import com.vknxworks.jellyctrl.ui.components.formatDateTime
import com.vknxworks.jellyctrl.ui.components.formatTime
import com.vknxworks.jellyctrl.ui.components.friendlyMessage
import com.vknxworks.jellyctrl.ui.components.isoToEpochMs
import com.vknxworks.jellyctrl.ui.components.primaryImage
import com.vknxworks.jellyctrl.ui.components.rememberLoader
import com.vknxworks.jellyctrl.ui.nav.LocalNavigator
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit

@Composable
fun LiveTvScreen() {
    val nav = LocalNavigator.current
    var tab by rememberSaveable { mutableStateOf(0) }
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Live TV") },
                    navigationIcon = { IconButton(onClick = nav::back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                )
                PrimaryTabRow(selectedTabIndex = tab) {
                    listOf("Channels", "Guide", "Recordings", "Scheduled").forEachIndexed { i, t ->
                        Tab(tab == i, onClick = { tab = i }, text = { Text(t) })
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                0 -> Channels()
                1 -> Guide()
                2 -> Recordings()
                else -> Timers()
            }
        }
    }
}

@Composable
private fun Channels() {
    val context = LocalContext.current
    val c = context.container
    val client = c.api
    val loader = rememberLoader("tv-channels") { client.liveTvChannels() }
    LoadContent(loader) { channels ->
        if (channels.isEmpty()) {
            MessageView(Icons.Outlined.LiveTv, "No channels", "Set up a tuner in the server's Live TV settings.")
            return@LoadContent
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(channels, key = { it.id }) { ch ->
                val prog = ch.currentProgram
                ListItem(
                    headlineContent = { Text(listOfNotNull(ch.channelNumber, ch.name).joinToString("  "), fontWeight = FontWeight.Medium) },
                    supportingContent = {
                        Column {
                            Text(prog?.name ?: "No guide data", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            programProgress(prog)?.let { p ->
                                LinearProgressIndicator(progress = { p }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp), drawStopIndicator = {})
                            }
                        }
                    },
                    leadingContent = {
                        Box(Modifier.size(width = 72.dp, height = 44.dp).clip(RoundedCornerShape(6.dp))) {
                            ArtImage(client.primaryImage(ch, 200), Modifier.fillMaxSize(), contentScale = ContentScale.Fit, placeholderText = ch.channelNumber ?: ch.name)
                        }
                    },
                    modifier = Modifier.clickable { PlayerActivity.start(context, ch.id, startMs = 0) },
                )
            }
        }
    }
}

private fun programProgress(p: BaseItemDto?): Float? {
    val start = isoToEpochMs(p?.startDate) ?: return null
    val end = isoToEpochMs(p?.endDate) ?: return null
    if (end <= start) return null
    return ((System.currentTimeMillis() - start).toFloat() / (end - start)).coerceIn(0f, 1f)
}

@Composable
private fun Guide() {
    val context = LocalContext.current
    val c = context.container
    val client = c.api
    val scope = rememberCoroutineScope()
    var recordTarget by remember { mutableStateOf<BaseItemDto?>(null) }
    val loader = rememberLoader("tv-guide") {
        val channels = client.liveTvChannels()
        val now = Instant.now()
        val programs = client.liveTvPrograms(
            channels.map { it.id },
            now.toString(),
            now.plus(12, ChronoUnit.HOURS).toString(),
        )
        val byChannel = programs.groupBy { it.channelId }
        channels.map { it to (byChannel[it.id] ?: emptyList()) }
    }
    LoadContent(loader) { rows ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
            items(rows, key = { it.first.id }) { (ch, progs) ->
                Column(Modifier.padding(vertical = 6.dp)) {
                    Text(
                        listOfNotNull(ch.channelNumber, ch.name).joinToString("  "),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).clickable { PlayerActivity.start(context, ch.id, startMs = 0) },
                    )
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(progs, key = { it.id }) { p ->
                            val live = programProgress(p)?.let { it > 0f && it < 1f } == true
                            Card(
                                modifier = Modifier.width(200.dp).combinedClickable(
                                    onClick = { if (live) PlayerActivity.start(context, ch.id, startMs = 0) else recordTarget = p },
                                    onLongClick = { recordTarget = p },
                                ),
                                colors = if (live) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else CardDefaults.cardColors(),
                            ) {
                                Column(Modifier.padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("${formatTime(p.startDate) ?: ""} – ${formatTime(p.endDate) ?: ""}", style = MaterialTheme.typography.labelSmall)
                                        if (p.timerId != null) {
                                            Icon(Icons.Filled.FiberManualRecord, "Recording", Modifier.padding(start = 4.dp).size(10.dp), tint = Color.Red)
                                        }
                                    }
                                    Text(p.name.orEmpty(), style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    p.episodeTitle?.let { Text(it, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    recordTarget?.let { p ->
        AlertDialog(
            onDismissRequest = { recordTarget = null },
            title = { Text(p.name.orEmpty()) },
            text = {
                Column {
                    Text("${formatDateTime(p.startDate) ?: ""} – ${formatTime(p.endDate) ?: ""}")
                    p.overview?.let { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp)) }
                }
            },
            confirmButton = {
                if (p.timerId == null) {
                    TextButton(onClick = {
                        recordTarget = null
                        scope.launch {
                            runCatching { client.recordProgram(p.id) }
                                .onSuccess { Toast.makeText(context, "Recording scheduled", Toast.LENGTH_SHORT).show(); loader.reload(silent = true) }
                                .onFailure { Toast.makeText(context, it.friendlyMessage(), Toast.LENGTH_SHORT).show() }
                        }
                    }) { Text("Record") }
                } else {
                    TextButton(onClick = {
                        recordTarget = null
                        scope.launch {
                            runCatching { client.cancelTimer(p.timerId) }
                                .onSuccess { loader.reload(silent = true) }
                                .onFailure { Toast.makeText(context, it.friendlyMessage(), Toast.LENGTH_SHORT).show() }
                        }
                    }) { Text("Cancel recording") }
                }
            },
            dismissButton = { TextButton(onClick = { recordTarget = null }) { Text("Close") } },
        )
    }
}

@Composable
private fun Recordings() {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val loader = rememberLoader("tv-recordings") { c.api.liveTvRecordings() }
    LoadContent(loader) { recs ->
        ItemGrid(recs, c.api, onItem = { nav.openItem(it) }, emptyText = "No recordings yet")
    }
}

@Composable
private fun Timers() {
    val context = LocalContext.current
    val c = context.container
    val scope = rememberCoroutineScope()
    val loader = rememberLoader("tv-timers") { c.api.liveTvTimers() }
    LoadContent(loader) { timers ->
        if (timers.isEmpty()) {
            MessageView(Icons.Outlined.LiveTv, "Nothing scheduled", "Tap a program in the guide to record it.")
            return@LoadContent
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(timers, key = { it.id ?: it.programId ?: it.name.orEmpty() }) { t ->
                ListItem(
                    headlineContent = { Text(t.name.orEmpty()) },
                    supportingContent = { Text("${t.channelName.orEmpty()}  ·  ${formatDateTime(t.startDate) ?: ""}") },
                    trailingContent = {
                        IconButton(onClick = {
                            val id = t.id ?: return@IconButton
                            scope.launch {
                                runCatching { c.api.cancelTimer(id) }
                                    .onSuccess { loader.reload(silent = true) }
                                    .onFailure { Toast.makeText(context, it.friendlyMessage(), Toast.LENGTH_SHORT).show() }
                            }
                        }) { Icon(Icons.Filled.Delete, "Cancel") }
                    },
                )
            }
        }
    }
}

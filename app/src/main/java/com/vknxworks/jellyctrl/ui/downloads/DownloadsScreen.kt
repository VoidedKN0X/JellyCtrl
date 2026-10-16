package com.vknxworks.jellyctrl.ui.downloads

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.DownloadForOffline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.jellyctrl.container
import com.vknxworks.jellyctrl.data.download.DownloadEntry
import com.vknxworks.jellyctrl.data.download.DownloadStatus
import com.vknxworks.jellyctrl.player.PlayerActivity
import com.vknxworks.jellyctrl.ui.components.ArtImage
import com.vknxworks.jellyctrl.ui.components.MessageView
import com.vknxworks.jellyctrl.ui.components.ScreenTopBar
import com.vknxworks.jellyctrl.ui.components.formatBytes

@Composable
fun DownloadsScreen() {
    val context = LocalContext.current
    val c = context.container
    val entries by c.downloads.entries.collectAsStateWithLifecycle()
    val settings by c.settings.settings.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf<DownloadEntry?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            ScreenTopBar("Downloads") {
                if (entries.isNotEmpty()) {
                    Text(
                        formatBytes(c.downloads.usedBytes()),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 16.dp),
                    )
                }
            }
        },
    ) { padding ->
        if (entries.isEmpty()) {
            MessageView(
                Icons.Outlined.DownloadForOffline,
                "Nothing downloaded",
                "Download a film, an episode, a whole season or an album to watch it without a connection.",
                Modifier.padding(padding),
            )
            return@Scaffold
        }
        val sorted = entries.sortedWith(compareBy<DownloadEntry> { it.status == DownloadStatus.DONE }.thenBy { it.seriesName ?: it.name }.thenBy { it.item.parentIndexNumber ?: 0 }.thenBy { it.item.indexNumber ?: 0 })
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(sorted, key = { it.itemId }) { e ->
                val item = e.item
                val poster = c.downloads.poster(e)?.takeIf { it.exists() }
                ListItem(
                    headlineContent = { Text(if (e.seriesName != null) "${e.seriesName} · ${e.name}" else e.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                    supportingContent = {
                        Column {
                            val status = when (e.status) {
                                DownloadStatus.DONE -> formatBytes(e.totalBytes)
                                DownloadStatus.RUNNING -> "${(e.progress * 100).toInt()}%  ·  ${formatBytes(e.downloadedBytes)} of ${formatBytes(e.totalBytes)}"
                                DownloadStatus.QUEUED -> if (settings.downloadWifiOnly) "Waiting for Wi-Fi or queue" else "Queued"
                                DownloadStatus.PAUSED -> "Paused"
                                DownloadStatus.FAILED -> "Failed: ${e.error ?: "unknown error"}"
                            }
                            Text(status, style = MaterialTheme.typography.bodySmall)
                            if (e.status == DownloadStatus.RUNNING) {
                                LinearProgressIndicator(progress = { e.progress }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp), drawStopIndicator = {})
                            }
                        }
                    },
                    leadingContent = {
                        Box(Modifier.width(if (item.type == "Episode") 96.dp else 56.dp).aspectRatio(if (item.type == "Episode") 16f / 9f else if (item.mediaType == "Audio") 1f else 2f / 3f).clip(RoundedCornerShape(6.dp))) {
                            ArtImage(poster?.let { android.net.Uri.fromFile(it).toString() }, Modifier.fillMaxSize(), placeholderText = e.name)
                        }
                    },
                    trailingContent = {
                        Row {
                            if (e.status == DownloadStatus.FAILED || e.status == DownloadStatus.PAUSED) {
                                IconButton(onClick = { c.downloads.retry(e.itemId, settings.downloadWifiOnly) }) { Icon(Icons.Filled.Refresh, "Retry") }
                            }
                            IconButton(onClick = { confirmDelete = e }) { Icon(Icons.Filled.Delete, "Delete") }
                        }
                    },
                    modifier = Modifier.clickable(enabled = e.status == DownloadStatus.DONE) {
                        if (item.mediaType == "Audio") c.music.playItems(listOf(item), 0)
                        else PlayerActivity.start(context, e.itemId)
                    },
                )
            }
        }
    }

    confirmDelete?.let { e ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text(if (e.status == DownloadStatus.DONE) "Delete download?" else "Cancel download?") },
            text = { Text(e.name) },
            confirmButton = { TextButton(onClick = { c.downloads.cancel(e.itemId); confirmDelete = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Keep") } },
        )
    }
}

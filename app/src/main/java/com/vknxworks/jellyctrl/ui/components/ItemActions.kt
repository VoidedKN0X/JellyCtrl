package com.vknxworks.jellyctrl.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import com.vknxworks.jellyctrl.MainActivity
import com.vknxworks.jellyctrl.container
import com.vknxworks.jellyctrl.data.api.BaseItemDto
import com.vknxworks.jellyctrl.data.api.ItemQuery
import com.vknxworks.jellyctrl.data.api.addToPlaylist
import com.vknxworks.jellyctrl.data.api.createPlaylist
import com.vknxworks.jellyctrl.data.api.items
import com.vknxworks.jellyctrl.data.api.setFavorite
import com.vknxworks.jellyctrl.data.api.setPlayed
import com.vknxworks.jellyctrl.ui.nav.ItemRoute
import com.vknxworks.jellyctrl.ui.nav.LocalNavigator
import kotlinx.coroutines.launch

/** Long-press menu for any item. [onChanged] is called after played/favorite state changes. */
@Composable
fun ItemActionsSheet(item: BaseItemDto, onDismiss: () -> Unit, onChanged: () -> Unit = {}) {
    val context = LocalContext.current
    val c = context.container
    val nav = LocalNavigator.current
    val scope = rememberCoroutineScope()
    var playlistPicker by remember { mutableStateOf(false) }

    fun act(block: suspend () -> Unit) {
        scope.launch {
            runCatching { block() }
                .onSuccess { onChanged() }
                .onFailure { Toast.makeText(context, it.friendlyMessage(), Toast.LENGTH_SHORT).show() }
            onDismiss()
        }
    }

    if (playlistPicker) {
        AddToPlaylistDialog(item, onDismiss = { playlistPicker = false; onDismiss() })
        return
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 12.dp)) {
            Text(
                item.name.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val played = item.userData?.played == true
            val fav = item.userData?.isFavorite == true
            if (item.isPlayable || item.isFolder) {
                Action(Icons.Filled.PlayArrow, "Play") { nav.play(item); onDismiss() }
            }
            if ((item.userData?.playbackPositionTicks ?: 0) > 0) {
                Action(Icons.Filled.Replay, "Play from beginning") { nav.play(item, fromStart = true); onDismiss() }
            }
            if (item.isFolder) {
                Action(Icons.Filled.Shuffle, "Shuffle") { nav.shuffle(item); onDismiss() }
            }
            if (item.mediaType == "Audio" || item.type == "MusicAlbum") {
                Action(Icons.AutoMirrored.Filled.QueueMusic, "Add to queue") {
                    c.music.enqueue(item)
                    onDismiss()
                }
            }
            Action(
                if (played) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                if (played) "Mark unplayed" else "Mark played",
            ) { act { c.api.setPlayed(item.id, !played) } }
            Action(if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, if (fav) "Remove from favorites" else "Add to favorites") {
                act { c.api.setFavorite(item.id, !fav) }
            }
            if (item.type != "Playlist" && (item.isPlayable || item.isFolder)) {
                Action(Icons.AutoMirrored.Filled.PlaylistAdd, "Add to playlist") { playlistPicker = true }
            }
            if (item.type == "Episode" && item.seriesId != null) {
                Action(Icons.Filled.Tv, "Go to series") { nav.navigate(ItemRoute(item.seriesId)); onDismiss() }
            }
            if (item.isPlayable && item.mediaType == "Video" || item.type in DOWNLOADABLE_FOLDERS) {
                Action(Icons.Filled.Download, "Download") {
                    (context as? MainActivity)?.ensureNotificationPermission()
                    scope.launch {
                        val n = downloadItemWith(c, item)
                        Toast.makeText(context, if (n == 1) "Download queued" else "$n downloads queued", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                }
            }
            Action(Icons.Outlined.Info, "Details") { nav.navigate(ItemRoute(item.id)); onDismiss() }
        }
    }
}

private val DOWNLOADABLE_FOLDERS = setOf("Season", "Series", "MusicAlbum")

@Composable
private fun Action(icon: ImageVector, label: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = { Icon(icon, null) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

/** Queue an item (or every playable child of a season/series/album) for offline use. Returns how many were queued. */
suspend fun downloadItemWith(c: com.vknxworks.jellyctrl.AppContainer, item: BaseItemDto): Int {
    val account = c.sessions.active.value?.account ?: return 0
    val wifiOnly = c.settings.settings.value.downloadWifiOnly
    val targets = if (item.isFolder) {
        c.api.items(
            ItemQuery(
                parentId = item.id,
                recursive = true,
                includeItemTypes = listOf("Episode", "Movie", "Audio", "Video", "MusicVideo"),
                sortBy = listOf("ParentIndexNumber", "IndexNumber", "SortName"),
                fields = listOf("MediaSources", "Path", "PrimaryImageAspectRatio", "Overview"),
            ),
            limit = 1000,
        ).items
    } else listOf(item)
    targets.forEach { c.downloads.enqueue(it, account.key, wifiOnly) }
    return targets.size
}

@Composable
fun AddToPlaylistDialog(item: BaseItemDto, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val c = context.container
    val scope = rememberCoroutineScope()
    val mediaType = if (item.mediaType == "Audio" || item.type == "MusicAlbum") "Audio" else "Video"
    val playlists = rememberLoader("playlists-$mediaType") {
        c.api.items(ItemQuery(includeItemTypes = listOf("Playlist"), recursive = true)).items
    }
    var newName by remember { mutableStateOf("") }

    fun done(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to playlist") },
        text = {
            Column {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("New playlist name") },
                    singleLine = true,
                )
                LoadContent(playlists, Modifier.padding(top = 8.dp)) { list ->
                    LazyColumn(Modifier.padding(top = 8.dp)) {
                        items(list, key = { it.id }) { pl ->
                            ListItem(
                                headlineContent = { Text(pl.name.orEmpty()) },
                                modifier = Modifier.clickable {
                                    scope.launch {
                                        runCatching { c.api.addToPlaylist(pl.id, listOf(item.id)) }
                                            .onSuccess { done("Added to ${pl.name}") }
                                            .onFailure { done(it.friendlyMessage()) }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = newName.isNotBlank(),
                onClick = {
                    scope.launch {
                        runCatching { c.api.createPlaylist(newName.trim(), listOf(item.id), mediaType) }
                            .onSuccess { done("Created $newName") }
                            .onFailure { done(it.friendlyMessage()) }
                    }
                },
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

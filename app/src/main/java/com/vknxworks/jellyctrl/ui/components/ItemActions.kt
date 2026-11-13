package com.vknxworks.jellyctrl.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.Tv
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
import com.vknxworks.jellyctrl.data.api.episodes
import com.vknxworks.jellyctrl.data.download.DownloadPlan
import com.vknxworks.jellyctrl.data.download.DownloadStatus
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.pluralStringResource
import com.vknxworks.jellyctrl.data.api.setFavorite
import com.vknxworks.jellyctrl.data.api.setPlayed
import com.vknxworks.jellyctrl.ui.nav.ItemRoute
import com.vknxworks.jellyctrl.ui.nav.LocalNavigator
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.vknxworks.jellyctrl.R
import com.vknxworks.jellyctrl.str
import com.vknxworks.jellyctrl.plural


/** Long-press menu for any item. [onChanged] is called after played/favorite state changes. */
@Composable
fun ItemActionsSheet(item: BaseItemDto, onDismiss: () -> Unit, onChanged: () -> Unit = {}) {
    val context = LocalContext.current
    val c = context.container
    val nav = LocalNavigator.current
    val scope = rememberCoroutineScope()
    var playlistPicker by remember { mutableStateOf(false) }

    // The menu closes at once; the change runs in the app's scope so closing the menu can't cancel it.
    // Screens showing the item update when setPlayed/setFavorite announce the change.
    fun act(block: suspend () -> Unit) {
        val appContext = context.applicationContext
        onDismiss()
        c.appScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            runCatching { block() }
                .onSuccess { onChanged() }
                .onFailure { Toast.makeText(appContext, it.friendlyMessage(), Toast.LENGTH_SHORT).show() }
        }
    }

    var downloadOptions by remember { mutableStateOf(false) }

    if (playlistPicker) {
        AddToPlaylistDialog(item, onDismiss = { playlistPicker = false; onDismiss() })
        return
    }
    if (downloadOptions) {
        DownloadOptionsSheet(item, onDismiss = { downloadOptions = false; onDismiss() })
        return
    }

    CtrlSheet(onDismissRequest = onDismiss) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 12.dp)) {
            Text(
                item.name.orEmpty(),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val played = item.userData?.played == true
            val fav = item.userData?.isFavorite == true
            if (item.isPlayable || item.isFolder) {
                Action(Icons.Filled.PlayArrow, stringResource(R.string.play)) { nav.play(item); onDismiss() }
            }
            if ((item.userData?.playbackPositionTicks ?: 0) > 0) {
                Action(Icons.Filled.Replay, stringResource(R.string.play_from_the_beginning)) { nav.play(item, fromStart = true); onDismiss() }
            }
            if (item.isFolder) {
                Action(Icons.Filled.Shuffle, stringResource(R.string.shuffle)) { nav.shuffle(item); onDismiss() }
            }
            if (item.mediaType == "Audio" || item.type == "MusicAlbum") {
                Action(Icons.AutoMirrored.Outlined.QueueMusic, stringResource(R.string.add_to_queue)) {
                    c.music.enqueue(item)
                    onDismiss()
                }
            }
            Action(
                if (played) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                if (played) stringResource(R.string.mark_as_unwatched) else stringResource(R.string.mark_as_watched),
            ) { act { c.api.setPlayed(item.id, !played) } }
            Action(if (fav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, if (fav) stringResource(R.string.remove_from_favourites) else stringResource(R.string.add_to_favourites)) {
                act { c.api.setFavorite(item.id, !fav) }
            }
            if (item.type != "Playlist" && (item.isPlayable || item.isFolder)) {
                Action(Icons.AutoMirrored.Outlined.PlaylistAdd, stringResource(R.string.add_to_playlist)) { playlistPicker = true }
            }
            if (item.type == "Episode" && item.seriesId != null) {
                Action(Icons.Outlined.Tv, stringResource(R.string.go_to_show)) { nav.navigate(ItemRoute(item.seriesId)); onDismiss() }
            }
            if (item.isPlayable && item.mediaType == "Video" || item.type in DOWNLOADABLE_FOLDERS) {
                Action(Icons.Outlined.Download, stringResource(R.string.download)) {
                    if (askHowMuchToDownload(item)) {
                        downloadOptions = true
                        return@Action
                    }
                    (context as? MainActivity)?.ensureNotificationPermission()
                    scope.launch {
                        val n = downloadItemWith(c, item)
                        Toast.makeText(context, plural(R.plurals.downloading_items, n, n), Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                }
            }
            Action(Icons.Outlined.Info, stringResource(R.string.details)) { nav.navigate(ItemRoute(item.id)); onDismiss() }
        }
    }
}

private val DOWNLOADABLE_FOLDERS = setOf("Season", "Series", "MusicAlbum")

@Composable
private fun Action(icon: ImageVector, label: String, onClick: () -> Unit) {
    GroupRow(label, icon = icon, iconTint = MaterialTheme.colorScheme.onSurfaceVariant, onClick = onClick)
}

/**
 * Queue an item (or every playable child of a season/series/album) for offline use. With
 * [nextUnwatched] > 0, a series or season only gets its next that many unwatched episodes.
 * Quality comes from Settings > Downloads. Returns how many were queued.
 */
suspend fun downloadItemWith(c: com.vknxworks.jellyctrl.AppContainer, item: BaseItemDto, nextUnwatched: Int = 0): Int {
    val account = c.sessions.active.value?.account ?: return 0
    val s = c.settings.settings.value
    val targets = when {
        !item.isFolder -> listOf(item)
        nextUnwatched > 0 && item.type in setOf("Series", "Season") -> {
            val seriesId = if (item.type == "Series") item.id else item.seriesId ?: item.parentId ?: return 0
            val episodes = c.api.episodes(seriesId, seasonId = if (item.type == "Season") item.id else null)
            DownloadPlan.nextUnwatched(episodes, nextUnwatched)
        }
        else -> c.api.items(
            ItemQuery(
                parentId = item.id,
                recursive = true,
                includeItemTypes = listOf("Episode", "Movie", "Audio", "Video", "MusicVideo"),
                sortBy = listOf("ParentIndexNumber", "IndexNumber", "SortName"),
                fields = listOf("MediaSources", "Path", "PrimaryImageAspectRatio", "Overview"),
            ),
            limit = 1000,
        ).items
    }.filter { t -> c.downloads.find(t.id)?.status != DownloadStatus.DONE }
    targets.forEach { c.downloads.enqueue(it, account.key, s.downloadWifiOnly, DownloadPlan.bitrateFor(it, s.downloadVideoMbps, s.downloadMusicKbps)) }
    return targets.size
}

/** Series and seasons ask what to download: everything, or the next few unwatched episodes. */
fun askHowMuchToDownload(item: BaseItemDto): Boolean = item.type == "Series" || item.type == "Season"

/** Download choices for a series or season, with the quality it will use. */
@Composable
fun DownloadOptionsSheet(item: BaseItemDto, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val c = context.container
    val s by c.settings.settings.collectAsStateWithLifecycle()

    fun start(next: Int) {
        (context as? MainActivity)?.ensureNotificationPermission()
        val appContext = context.applicationContext
        onDismiss()
        c.appScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            runCatching { downloadItemWith(c, item, next) }
                .onSuccess { n ->
                    Toast.makeText(appContext, if (n == 0) str(R.string.nothing_new_to_download) else plural(R.plurals.downloading_items, n, n), Toast.LENGTH_SHORT).show()
                }
                .onFailure { Toast.makeText(appContext, it.friendlyMessage(), Toast.LENGTH_SHORT).show() }
        }
    }

    CtrlSheet(onDismissRequest = onDismiss) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 12.dp)) {
            Text(
                stringResource(R.string.download_x, item.name.orEmpty()),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (s.downloadVideoMbps > 0) stringResource(R.string.download_quality_converted_x, s.downloadVideoMbps) else stringResource(R.string.download_quality_original_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
            )
            Action(Icons.Outlined.Download, stringResource(if (item.type == "Series") R.string.download_every_episode else R.string.download_this_season)) { start(0) }
            listOf(1, 3, 5, 10).forEach { n ->
                Action(Icons.Outlined.Download, pluralStringResource(R.plurals.download_next_unwatched, n, n)) { start(n) }
            }
        }
    }
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

    CtrlAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_to_playlist)) },
        text = {
            Column {
                CtrlTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(stringResource(R.string.new_playlist_name)) },
                    singleLine = true,
                )
                LoadContent(playlists, Modifier.padding(top = 8.dp)) { list ->
                    LazyColumn(Modifier.padding(top = 8.dp)) {
                        items(list, key = { it.id }) { pl ->
                            CtrlListItem(
                                headlineContent = { Text(pl.name.orEmpty()) },
                                modifier = Modifier.clickable {
                                    scope.launch {
                                        runCatching { c.api.addToPlaylist(pl.id, listOf(item.id)) }
                                            .onSuccess { done(str(R.string.added_to_x, pl.name)) }
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
                            .onSuccess { done(str(R.string.created_x, newName)) }
                            .onFailure { done(it.friendlyMessage()) }
                    }
                },
            ) { Text(stringResource(R.string.create)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

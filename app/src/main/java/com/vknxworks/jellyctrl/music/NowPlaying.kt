package com.vknxworks.jellyctrl.music

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.vknxworks.jellyctrl.MainActivity
import com.vknxworks.jellyctrl.container
import com.vknxworks.jellyctrl.data.api.lyrics
import com.vknxworks.jellyctrl.data.api.ticksToMs
import com.vknxworks.jellyctrl.ui.components.ArtImage
import com.vknxworks.jellyctrl.ui.components.MessageView
import com.vknxworks.jellyctrl.ui.components.formatClock
import com.vknxworks.jellyctrl.ui.components.rememberLoader
import com.vknxworks.jellyctrl.ui.nav.ItemRoute
import com.vknxworks.jellyctrl.ui.nav.LocalNavigator

@Composable
fun MiniPlayer(onOpen: () -> Unit) {
    val context = LocalContext.current
    val music = context.container.music
    val s by music.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { music.connectIfRunning() }
    LaunchedEffect(s.hasMedia) { if (s.hasMedia) (context as? MainActivity)?.ensureNotificationPermission() }
    if (!s.hasMedia) return
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, tonalElevation = 3.dp) {
        Column {
            LinearProgressIndicator(
                progress = { if (s.durationMs > 0) (s.positionMs.toFloat() / s.durationMs).coerceIn(0f, 1f) else 0f },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                drawStopIndicator = {},
            )
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(6.dp))) { ArtImage(s.artworkUri, Modifier.fillMaxSize()) }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(s.title.orEmpty(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(s.artist.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = { music.togglePlay() }) {
                    Icon(if (s.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (s.isPlaying) "Pause" else "Play")
                }
                IconButton(onClick = { music.next() }) { Icon(Icons.Filled.SkipNext, "Next") }
                IconButton(onClick = { music.stop() }) { Icon(Icons.Filled.Close, "Stop") }
            }
        }
    }
}

@Composable
fun NowPlayingScreen() {
    val context = LocalContext.current
    val c = context.container
    val music = c.music
    val nav = LocalNavigator.current
    val s by music.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(0) }
    var scrub by remember { mutableStateOf<Float?>(null) }

    if (!s.hasMedia) {
        MessageView(Icons.Outlined.MusicNote, "Nothing playing", "Play an album, playlist or song to see it here.")
        return
    }

    BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding()) {
        val wide = maxWidth > maxHeight && maxWidth >= 600.dp
        val player: @Composable (Modifier) -> Unit = { mod ->
            Column(mod.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth()) {
                    IconButton(onClick = nav::back) { Icon(Icons.Filled.KeyboardArrowDown, "Close") }
                }
                Box(
                    Modifier.widthIn(max = 420.dp).fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp))
                        .clickable { s.itemId?.let { nav.navigate(ItemRoute(it)) } },
                ) {
                    ArtImage(s.artworkUri, Modifier.fillMaxSize())
                }
                Spacer(Modifier.height(24.dp))
                Text(s.title.orEmpty(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(s.artist, s.album).joinToString(" — "),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
                Spacer(Modifier.height(16.dp))
                Slider(
                    value = scrub ?: s.positionMs.toFloat(),
                    onValueChange = { scrub = it },
                    onValueChangeFinished = { scrub?.let { music.seekTo(it.toLong()) }; scrub = null },
                    valueRange = 0f..s.durationMs.toFloat().coerceAtLeast(1f),
                )
                Row(Modifier.fillMaxWidth()) {
                    Text(formatClock((scrub ?: s.positionMs.toFloat()).toLong()), style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.weight(1f))
                    Text(formatClock(s.durationMs), style = MaterialTheme.typography.labelMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconButton(onClick = { music.toggleShuffle() }) {
                        Icon(Icons.Filled.Shuffle, "Shuffle", tint = if (s.shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { music.previous() }) { Icon(Icons.Filled.SkipPrevious, "Previous", Modifier.size(36.dp)) }
                    FilledIconButton(onClick = { music.togglePlay() }, modifier = Modifier.size(72.dp), colors = IconButtonDefaults.filledIconButtonColors()) {
                        Icon(if (s.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Play/pause", Modifier.size(40.dp))
                    }
                    IconButton(onClick = { music.next() }) { Icon(Icons.Filled.SkipNext, "Next", Modifier.size(36.dp)) }
                    IconButton(onClick = { music.cycleRepeat() }) {
                        Icon(
                            if (s.repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                            "Repeat",
                            tint = if (s.repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        val side: @Composable (Modifier) -> Unit = { mod ->
            Column(mod) {
                PrimaryTabRow(selectedTabIndex = tab) {
                    Tab(tab == 0, onClick = { tab = 0 }, text = { Text("Queue") })
                    Tab(tab == 1, onClick = { tab = 1 }, text = { Text("Lyrics") })
                }
                if (tab == 0) QueueList(s) else LyricsView(s)
            }
        }
        if (wide) {
            Row(Modifier.fillMaxSize()) {
                player(Modifier.weight(1f))
                side(Modifier.weight(1f).fillMaxSize())
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                item { player(Modifier.fillMaxWidth()) }
                item { side(Modifier.fillMaxWidth().height(480.dp)) }
            }
        }
    }
}

@Composable
private fun QueueList(s: MusicState) {
    val music = LocalContext.current.container.music
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = s.index.coerceAtLeast(0))
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        itemsIndexed(s.queue, key = { i, e -> "$i-${e.itemId}" }) { i, e ->
            val current = i == s.index
            ListItem(
                headlineContent = { Text(e.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = if (current) FontWeight.Bold else null) },
                supportingContent = { e.artist?.let { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
                leadingContent = { Box(Modifier.size(40.dp).clip(RoundedCornerShape(4.dp))) { ArtImage(e.artworkUri, Modifier.fillMaxSize()) } },
                trailingContent = {
                    if (!current) IconButton(onClick = { music.removeAt(i) }) { Icon(Icons.Filled.Close, "Remove") }
                },
                colors = if (current) ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.secondaryContainer) else ListItemDefaults.colors(),
                modifier = Modifier.clickable { music.skipTo(i) },
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun LyricsView(s: MusicState) {
    val c = LocalContext.current.container
    val id = s.itemId ?: return
    val loader = rememberLoader("lyrics-$id") { c.api.lyrics(id) }
    val lyrics = loader.value
    if (lyrics == null || lyrics.lyrics.isEmpty()) {
        MessageView(Icons.Outlined.MusicNote, if (loader.value == null && loader.state is com.vknxworks.jellyctrl.ui.components.Load.Loading) "Loading lyrics…" else "No lyrics for this track")
        return
    }
    val synced = lyrics.lyrics.any { it.start != null }
    val currentIndex = if (synced) lyrics.lyrics.indexOfLast { (it.start ?: Long.MAX_VALUE).ticksToMs() <= s.positionMs } else -1
    val listState = rememberLazyListState()
    LaunchedEffect(currentIndex) { if (currentIndex > 2) listState.animateScrollToItem(currentIndex - 2) }
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 12.dp)) {
        itemsIndexed(lyrics.lyrics) { i, line ->
            val active = i == currentIndex
            Text(
                line.text.ifBlank { "♪" },
                style = if (active) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                color = if (active || !synced) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable(enabled = line.start != null) { line.start?.let { c.music.seekTo(it.ticksToMs()) } },
            )
        }
    }
}

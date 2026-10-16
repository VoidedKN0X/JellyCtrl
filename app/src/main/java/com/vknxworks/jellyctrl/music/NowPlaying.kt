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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.Subject
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
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
import com.vknxworks.jellyctrl.ui.components.Load
import com.vknxworks.jellyctrl.ui.components.MessageView
import com.vknxworks.jellyctrl.ui.components.SeekBar
import com.vknxworks.jellyctrl.ui.components.glass
import com.vknxworks.jellyctrl.ui.components.formatClock
import com.vknxworks.jellyctrl.ui.components.rememberLoader
import com.vknxworks.jellyctrl.ui.nav.ItemRoute
import com.vknxworks.jellyctrl.ui.nav.LocalNavigator
import com.vknxworks.jellyctrl.ui.theme.ArtworkTint
import com.vknxworks.jellyctrl.ui.theme.rememberArtworkColor

/** Floating card above the navigation bar while music plays. */
@Composable
fun MiniPlayer(onOpen: () -> Unit) {
    val context = LocalContext.current
    val music = context.container.music
    val s by music.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { music.connectIfRunning() }
    LaunchedEffect(s.hasMedia) { if (s.hasMedia) (context as? MainActivity)?.ensureNotificationPermission() }
    if (!s.hasMedia) return
    val seed = rememberArtworkColor(s.artworkUri)
    ArtworkTint(seed) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp)
                .glass(MaterialTheme.shapes.large, elevation = 12.dp, tint = MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            Box {
                Row(
                    Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(start = 8.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(44.dp).clip(MaterialTheme.shapes.small)) { ArtImage(s.artworkUri, Modifier.fillMaxSize()) }
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(s.title.orEmpty(), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(s.artist.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = { music.togglePlay() }) {
                        Icon(if (s.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (s.isPlaying) "Pause" else "Play")
                    }
                    IconButton(onClick = { music.next() }) { Icon(Icons.Filled.SkipNext, "Next track") }
                }
                val p = if (s.durationMs > 0) (s.positionMs.toFloat() / s.durationMs).coerceIn(0f, 1f) else 0f
                Box(
                    Modifier.align(Alignment.BottomStart).padding(horizontal = 16.dp).fillMaxWidth(p).height(2.dp)
                        .clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                )
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
    var panel by rememberSaveable { mutableStateOf<String?>(null) }
    var scrub by remember { mutableStateOf<Long?>(null) }
    val seed = rememberArtworkColor(s.artworkUri)

    if (!s.hasMedia) {
        MessageView(Icons.Outlined.MusicNote, "Nothing playing", "Play an album, a playlist or a song and it shows up here.")
        return
    }

    ArtworkTint(seed) {
        val top = MaterialTheme.colorScheme.primaryContainer
        val bg = MaterialTheme.colorScheme.background
        BoxWithConstraints(
            Modifier.fillMaxSize().background(Brush.verticalGradient(0f to top, 0.75f to bg)),
        ) {
            val wide = maxWidth > maxHeight && maxWidth >= 600.dp
            val player: @Composable (Modifier) -> Unit = { mod ->
                Column(mod.statusBarsPadding().padding(horizontal = 28.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = nav::back) { Icon(Icons.Filled.KeyboardArrowDown, "Close") }
                        Text(
                            s.album.orEmpty(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { music.stop(); nav.back() }) { Icon(Icons.Filled.Stop, "Stop playback") }
                    }
                    Spacer(Modifier.height(20.dp))
                    Box(
                        Modifier.widthIn(max = 440.dp).fillMaxWidth().aspectRatio(1f).clip(MaterialTheme.shapes.large)
                            .clickable { s.itemId?.let { nav.navigate(ItemRoute(it)) } },
                    ) { ArtImage(s.artworkUri, Modifier.fillMaxSize()) }
                    Spacer(Modifier.height(28.dp))
                    Column(Modifier.widthIn(max = 440.dp).fillMaxWidth()) {
                        Text(s.title.orEmpty(), style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(s.artist.orEmpty(), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(16.dp))
                        SeekBar(
                            positionMs = scrub ?: s.positionMs,
                            durationMs = s.durationMs,
                            onScrub = { scrub = it },
                            onSeek = { music.seekTo(it); scrub = null },
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                            bufferColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                        )
                        Row(Modifier.fillMaxWidth()) {
                            Text(formatClock(scrub ?: s.positionMs), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.weight(1f))
                            Text(formatClock(s.durationMs), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.widthIn(max = 440.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        IconButton(onClick = { music.toggleShuffle() }) {
                            Icon(Icons.Filled.Shuffle, "Shuffle", tint = if (s.shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { music.previous() }, modifier = Modifier.size(56.dp)) { Icon(Icons.Filled.SkipPrevious, "Previous", Modifier.size(36.dp)) }
                        IconButton(
                            onClick = { music.togglePlay() },
                            modifier = Modifier.size(76.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                        ) {
                            Icon(
                                if (s.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                if (s.isPlaying) "Pause" else "Play",
                                Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                        IconButton(onClick = { music.next() }, modifier = Modifier.size(56.dp)) { Icon(Icons.Filled.SkipNext, "Next", Modifier.size(36.dp)) }
                        IconButton(onClick = { music.cycleRepeat() }) {
                            Icon(
                                if (s.repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                                "Repeat",
                                tint = if (s.repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (!wide) {
                        Spacer(Modifier.weight(1f))
                        Row(Modifier.fillMaxWidth().navigationBarsPadding(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            PanelButton(Icons.AutoMirrored.Filled.Subject, "Lyrics") { panel = "lyrics" }
                            PanelButton(Icons.AutoMirrored.Filled.QueueMusic, "Up next") { panel = "queue" }
                        }
                    }
                }
            }
            if (wide) {
                Row(Modifier.fillMaxSize()) {
                    player(Modifier.weight(1f).fillMaxSize())
                    Column(Modifier.weight(1f).fillMaxSize().statusBarsPadding()) {
                        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PanelButton(Icons.AutoMirrored.Filled.QueueMusic, "Up next", selected = panel != "lyrics") { panel = "queue" }
                            PanelButton(Icons.AutoMirrored.Filled.Subject, "Lyrics", selected = panel == "lyrics") { panel = "lyrics" }
                        }
                        if (panel == "lyrics") LyricsView(s) else QueueList(s)
                    }
                }
            } else {
                player(Modifier.fillMaxSize())
            }
        }

        if (!isWideScreen(context) && panel != null) {
            ModalBottomSheet(onDismissRequest = { panel = null }, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                Box(Modifier.fillMaxWidth().height(520.dp)) {
                    if (panel == "lyrics") LyricsView(s) else QueueList(s)
                }
            }
        }
    }
}

/** Wide layouts show queue and lyrics side by side instead of in a sheet. */
private fun isWideScreen(context: android.content.Context): Boolean {
    val cfg = context.resources.configuration
    return cfg.screenWidthDp > cfg.screenHeightDp && cfg.screenWidthDp >= 600
}

@Composable
private fun PanelButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(if (selected) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun QueueList(s: MusicState) {
    val music = LocalContext.current.container.music
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = s.index.coerceAtLeast(0))
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        itemsIndexed(s.queue, key = { i, e -> "$i-${e.itemId}" }) { i, e ->
            val current = i == s.index
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { music.skipTo(i) }
                    .background(if (current) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else androidx.compose.ui.graphics.Color.Transparent)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(44.dp).clip(MaterialTheme.shapes.extraSmall)) { ArtImage(e.artworkUri, Modifier.fillMaxSize()) }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(
                        e.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    e.artist?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
                if (!current) {
                    IconButton(onClick = { music.removeAt(i) }) { Icon(Icons.Filled.Close, "Remove from queue", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
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
        MessageView(Icons.Outlined.MusicNote, if (loader.state is Load.Loading) "Loading lyrics" else "No lyrics for this song")
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
                style = MaterialTheme.typography.headlineSmall,
                color = if (active || !synced) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable(enabled = line.start != null) { line.start?.let { c.music.seekTo(it.ticksToMs()) } },
            )
        }
    }
}

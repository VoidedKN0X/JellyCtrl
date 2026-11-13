package com.vknxworks.jellyctrl.music

import com.vknxworks.jellyctrl.ui.components.CtrlIconSquare
import com.vknxworks.jellyctrl.ui.components.CtrlSectionLabel
import com.vknxworks.jellyctrl.ui.components.ctrlTopDivider
import com.vknxworks.jellyctrl.ui.theme.CtrlColors
import com.vknxworks.jellyctrl.ui.theme.CtrlShapes
import com.vknxworks.jellyctrl.ui.theme.CtrlType
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Stop
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
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.automirrored.outlined.Subject
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
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
import androidx.compose.ui.res.stringResource
import com.vknxworks.jellyctrl.R
import com.vknxworks.jellyctrl.ui.components.CtrlSheet


/** Floating card above the navigation bar while music plays. */
@Composable
fun MiniPlayer(onOpen: () -> Unit) {
    val context = LocalContext.current
    val music = context.container.music
    val s by music.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { music.connectIfRunning() }
    LaunchedEffect(s.hasMedia) { if (s.hasMedia) (context as? MainActivity)?.ensureNotificationPermission() }
    if (!s.hasMedia) return
    val seed: androidx.compose.ui.graphics.Color? = null
    ArtworkTint(seed) {
        Box(
            Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .padding(horizontal = 20.dp)
                .glass(CtrlShapes.Card),
        ) {
            Box {
                Row(
                    Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(start = 8.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(44.dp).clip(CtrlShapes.Control)) { ArtImage(s.artworkUri, Modifier.fillMaxSize()) }
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(s.title.orEmpty(), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(s.artist.orEmpty(), style = CtrlType.Mono, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    CtrlIconSquare(
                        if (s.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        if (s.isPlaying) stringResource(R.string.pause) else stringResource(R.string.play),
                        onClick = { music.togglePlay() },
                        container = CtrlColors.Accent,
                        content = CtrlColors.OnAccent,
                        border = null,
                    )
                    IconButton(onClick = { music.next() }) { Icon(Icons.Filled.SkipNext, stringResource(R.string.next_track), tint = CtrlColors.Muted) }
                }
                val p = if (s.durationMs > 0) (s.positionMs.toFloat() / s.durationMs).coerceIn(0f, 1f) else 0f
                Box(
                    Modifier.align(Alignment.BottomStart).padding(horizontal = 18.dp).fillMaxWidth(p).height(2.dp)
                        .clip(CtrlShapes.Dot).background(CtrlColors.Accent),
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
    val seed: androidx.compose.ui.graphics.Color? = null

    if (!s.hasMedia) {
        MessageView(Icons.Outlined.MusicNote, stringResource(R.string.nothing_playing), stringResource(R.string.play_an_album_a_playlist_or))
        return
    }

    ArtworkTint(seed) {
        BoxWithConstraints(
            Modifier.fillMaxSize().background(CtrlColors.Background),
        ) {
            val wide = maxWidth > maxHeight && maxWidth >= 600.dp
            val player: @Composable (Modifier) -> Unit = { mod ->
                Column(mod.statusBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        CtrlIconSquare(Icons.Outlined.KeyboardArrowDown, stringResource(R.string.close), onClick = nav::back)
                        Column(Modifier.weight(1f).padding(horizontal = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            CtrlSectionLabel(stringResource(R.string.np_playing_from))
                            Text(
                                s.album.orEmpty().uppercase(),
                                style = CtrlType.Label,
                                color = CtrlColors.Text,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        CtrlIconSquare(Icons.Outlined.Stop, stringResource(R.string.stop_playback), onClick = { music.stop(); nav.back() })
                    }
                    Spacer(Modifier.height(20.dp))
                    Box(
                        Modifier.widthIn(max = 440.dp).fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(28.dp))
                            .border(1.dp, CtrlColors.Outline, RoundedCornerShape(28.dp))
                            .clickable { s.itemId?.let { nav.navigate(ItemRoute(it)) } },
                    ) { ArtImage(s.artworkUri, Modifier.fillMaxSize()) }
                    Spacer(Modifier.height(28.dp))
                    Column(Modifier.widthIn(max = 440.dp).fillMaxWidth()) {
                        Text(s.title.orEmpty(), style = MaterialTheme.typography.headlineMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(s.artist.orEmpty(), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(16.dp))
                        SeekBar(
                            positionMs = scrub ?: s.positionMs,
                            durationMs = s.durationMs,
                            onScrub = { scrub = it },
                            onSeek = { music.seekTo(it); scrub = null },
                            trackColor = CtrlColors.ProgressTrack,
                            bufferColor = CtrlColors.ProgressTrack,
                        )
                        Row(Modifier.fillMaxWidth()) {
                            Text(formatClock(scrub ?: s.positionMs), style = CtrlType.Mono, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.weight(1f))
                            Text(formatClock(s.durationMs), style = CtrlType.Mono, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.widthIn(max = 440.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        IconButton(onClick = { music.toggleShuffle() }) {
                            Icon(Icons.Filled.Shuffle, stringResource(R.string.shuffle), tint = if (s.shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        CtrlIconSquare(Icons.Filled.SkipPrevious, stringResource(R.string.previous), onClick = { music.previous() }, size = 56.dp, iconSize = 22.dp, shape = CtrlShapes.MainButton)
                        IconButton(
                            onClick = { music.togglePlay() },
                            modifier = Modifier.size(88.dp).clip(RoundedCornerShape(30.dp)).background(CtrlColors.Accent),
                        ) {
                            Icon(
                                if (s.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                if (s.isPlaying) stringResource(R.string.pause) else stringResource(R.string.play),
                                Modifier.size(34.dp),
                                tint = CtrlColors.OnAccent,
                            )
                        }
                        CtrlIconSquare(Icons.Filled.SkipNext, stringResource(R.string.next), onClick = { music.next() }, size = 56.dp, iconSize = 22.dp, shape = CtrlShapes.MainButton)
                        IconButton(onClick = { music.cycleRepeat() }) {
                            Icon(
                                if (s.repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                                stringResource(R.string.repeat),
                                tint = if (s.repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (!wide) {
                        Spacer(Modifier.weight(1f))
                        Row(Modifier.fillMaxWidth().navigationBarsPadding(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            PanelButton(Icons.AutoMirrored.Outlined.Subject, stringResource(R.string.lyrics)) { panel = "lyrics" }
                            PanelButton(Icons.AutoMirrored.Outlined.QueueMusic, stringResource(R.string.up_next)) { panel = "queue" }
                        }
                    }
                }
            }
            if (wide) {
                Row(Modifier.fillMaxSize()) {
                    player(Modifier.weight(1f).fillMaxSize())
                    Column(Modifier.weight(1f).fillMaxSize().statusBarsPadding()) {
                        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PanelButton(Icons.AutoMirrored.Outlined.QueueMusic, stringResource(R.string.up_next), selected = panel != "lyrics") { panel = "queue" }
                            PanelButton(Icons.AutoMirrored.Outlined.Subject, stringResource(R.string.lyrics), selected = panel == "lyrics") { panel = "lyrics" }
                        }
                        if (panel == "lyrics") LyricsView(s) else QueueList(s)
                    }
                }
            } else {
                player(Modifier.fillMaxSize())
            }
        }

        if (!isWideScreen(context) && panel != null) {
            CtrlSheet(onDismissRequest = { panel = null }) {
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
            .clip(CtrlShapes.Button)
            .background(if (selected) CtrlColors.Accent else CtrlColors.Surface)
            .border(1.dp, if (selected) CtrlColors.Accent else CtrlColors.ControlOutline, CtrlShapes.Button)
            .clickable(onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val tint = if (selected) CtrlColors.OnAccent else CtrlColors.Text
        Icon(icon, null, Modifier.size(18.dp), tint = tint)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = tint)
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
                    .padding(horizontal = 12.dp)
                    .ctrlTopDivider()
                    .clickable { music.skipTo(i) }
                    .heightIn(min = 60.dp)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "%02d".format(i + 1),
                    style = CtrlType.Mono,
                    color = if (current) CtrlColors.AccentText else CtrlColors.Muted,
                    modifier = Modifier.width(30.dp),
                )
                Box(Modifier.size(44.dp).clip(CtrlShapes.Badge)) { ArtImage(e.artworkUri, Modifier.fillMaxSize()) }
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
                    IconButton(onClick = { music.removeAt(i) }) { Icon(Icons.Outlined.Close, stringResource(R.string.remove_from_queue), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
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
        MessageView(Icons.Outlined.MusicNote, if (loader.state is Load.Loading) stringResource(R.string.loading_lyrics) else stringResource(R.string.no_lyrics_for_this_song))
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

package com.vknxworks.jellyctrl.player

import com.vknxworks.jellyctrl.ui.components.CtrlIconSquare
import com.vknxworks.jellyctrl.ui.components.CtrlButton
import com.vknxworks.jellyctrl.ui.components.CtrlSecondaryButton
import com.vknxworks.jellyctrl.ui.components.CtrlStatusDot
import com.vknxworks.jellyctrl.ui.components.CtrlSectionLabel
import com.vknxworks.jellyctrl.ui.components.ctrlTopDivider
import com.vknxworks.jellyctrl.ui.theme.CtrlColors
import com.vknxworks.jellyctrl.ui.theme.CtrlShapes
import com.vknxworks.jellyctrl.ui.theme.CtrlType
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Search
import com.vknxworks.jellyctrl.ui.components.SubtitleSearchSheet
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import android.app.Activity
import android.content.Context
import android.media.AudioManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.outlined.ScreenLockRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.outlined.HighQuality
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.vknxworks.jellyctrl.container
import com.vknxworks.jellyctrl.data.api.TrickplayInfo
import com.vknxworks.jellyctrl.data.api.ticksToMs
import com.vknxworks.jellyctrl.ui.components.ArtImage
import com.vknxworks.jellyctrl.ui.components.ChoiceSheet
import com.vknxworks.jellyctrl.ui.components.GroupRow
import com.vknxworks.jellyctrl.ui.components.SeekBar
import com.vknxworks.jellyctrl.ui.components.formatClock
import com.vknxworks.jellyctrl.ui.components.landscapeImage
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.ui.res.stringResource
import com.vknxworks.jellyctrl.R
import com.vknxworks.jellyctrl.str
import com.vknxworks.jellyctrl.ui.components.CtrlSheet
import com.vknxworks.jellyctrl.ui.components.CtrlSwitch


// The player is always dark (content first), so these use the dark palette whatever the phone uses.
private val Scrim = CtrlColors.Dark.background.copy(alpha = 0.72f)
private val PlayerControl = CtrlColors.Dark.surface.copy(alpha = 0.85f)

@Composable
fun PlayerScreen(
    vm: PlayerViewModel,
    inPip: Boolean,
    onBack: () -> Unit,
    onEnterPip: () -> Unit,
    rotationLocked: Boolean,
    onToggleRotationLock: () -> Unit,
) {
    val context = LocalContext.current
    val c = context.container
    val settings by c.settings.settings.collectAsState()
    val engine by vm.engine.collectAsState()
    val st = engine?.state?.collectAsState()?.value ?: EngineState()
    var controls by remember { mutableStateOf(true) }
    var sheet by remember { mutableStateOf<String?>(null) }
    var gestureHint by remember { mutableStateOf<String?>(null) }
    var scrubMs by remember { mutableStateOf<Long?>(null) }
    var lastInteraction by remember { mutableLongStateOf(System.currentTimeMillis()) }

    fun poke() {
        lastInteraction = System.currentTimeMillis()
        controls = true
    }

    BackHandler { onBack() }

    LaunchedEffect(controls, lastInteraction, st.playWhenReady, sheet, scrubMs) {
        if (controls && st.playWhenReady && sheet == null && scrubMs == null) {
            delay(3500)
            controls = false
        }
    }
    LaunchedEffect(gestureHint) { if (gestureHint != null) { delay(900); gestureHint = null } }
    LaunchedEffect(vm.notice) { if (vm.notice != null) { delay(3500); vm.notice = null } }

    val seekStep = settings.seekStepSeconds * 1000L

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        engine?.VideoSurface(Modifier.fillMaxSize(), vm.resizeMode)

        if (!inPip) {
            GestureLayer(
                enabled = settings.gestures,
                onTap = { if (controls) controls = false else poke() },
                onDoubleTapSide = { forward ->
                    vm.seekBy(if (forward) seekStep else -seekStep)
                    gestureHint = str(if (forward) R.string.seek_step_forward else R.string.seek_step_back, settings.seekStepSeconds)
                },
                onHint = { gestureHint = it },
                positionMs = st.positionMs,
                durationMs = st.durationMs,
                onScrub = { scrubMs = it },
                onScrubEnd = { ms -> vm.seekTo(ms); scrubMs = null },
            )
        }

        if (vm.loading || (st.buffering && vm.error == null && !st.ended)) {
            CircularProgressIndicator(Modifier.align(Alignment.Center).size(48.dp), color = CtrlColors.Accent, strokeWidth = 3.dp)
        }

        gestureHint?.let {
            Surface(Modifier.align(Alignment.Center), shape = CtrlShapes.Button, color = Scrim, border = BorderStroke(1.dp, CtrlColors.ControlOutline)) {
                Text(it, color = Color.White, style = CtrlType.MonoMedium.copy(fontSize = 15.sp), modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
            }
        }

        if (!inPip) {
            AnimatedVisibility(controls && vm.error == null, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.fillMaxSize()) {
                Controls(
                    vm = vm,
                    st = st,
                    scrubMs = scrubMs,
                    seekStepMs = seekStep,
                    onBack = onBack,
                    onScrub = { scrubMs = it; poke() },
                    onSeek = { vm.seekTo(it); scrubMs = null; poke() },
                    onSheet = { sheet = it; poke() },
                    onPoke = { poke() },
                )
            }

            // Skip and up-next sit above the seek bar whether or not the controls are showing.
            Box(
                Modifier.align(Alignment.BottomEnd).windowInsetsPadding(WindowInsets.safeDrawing).padding(end = 24.dp, bottom = 104.dp),
            ) {
                val seg = vm.activeSegment
                val next = vm.nextItem
                when {
                    seg != null -> Button(
                        onClick = { vm.skipSegment() },
                        shape = CtrlShapes.Button,
                        colors = ButtonDefaults.buttonColors(containerColor = CtrlColors.Accent, contentColor = CtrlColors.OnAccent),
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
                    ) {
                        Text(
                            when (seg.type) {
                                "Intro" -> stringResource(R.string.skip_intro)
                                "Outro" -> if (next != null) stringResource(R.string.next_episode) else stringResource(R.string.skip_credits)
                                "Recap" -> stringResource(R.string.skip_recap)
                                "Preview" -> stringResource(R.string.skip_preview)
                                else -> stringResource(R.string.skip)
                            },
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                    vm.showUpNext && next != null -> UpNextCard(
                        title = next.name.orEmpty(),
                        subtitle = if (next.type == "Episode") str(R.string.season_x_episode_x, next.parentIndexNumber?.toString() ?: "?", next.indexNumber?.toString() ?: "?") else next.productionYear?.toString(),
                        imageUrl = c.sessions.active.value?.client?.landscapeImage(next, 400),
                        remainingMs = (st.durationMs - st.positionMs).coerceAtLeast(0),
                        onPlay = { vm.playNext() },
                        onDismiss = { vm.dismissUpNext() },
                    )
                }
            }

            vm.notice?.let {
                Surface(
                    Modifier.align(Alignment.TopCenter).windowInsetsPadding(WindowInsets.safeDrawing).padding(top = 72.dp),
                    shape = CtrlShapes.Control,
                    color = Scrim,
                    border = BorderStroke(1.dp, CtrlColors.ControlOutline),
                ) { Text(it, color = Color.White, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) }
            }

            vm.error?.let { err ->
                ErrorOverlay(err, onRetry = { vm.retry() }, onSwitch = { vm.switchEngine() }, onBack = onBack, engineName = engine?.name)
            }
        }
    }

    when (sheet) {
        "tracks" -> TracksSheet(vm, onDismiss = { sheet = null }, onSearchSubtitles = { sheet = "subsearch" })
        "subsearch" -> {
            val cl = c.sessions.active.value?.client
            val cur = vm.item
            if (cl != null && cur != null) {
                SubtitleSearchSheet(
                    client = cl,
                    itemId = cur.id,
                    sourceId = vm.playback?.source?.id,
                    title = if (cur.type == "Episode") listOfNotNull(cur.seriesName, cur.name).joinToString(" · ") else cur.name,
                    onDismiss = { sheet = null },
                    onDownloaded = { stream -> sheet = null; vm.onSubtitleDownloaded(stream) },
                )
            } else {
                LaunchedEffect(Unit) { sheet = null }
            }
        }
        "settings" -> SettingsSheet(
            vm, st,
            rotationLocked = rotationLocked,
            onToggleRotationLock = onToggleRotationLock,
            onPip = { sheet = null; onEnterPip() },
            onDismiss = { sheet = null },
        )
    }
}

@Composable
private fun GestureLayer(
    enabled: Boolean,
    onTap: () -> Unit,
    onDoubleTapSide: (forward: Boolean) -> Unit,
    onHint: (String) -> Unit,
    positionMs: Long,
    durationMs: Long,
    onScrub: (Long) -> Unit,
    onScrubEnd: (Long) -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val audio = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    var brightness by remember { mutableFloatStateOf(activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0 } ?: 0.5f) }
    var volumeAcc by remember { mutableFloatStateOf(0f) }
    var dragStartPos by remember { mutableLongStateOf(0L) }
    var dragTarget by remember { mutableLongStateOf(0L) }
    var accum by remember { mutableFloatStateOf(0f) }
    val currentPos by rememberUpdatedState(positionMs)
    val currentDur by rememberUpdatedState(durationMs)

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        var mod = Modifier.fillMaxSize().pointerInput(Unit) {
            detectTapGestures(
                onTap = { onTap() },
                onDoubleTap = { offset ->
                    when {
                        offset.x < widthPx * 0.4f -> onDoubleTapSide(false)
                        offset.x > widthPx * 0.6f -> onDoubleTapSide(true)
                        else -> onTap()
                    }
                },
            )
        }
        if (enabled) {
            mod = mod
                .pointerInput(Unit) {
                    var leftSide = false
                    detectVerticalDragGestures(
                        onDragStart = { o -> leftSide = o.x < widthPx / 2; volumeAcc = 0f },
                        onVerticalDrag = { change, dy ->
                            change.consume()
                            if (leftSide && activity != null) {
                                brightness = (brightness - dy / heightPx).coerceIn(0.01f, 1f)
                                val lp = activity.window.attributes
                                lp.screenBrightness = brightness
                                activity.window.attributes = lp
                                onHint(str(R.string.brightness_x, (brightness * 100).roundToInt()))
                            } else {
                                val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                volumeAcc -= dy / heightPx * max * 1.5f
                                val steps = volumeAcc.toInt()
                                if (steps != 0) {
                                    volumeAcc -= steps
                                    val v = (audio.getStreamVolume(AudioManager.STREAM_MUSIC) + steps).coerceIn(0, max)
                                    audio.setStreamVolume(AudioManager.STREAM_MUSIC, v, 0)
                                }
                                val now = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
                                onHint(str(R.string.volume_x, (now * 100f / max).roundToInt()))
                            }
                        },
                    )
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragStartPos = currentPos; dragTarget = currentPos; accum = 0f },
                        onDragEnd = { if (currentDur > 0) onScrubEnd(dragTarget) },
                        onDragCancel = { onScrubEnd(dragStartPos) },
                        onHorizontalDrag = { change, dx ->
                            if (currentDur <= 0) return@detectHorizontalDragGestures
                            change.consume()
                            accum += dx
                            // Full screen width = 3 minutes, so precise seeking stays easy.
                            val delta = (accum / widthPx * 180_000).toLong()
                            dragTarget = (dragStartPos + delta).coerceIn(0, currentDur)
                            onScrub(dragTarget)
                            val d = delta / 1000
                            onHint(str(if (d >= 0) R.string.seek_hint_forward else R.string.seek_hint_back, abs(d), formatClock(dragTarget)))
                        },
                    )
                }
        }
        Box(mod)
    }
}

@Composable
private fun Controls(
    vm: PlayerViewModel,
    st: EngineState,
    scrubMs: Long?,
    seekStepMs: Long,
    onBack: () -> Unit,
    onScrub: (Long) -> Unit,
    onSeek: (Long) -> Unit,
    onSheet: (String) -> Unit,
    onPoke: () -> Unit,
) {
    val item = vm.item
    val pb = vm.playback
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(0f to Scrim, 0.22f to Color.Transparent, 0.68f to Color.Transparent, 1f to Scrim)),
    ) {
        // Top: back, title, and two buttons.
        Row(
            Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CtrlIconSquare(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back), onClick = onBack, container = PlayerControl)
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                val title = if (item?.type == "Episode") item.seriesName ?: item.name.orEmpty() else item?.name.orEmpty()
                Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (item?.type == "Episode") {
                    Text(
                        "S${item.parentIndexNumber ?: "?"} E${item.indexNumber ?: "?"}  ${item.name.orEmpty()}",
                        color = CtrlColors.Muted,
                        style = CtrlType.Mono,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (pb != null && (pb.audioStreams.size > 1 || pb.subtitleStreams.isNotEmpty() || vm.canSearchSubtitles)) {
                CtrlIconSquare(Icons.Outlined.Subtitles, stringResource(R.string.audio_and_subtitles), onClick = { onSheet("tracks") }, container = PlayerControl)
            }
            CtrlIconSquare(Icons.Outlined.Tune, stringResource(R.string.playback_options), onClick = { onSheet("settings") }, container = PlayerControl)
        }

        // Centre: back, play/pause, forward.
        Row(
            Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CtrlIconSquare(
                Icons.Filled.Replay10,
                stringResource(R.string.back_x_seconds, seekStepMs / 1000),
                onClick = { vm.seekBy(-seekStepMs); onPoke() },
                size = 56.dp,
                iconSize = 28.dp,
                shape = CtrlShapes.MainButton,
                container = PlayerControl,
            )
            IconButton(
                onClick = { vm.togglePlay(); onPoke() },
                modifier = Modifier.size(88.dp).clip(RoundedCornerShape(30.dp)).background(CtrlColors.Accent),
            ) {
                Icon(
                    when {
                        st.ended -> Icons.Filled.Replay
                        st.playWhenReady -> Icons.Filled.Pause
                        else -> Icons.Filled.PlayArrow
                    },
                    if (st.playWhenReady) stringResource(R.string.pause) else stringResource(R.string.play),
                    tint = CtrlColors.OnAccent,
                    modifier = Modifier.size(40.dp),
                )
            }
            CtrlIconSquare(
                Icons.Filled.Forward10,
                stringResource(R.string.forward_x_seconds, seekStepMs / 1000),
                onClick = { vm.seekBy(seekStepMs); onPoke() },
                size = 56.dp,
                iconSize = 28.dp,
                shape = CtrlShapes.MainButton,
                container = PlayerControl,
            )
        }

        // Bottom: time, seek bar, next.
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            // Only real live streams (TV channels) count as live. Before the player knows the file's
            // length, fall back to the runtime from the server so the seek bar shows straight away.
            val live = pb?.source?.isInfiniteStream == true || item?.type == "TvChannel" || item?.type == "Program"
            val duration = st.durationMs.takeIf { it > 0 } ?: (item?.runTimeTicks?.ticksToMs() ?: 0L)
            val shown = scrubMs ?: st.positionMs
            val trick = vm.trickplay
            if (scrubMs != null && trick != null && item != null) {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val frac = if (duration > 0) scrubMs.toFloat() / duration else 0f
                    val previewW = 184.dp
                    val x = (maxWidth - previewW) * frac
                    TrickplayPreview(trick, item.id, pb?.source?.id, scrubMs, previewW, Modifier.offset(x = x))
                }
                Spacer(Modifier.height(10.dp))
            }
            if (live) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                    CtrlStatusDot(CtrlColors.Accent)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.live).uppercase(), color = CtrlColors.Accent, style = CtrlType.LabelStrong)
                }
            } else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(formatClock(shown), color = Color.White, style = CtrlType.MonoMedium)
                    Spacer(Modifier.weight(1f))
                    val chapter = item?.chapters?.lastOrNull { it.startPositionTicks.ticksToMs() <= shown }?.name
                    if (chapter != null && (item?.chapters?.size ?: 0) > 1) {
                        Text(chapter, color = CtrlColors.Muted, style = CtrlType.Mono, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.weight(1f))
                    }
                    Text("−" + formatClock((duration - shown).coerceAtLeast(0)), color = CtrlColors.Muted, style = CtrlType.MonoMedium)
                    if (vm.nextItem != null) {
                        Spacer(Modifier.width(12.dp))
                        TextButton(onClick = { vm.playNext(); onPoke() }) {
                            Icon(Icons.Filled.SkipNext, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.next), color = Color.White, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
                SeekBar(
                    positionMs = shown,
                    durationMs = duration,
                    bufferedMs = st.bufferedMs,
                    onScrub = onScrub,
                    onSeek = onSeek,
                    activeColor = CtrlColors.Accent,
                )
            }
        }
    }
}

@Composable
private fun TrickplayPreview(info: TrickplayInfo, itemId: String, sourceId: String?, positionMs: Long, width: Dp, modifier: Modifier) {
    val client = LocalContext.current.container.sessions.active.value?.client ?: return
    if (info.width <= 0 || info.height <= 0) return
    val tilesPerImage = (info.tileWidth * info.tileHeight).coerceAtLeast(1)
    val index = (positionMs / info.interval.coerceAtLeast(1)).toInt().coerceAtMost((info.thumbnailCount - 1).coerceAtLeast(0))
    val imageIndex = index / tilesPerImage
    val within = index % tilesPerImage
    val col = within % info.tileWidth
    val row = within / info.tileWidth
    val scale = width / info.width.toFloat() // dp per source pixel
    val height = scale * info.height.toFloat()
    // Authenticated by the app-wide OkHttp interceptor (Coil shares that client), so no token in the URL.
    val url = client.url("Videos/$itemId/Trickplay/${info.width}/$imageIndex.jpg", mapOf("mediaSourceId" to sourceId)).toString()
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.width(width).height(height).clip(CtrlShapes.Control).border(1.dp, CtrlColors.ControlOutline, CtrlShapes.Control).clipToBounds().background(Color.Black)) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .wrapContentSize(Alignment.TopStart, unbounded = true)
                    .offset(x = -(scale * (col * info.width).toFloat()), y = -(scale * (row * info.height).toFloat()))
                    .size(scale * (info.width * info.tileWidth).toFloat(), scale * (info.height * info.tileHeight).toFloat()),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(formatClock(positionMs), color = Color.White, style = CtrlType.MonoMedium)
    }
}

@Composable
private fun UpNextCard(
    title: String,
    subtitle: String?,
    imageUrl: String?,
    remainingMs: Long,
    onPlay: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(Modifier.width(320.dp), shape = CtrlShapes.Card, color = CtrlColors.Surface.copy(alpha = 0.96f), border = BorderStroke(1.dp, CtrlColors.Outline)) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(112.dp).height(64.dp).clip(CtrlShapes.Control).clickable(onClick = onPlay)) {
                ArtImage(imageUrl, Modifier.fillMaxSize())
                Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.align(Alignment.Center))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.up_next_in_x_s, remainingMs / 1000).uppercase(), color = CtrlColors.Accent, style = CtrlType.Label)
                Text(title, color = Color.White, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                subtitle?.let { Text(it, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall, maxLines = 1) }
                Row {
                    TextButton(onClick = onPlay, contentPadding = PaddingValues(horizontal = 0.dp)) { Text(stringResource(R.string.play_now), color = CtrlColors.Accent) }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.hide), color = Color.White.copy(alpha = 0.7f)) }
                }
            }
        }
    }
}

@Composable
private fun ErrorOverlay(message: String, onRetry: () -> Unit, onSwitch: () -> Unit, onBack: () -> Unit, engineName: String?) {
    Box(Modifier.fillMaxSize().background(CtrlColors.Background.copy(alpha = 0.92f)), contentAlignment = Alignment.Center) {
        Column(Modifier.padding(32.dp).width(420.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(R.string.this_won_t_play_right_now), color = Color.White, style = MaterialTheme.typography.titleLarge)
            Text(message, color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CtrlButton(stringResource(R.string.try_again), onClick = onRetry, minHeight = 52.dp)
                if (engineName != null) {
                    CtrlSecondaryButton(if (engineName == "mpv") stringResource(R.string.use_media3) else stringResource(R.string.use_mpv), onClick = onSwitch, minHeight = 52.dp)
                }
            }
            TextButton(onClick = onBack) { Text(stringResource(R.string.close), color = Color.White.copy(alpha = 0.7f)) }
        }
    }
}

@Composable
private fun TracksSheet(vm: PlayerViewModel, onDismiss: () -> Unit, onSearchSubtitles: () -> Unit) {
    val pb = vm.playback ?: return
    CtrlSheet(onDismissRequest = onDismiss) {
        LazyColumn(Modifier.navigationBarsPadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
            if (pb.audioStreams.isNotEmpty()) {
                item { SheetHeader(stringResource(R.string.audio)) }
                items(pb.audioStreams, key = { "a" + it.index }) { s ->
                    CheckRow(
                        s.displayTitle ?: s.language ?: stringResource(R.string.track_x, s.index),
                        listOfNotNull(s.codec?.uppercase(), s.channelLayout).joinToString(" "),
                        pb.audioIndex == s.index,
                    ) { vm.selectAudio(s.index); onDismiss() }
                }
            }
            item { SheetHeader(stringResource(R.string.subtitles)) }
            item { CheckRow(stringResource(R.string.off), null, pb.subtitleIndex == null) { vm.selectSubtitle(-1); onDismiss() } }
            items(pb.subtitleStreams, key = { "s" + it.index }) { s ->
                CheckRow(
                    s.displayTitle ?: s.language ?: stringResource(R.string.track_x, s.index),
                    listOfNotNull(s.codec?.uppercase(), if (s.isExternal) stringResource(R.string.external_file) else null, if (s.isForced) "forced" else null).joinToString(", "),
                    pb.subtitleIndex == s.index,
                ) { vm.selectSubtitle(s.index); onDismiss() }
            }
            if (vm.canSearchSubtitles) {
                item {
                    GroupRow(
                        stringResource(R.string.subs_search_action),
                        stringResource(R.string.subs_search_hint),
                        icon = Icons.Outlined.Search,
                        iconTint = CtrlColors.AccentText,
                        onClick = onSearchSubtitles,
                    )
                }
            }
        }
    }
}

private val QUALITY: List<Pair<Long?, String>>
    get() = listOf<Pair<Long?, String>>(null to str(R.string.best_available)) +
        listOf(40, 20, 10, 8, 4, 2, 1).map { it * 1_000_000L to str(R.string.x_mbps, it) } +
        listOf(420_000L to str(R.string.x_kbps, 420))

private val SPEEDS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

@Composable
private fun SettingsSheet(
    vm: PlayerViewModel,
    st: EngineState,
    rotationLocked: Boolean,
    onToggleRotationLock: () -> Unit,
    onPip: () -> Unit,
    onDismiss: () -> Unit,
) {
    val pb = vm.playback
    var quality by remember { mutableStateOf(false) }
    CtrlSheet(onDismissRequest = onDismiss) {
        LazyColumn(Modifier.navigationBarsPadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                SheetHeader(stringResource(R.string.speed))
                ChipLine(SPEEDS.map { if (it == 1f) stringResource(R.string.normal) else "${it}×" }, SPEEDS.indexOfFirst { abs(st.speed - it) < 0.01f }) { vm.setSpeed(SPEEDS[it]) }
            }
            item {
                SheetHeader(stringResource(R.string.picture))
                ChipLine(ResizeMode.entries.map { it.label }, vm.resizeMode.ordinal) { vm.resizeMode = ResizeMode.entries[it] }
            }
            item { Spacer(Modifier.height(12.dp)) }
            if (pb != null && !pb.offline) {
                item {
                    GroupRow(
                        stringResource(R.string.quality),
                        QUALITY.firstOrNull { it.first == vm.maxBitrateOverride }?.second ?: stringResource(R.string.best_available),
                        icon = Icons.Outlined.HighQuality,
                        showChevron = true,
                        onClick = { quality = true },
                    )
                }
            }
            item {
                GroupRow(stringResource(R.string.picture_in_picture), icon = Icons.Outlined.PictureInPictureAlt, onClick = onPip)
            }
            item {
                GroupRow(stringResource(R.string.lock_rotation), icon = Icons.Outlined.ScreenLockRotation, onClick = onToggleRotationLock) {
                    CtrlSwitch(rotationLocked, onCheckedChange = { onToggleRotationLock() })
                }
            }
            item {
                GroupRow(
                    stringResource(if (pb?.engine == EngineKind.MPV) R.string.player_mpv else R.string.player_media3),
                    stringResource(if (pb?.engine == EngineKind.MPV) R.string.switch_to_media3_hint else R.string.switch_to_mpv_hint),
                    icon = Icons.Outlined.Memory,
                    onClick = { vm.switchEngine(); onDismiss() },
                )
            }
            if (pb != null) {
                item {
                    val video = pb.source.mediaStreams.firstOrNull { it.type == "Video" }
                    val audio = pb.audioStreams.firstOrNull { it.index == pb.audioIndex }
                    GroupRow(
                        when (pb.playMethod) {
                            "DirectPlay" -> stringResource(R.string.playing_the_original_file)
                            "DirectStream" -> stringResource(R.string.streaming_the_original_video)
                            else -> stringResource(R.string.converted_by_the_server)
                        },
                        listOfNotNull(
                            pb.source.container?.uppercase(),
                            video?.let { v -> listOfNotNull(v.codec?.uppercase(), v.width?.let { "${it}×${v.height}" }, v.videoRangeType).joinToString(" ") },
                            audio?.let { a -> listOfNotNull(a.codec?.uppercase(), a.channelLayout).joinToString(" ") },
                            pb.source.bitrate?.let { stringResource(R.string.x_mbps, it / 1_000_000) },
                            if (pb.offline) "downloaded" else null,
                        ).joinToString(", "),
                        icon = Icons.Outlined.Info,
                        iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
    if (quality) {
        ChoiceSheet(stringResource(R.string.quality), QUALITY, vm.maxBitrateOverride, onDismiss = { quality = false }) {
            vm.setMaxBitrate(it)
            quality = false
            onDismiss()
        }
    }
}

@Composable
private fun ChipLine(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(labels.size) { i ->
            val isSel = i == selected
            Text(
                labels[i],
                style = MaterialTheme.typography.labelLarge,
                color = if (isSel) CtrlColors.OnAccent else CtrlColors.Text,
                modifier = Modifier
                    .clip(CtrlShapes.Chip)
                    .background(if (isSel) CtrlColors.Accent else CtrlColors.Raised)
                    .border(1.dp, if (isSel) CtrlColors.Accent else CtrlColors.ControlOutline, CtrlShapes.Chip)
                    .clickable { onSelect(i) }
                    .padding(horizontal = 16.dp, vertical = 9.dp),
            )
        }
    }
}

@Composable
private fun SheetHeader(text: String) {
    CtrlSectionLabel(text, Modifier.padding(start = 24.dp, top = 16.dp, bottom = 10.dp))
}

@Composable
private fun CheckRow(title: String, subtitle: String?, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp).ctrlTopDivider().heightIn(min = 52.dp).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (selected) CtrlStatusDot(CtrlColors.Accent, Modifier.semantics { contentDescription = str(R.string.selected) })
    }
}

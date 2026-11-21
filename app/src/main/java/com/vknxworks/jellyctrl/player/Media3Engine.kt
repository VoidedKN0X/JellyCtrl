package com.vknxworks.jellyctrl.player

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.media3.ui.SubtitleView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.vknxworks.jellyctrl.data.api.JellyfinClient
import okhttp3.OkHttpClient
import com.vknxworks.jellyctrl.R
import com.vknxworks.jellyctrl.str

/**
 * [passthrough]: Dolby and DTS sound goes undecoded to an output that reports it can take it (Media3 checks
 * the connected output itself). Off: the audio output is treated as plain PCM only, so everything is decoded
 * on the phone (or, without a decoder for it, by mpv or the server).
 */
class Media3Engine(context: Context, http: OkHttpClient, passthrough: Boolean = false) : PlayerEngine {
    override val name = "Media3"

    // Read by the video view, which applies them again when they change.
    private var subScale by mutableFloatStateOf(1f)
    private var subColor by mutableIntStateOf(android.graphics.Color.WHITE)
    override val supportsSubtitleDelay = false

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(EngineState())
    override val state: StateFlow<EngineState> = _state.asStateFlow()

    private var pendingAudio: Int? = null
    private var pendingSub: SubSelection = SubSelection.Off
    private var tracksApplied = false

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setRenderersFactory(
            PassthroughRenderers(context, passthrough)
                .setEnableDecoderFallback(true)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
        )
        .setMediaSourceFactory(DefaultMediaSourceFactory(DefaultDataSource.Factory(context, OkHttpDataSource.Factory(http))))
        .setAudioAttributes(
            AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),
            true,
        )
        .setHandleAudioBecomingNoisy(true)
        .setSeekBackIncrementMs(10_000)
        .setSeekForwardIncrementMs(10_000)
        .setUsePlatformDiagnostics(false)
        .build()

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            _state.update {
                it.copy(
                    buffering = playbackState == Player.STATE_BUFFERING,
                    ended = playbackState == Player.STATE_ENDED,
                )
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            _state.update { it.copy(playWhenReady = playWhenReady) }
        }

        override fun onPlayerError(error: PlaybackException) {
            _state.update { it.copy(error = JellyfinClient.redact("${error.errorCodeName}: ${error.message ?: str(R.string.playback_failed)}")) }
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            _state.update { it.copy(videoWidth = videoSize.width, videoHeight = videoSize.height) }
        }

        override fun onTracksChanged(tracks: Tracks) {
            if (!tracksApplied && !tracks.isEmpty) {
                tracksApplied = true
                pendingAudio?.let { applyAudio(it) }
                applySub(pendingSub)
            }
        }
    }

    init {
        player.addListener(listener)
        scope.launch {
            while (isActive) {
                _state.update {
                    it.copy(
                        positionMs = player.currentPosition.coerceAtLeast(0),
                        durationMs = player.duration.takeIf { d -> d != C.TIME_UNSET } ?: 0,
                        bufferedMs = player.bufferedPosition,
                        speed = player.playbackParameters.speed,
                    )
                }
                delay(250)
            }
        }
    }

    override fun load(request: LoadRequest) {
        pendingAudio = request.audioOrdinal
        pendingSub = request.subtitle
        tracksApplied = false
        val subs = request.externalSubs.map { subtitleConfig(it) }
        val item = MediaItem.Builder()
            .setUri(request.url)
            .setMimeType(if (request.isHls) MimeTypes.APPLICATION_M3U8 else null)
            .setSubtitleConfigurations(subs)
            .build()
        _state.value = EngineState()
        player.setMediaItem(item, request.startMs)
        player.prepare()
        player.playWhenReady = true
    }

    override fun play() = player.play()
    override fun pause() = player.pause()
    override fun seekTo(ms: Long) = player.seekTo(ms.coerceAtLeast(0))
    override fun setSpeed(speed: Float) {
        player.playbackParameters = PlaybackParameters(speed)
    }

    override fun selectAudio(ordinal: Int) {
        pendingAudio = ordinal
        applyAudio(ordinal)
    }

    override fun selectSubtitle(selection: SubSelection) {
        pendingSub = selection
        applySub(selection)
    }

    override fun addExternalSubtitle(sub: ExternalSub) {
        val current = player.currentMediaItem ?: return
        val existing = current.localConfiguration?.subtitleConfigurations.orEmpty()
            .filter { it.id != EXT_PREFIX + sub.jellyfinIndex }
        val item = current.buildUpon()
            .setSubtitleConfigurations(existing + subtitleConfig(sub))
            .build()
        // Media3 only picks up new side-loaded files when the item is set again: same stream, same position.
        val position = player.currentPosition.coerceAtLeast(0)
        pendingSub = SubSelection.External(sub.jellyfinIndex)
        tracksApplied = false
        player.setMediaItem(item, position)
        player.prepare()
    }

    override fun setSubtitleStyle(scale: Float, argb: Int) {
        subScale = scale
        subColor = argb
    }

    /** Media3 has no subtitle delay; the player offers the mpv engine for it instead. */
    override fun setSubtitleDelay(ms: Long) = Unit

    private fun subtitleConfig(s: ExternalSub): MediaItem.SubtitleConfiguration =
        MediaItem.SubtitleConfiguration.Builder(Uri.parse(s.url))
            .setId(EXT_PREFIX + s.jellyfinIndex)
            .setMimeType(subtitleMime(s.codec))
            .setLanguage(s.language)
            .setLabel(s.title ?: s.language)
            .setSelectionFlags(0)
            .build()

    private fun applyAudio(ordinal: Int) {
        val groups = player.currentTracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
        val group = groups.getOrNull(ordinal) ?: return
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, 0))
            .build()
    }

    private fun applySub(selection: SubSelection) {
        val textGroups = player.currentTracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }
        val builder = player.trackSelectionParameters.buildUpon()
        when (selection) {
            SubSelection.Off -> builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            is SubSelection.Embedded -> {
                val embedded = textGroups.filter { g -> g.getTrackFormat(0).id?.contains(EXT_PREFIX) != true }
                val g = embedded.getOrNull(selection.ordinal)
                builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                if (g != null) builder.setOverrideForType(TrackSelectionOverride(g.mediaTrackGroup, 0))
            }
            is SubSelection.External -> {
                val id = EXT_PREFIX + selection.jellyfinIndex
                val g = textGroups.firstOrNull { grp -> grp.getTrackFormat(0).id?.contains(id) == true }
                builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                if (g != null) builder.setOverrideForType(TrackSelectionOverride(g.mediaTrackGroup, 0))
            }
        }
        player.trackSelectionParameters = builder.build()
    }

    override fun release() {
        scope.cancel()
        player.removeListener(listener)
        player.release()
    }

    @Composable
    override fun VideoSurface(modifier: Modifier, resizeMode: ResizeMode) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    setKeepContentOnPlayerReset(true)
                    player = this@Media3Engine.player
                }
            },
            update = { view ->
                view.player = player
                // Outlined text in the chosen colour and size, the same look mpv draws.
                val scale = subScale
                val color = subColor
                view.subtitleView?.let { subs ->
                    subs.setStyle(
                        CaptionStyleCompat(
                            color,
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT,
                            CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                            android.graphics.Color.BLACK,
                            null,
                        )
                    )
                    subs.setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * scale)
                }
                view.resizeMode = when (resizeMode) {
                    ResizeMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    ResizeMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    ResizeMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                }
            },
            onRelease = { it.player = null },
            modifier = modifier,
        )
    }

    /** The stock renderers, except that the audio output only takes undecoded Dolby/DTS when [passthrough] is on. */
    private class PassthroughRenderers(context: Context, private val passthrough: Boolean) : DefaultRenderersFactory(context) {
        override fun buildAudioSink(context: Context, enableFloatOutput: Boolean, enableAudioTrackPlaybackParams: Boolean): AudioSink? {
            if (passthrough) return super.buildAudioSink(context, enableFloatOutput, enableAudioTrackPlaybackParams)
            // Without a context the sink assumes a plain PCM output: no encoded passthrough.
            @Suppress("DEPRECATION")
            return DefaultAudioSink.Builder()
                .setEnableFloatOutput(enableFloatOutput)
                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                .build()
        }
    }

    companion object {
        private const val EXT_PREFIX = "ext-"

        fun subtitleMime(codec: String?): String = when (codec?.lowercase()) {
            "ass", "ssa" -> MimeTypes.TEXT_SSA
            "vtt", "webvtt" -> MimeTypes.TEXT_VTT
            "ttml" -> MimeTypes.APPLICATION_TTML
            else -> MimeTypes.APPLICATION_SUBRIP
        }
    }
}

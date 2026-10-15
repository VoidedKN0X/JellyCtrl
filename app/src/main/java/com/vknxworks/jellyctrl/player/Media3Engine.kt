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
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
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
import okhttp3.OkHttpClient

class Media3Engine(context: Context, http: OkHttpClient) : PlayerEngine {
    override val name = "Media3"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(EngineState())
    override val state: StateFlow<EngineState> = _state.asStateFlow()

    private var pendingAudio: Int? = null
    private var pendingSub: SubSelection = SubSelection.Off
    private var tracksApplied = false

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setRenderersFactory(
            DefaultRenderersFactory(context)
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
            _state.update { it.copy(error = "${error.errorCodeName}: ${error.message ?: "Playback failed"}") }
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
        val subs = request.externalSubs.map { s ->
            MediaItem.SubtitleConfiguration.Builder(Uri.parse(s.url))
                .setId(EXT_PREFIX + s.jellyfinIndex)
                .setMimeType(subtitleMime(s.codec))
                .setLanguage(s.language)
                .setLabel(s.title ?: s.language)
                .setSelectionFlags(0)
                .build()
        }
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

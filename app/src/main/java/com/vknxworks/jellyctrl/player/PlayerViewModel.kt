package com.vknxworks.jellyctrl.player

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vknxworks.jellyctrl.AppContainer
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import com.vknxworks.jellyctrl.data.api.BaseItemDto
import com.vknxworks.jellyctrl.data.api.ItemQuery
import com.vknxworks.jellyctrl.data.api.MediaSegment
import com.vknxworks.jellyctrl.data.api.MediaStream
import com.vknxworks.jellyctrl.data.api.ProgressReport
import com.vknxworks.jellyctrl.data.api.TrickplayInfo
import com.vknxworks.jellyctrl.data.api.closeLiveStream
import com.vknxworks.jellyctrl.data.api.episodes
import com.vknxworks.jellyctrl.data.api.item
import com.vknxworks.jellyctrl.data.api.items
import com.vknxworks.jellyctrl.data.api.mediaSegments
import com.vknxworks.jellyctrl.data.api.msToTicks
import com.vknxworks.jellyctrl.data.api.nextUp
import com.vknxworks.jellyctrl.data.api.playlistItems
import com.vknxworks.jellyctrl.data.api.reportProgress
import com.vknxworks.jellyctrl.data.api.reportStart
import com.vknxworks.jellyctrl.data.api.reportStopped
import com.vknxworks.jellyctrl.data.api.stopEncoding
import com.vknxworks.jellyctrl.data.api.ticksToMs
import com.vknxworks.jellyctrl.data.settings.PlayerEngineChoice
import com.vknxworks.jellyctrl.data.settings.SkipSegmentMode
import com.vknxworks.jellyctrl.ui.components.friendlyMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import com.vknxworks.jellyctrl.R
import com.vknxworks.jellyctrl.str

data class PlayerArgs(
    val itemId: String?,
    val startMs: Long?,
    val mediaSourceId: String?,
    val audioIndex: Int?,
    val subtitleIndex: Int?,
    val containerId: String?,
    val shuffle: Boolean,
)

/** How many redirects on the signed-in server are followed for mpv before giving up. */
private const val MAX_MPV_REDIRECTS = 5

class PlayerViewModel(
    private val c: AppContainer,
    private val appContext: Context,
    private val args: PlayerArgs,
) : ViewModel() {

    // ---------- observable UI state ----------
    var item by mutableStateOf<BaseItemDto?>(null)
        private set
    var playback by mutableStateOf<ResolvedPlayback?>(null)
        private set
    var loading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var activeSegment by mutableStateOf<MediaSegment?>(null)
        private set
    var nextItem by mutableStateOf<BaseItemDto?>(null)
        private set
    var showUpNext by mutableStateOf(false)
        private set
    var trickplay by mutableStateOf<TrickplayInfo?>(null)
        private set
    var resizeMode by mutableStateOf(ResizeMode.FIT)
    var maxBitrateOverride by mutableStateOf<Long?>(null)
        private set
    var finished by mutableStateOf(false)
        private set
    var notice by mutableStateOf<String?>(null)

    private val _engine = MutableStateFlow<PlayerEngine?>(null)
    val engine: StateFlow<PlayerEngine?> = _engine.asStateFlow()

    private var queue: List<String> = emptyList()
    private var queueIndex = 0
    private var segments: List<MediaSegment> = emptyList()
    private val skippedSegments = mutableSetOf<String>()
    private var engineOverride: PlayerEngineChoice? = null
    private var triedFallback = false
    private var reportedStart = false
    private var loadJob: Job? = null
    private var upNextDismissed = false

    private val settings get() = c.settings.settings.value
    private val client get() = c.sessions.active.value?.client

    init {
        viewModelScope.launch { buildQueueAndStart() }
        viewModelScope.launch { tickLoop() }
        viewModelScope.launch { watchEngine() }
    }

    // ---------- queue ----------

    private suspend fun buildQueueAndStart() {
        val cl = client
        val containerId = args.containerId
        try {
            if (containerId != null && cl != null) {
                val container = cl.item(containerId)
                val list = when (container.type) {
                    "Playlist" -> cl.playlistItems(containerId).filter { it.mediaType == "Video" }
                    else -> cl.items(
                        ItemQuery(
                            parentId = containerId,
                            recursive = true,
                            includeItemTypes = listOf("Episode", "Movie", "Video", "MusicVideo"),
                            sortBy = if (args.shuffle) listOf("Random") else listOf("ParentIndexNumber", "IndexNumber", "ProductionYear", "SortName"),
                            fields = listOf("MediaSources"),
                        ),
                        limit = 1000,
                    ).items
                }
                if (list.isEmpty()) throw IllegalStateException(str(R.string.nothing_to_play_here))
                queue = list.map { it.id }
                queueIndex = 0
                if (!args.shuffle && container.type == "Series") {
                    val next = runCatching { cl.nextUp(1, seriesId = containerId).firstOrNull()?.id }.getOrNull()
                    queueIndex = queue.indexOf(next).coerceAtLeast(0)
                }
                playIndex(queueIndex, null)
            } else {
                queue = listOfNotNull(args.itemId)
                playIndex(0, args.startMs, args.mediaSourceId, args.audioIndex, args.subtitleIndex)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            error = e.friendlyMessage()
            loading = false
        }
    }

    private fun playIndex(
        index: Int,
        startMs: Long?,
        mediaSourceId: String? = null,
        audioIndex: Int? = null,
        subtitleIndex: Int? = null,
    ) {
        val id = queue.getOrNull(index) ?: return
        queueIndex = index
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            reportStop()
            loading = true
            error = null
            showUpNext = false
            upNextDismissed = false
            nextItem = null
            activeSegment = null
            skippedSegments.clear()
            triedFallback = false
            try {
                val local = c.downloads.completedFile(id)
                val cl = client
                val it: BaseItemDto = if (cl != null && c.isOnline()) {
                    runCatching { cl.item(id) }.getOrElse { e -> c.downloads.find(id)?.item ?: throw e }
                } else {
                    c.downloads.find(id)?.item ?: throw IllegalStateException(str(R.string.you_re_offline_and_this_isn))
                }
                item = it
                val resumeMs = it.userData?.playbackPositionTicks?.ticksToMs() ?: 0L
                val runtime = it.runTimeTicks?.ticksToMs() ?: 0L
                val start = startMs ?: if (resumeMs > 0 && (runtime == 0L || resumeMs < runtime * 0.95)) resumeMs else 0L

                val resolved = if (local != null && mediaSourceId == null) {
                    PlaybackResolver.offline(it, local.absolutePath, engineOverride ?: settings.engine)
                } else {
                    val api = cl ?: throw IllegalStateException(str(R.string.not_signed_in))
                    PlaybackResolver.resolve(
                        api, id, engineOverride ?: settings.engine, currentMaxBitrate(), start,
                        mediaSourceId, audioIndex, subtitleIndex,
                    )
                }
                startEngine(resolved, start)
                loadExtras(it, resolved)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                error = e.friendlyMessage()
                loading = false
            }
        }
    }

    private fun currentMaxBitrate(): Long = maxBitrateOverride ?: settings.maxBitrateBps(c.isOnCellular())

    private suspend fun startEngine(resolved: ResolvedPlayback, startMs: Long) {
        val current = _engine.value
        val engine = if (current != null && current.kind() == resolved.engine) current else {
            current?.release()
            createEngine(resolved.engine).also { _engine.value = it }
        }
        val url = if (engine is MpvEngine && !resolved.offline) finalUrlForMpv(resolved.url) else resolved.url
        playback = resolved
        reportedStart = false
        engine.load(
            LoadRequest(
                url = url,
                startMs = startMs,
                isHls = resolved.isHls,
                externalSubs = resolved.externalSubs,
                audioOrdinal = resolved.audioOrdinal,
                subtitle = resolved.subtitle,
                title = item?.name.orEmpty(),
            )
        )
        engine.setSpeed(1f)
        loading = false
    }

    private fun createEngine(kind: EngineKind): PlayerEngine = when (kind) {
        EngineKind.MEDIA3 -> Media3Engine(appContext, c.http)
        EngineKind.MPV -> try {
            MpvEngine(appContext, settings.hwDecoding, ::mpvAuthHeader)
        } catch (e: Throwable) {
            notice = e.message?.let { str(R.string.mpv_unavailable_x_using_media3, it) } ?: str(R.string.mpv_unavailable_using_media3)
            Media3Engine(appContext, c.http)
        }
    }

    /**
     * mpv sends its request headers (and so the token) to wherever a redirect points. So for mpv the server's
     * redirects are followed here first, with OkHttp, which only authenticates the signed-in server, and mpv
     * gets the final address: it only adds the token while that is still the signed-in server.
     */
    private suspend fun finalUrlForMpv(url: String): String {
        if (mpvAuthHeader(url) == null) return url
        val noRedirects = c.http.newBuilder().followRedirects(false).followSslRedirects(false).build()
        var current = url
        repeat(MAX_MPV_REDIRECTS) {
            val from = current
            val target = withContext(Dispatchers.IO) {
                noRedirects.newCall(Request.Builder().url(from).header("Range", "bytes=0-0").build()).execute().use { r ->
                    if (!r.isRedirect) null else r.header("Location")?.let { from.toHttpUrlOrNull()?.resolve(it)?.toString() }
                }
            } ?: return from
            // Somewhere else: mpv opens that address without the token.
            if (mpvAuthHeader(target) == null) return target
            current = target
        }
        throw IllegalStateException(str(R.string.too_many_redirects))
    }

    /** Header line for mpv: only for the signed-in server, never for third-party or unapproved HTTP URLs. */
    private fun mpvAuthHeader(url: String): String? {
        val cl = client ?: return null
        val token = cl.accessToken?.takeIf { it.isNotBlank() } ?: return null
        val parsed = url.toHttpUrlOrNull() ?: return null
        if (!cl.ownsUrl(parsed) || !c.sessions.isCleartextAllowed(parsed)) return null
        return "X-Emby-Token: $token"
    }

        private fun PlayerEngine.kind() = if (this is MpvEngine) EngineKind.MPV else EngineKind.MEDIA3

    private suspend fun loadExtras(it: BaseItemDto, resolved: ResolvedPlayback) {
        val cl = client ?: return
        if (!resolved.offline) {
            segments = cl.mediaSegments(it.id)
        }
        trickplay = it.trickplay[resolved.source.id ?: it.id]?.values
            ?.sortedBy { t -> t.width }
            ?.firstOrNull { t -> t.width >= 240 }
            ?: it.trickplay.values.firstOrNull()?.values?.minByOrNull { t -> t.width }
        nextItem = findNext(it)
    }

    private suspend fun findNext(current: BaseItemDto): BaseItemDto? {
        val cl = client ?: return null
        queue.getOrNull(queueIndex + 1)?.let { id -> return runCatching { cl.item(id) }.getOrNull() }
        if (current.type != "Episode") return null
        val seriesId = current.seriesId ?: return null
        return runCatching {
            val all = cl.episodes(seriesId)
            val idx = all.indexOfFirst { e -> e.id == current.id }
            if (idx >= 0) all.getOrNull(idx + 1) else null
        }.getOrNull()
    }

    // ---------- controls ----------

    fun togglePlay() {
        val e = _engine.value ?: return
        if (e.state.value.ended) {
            e.seekTo(0); e.play()
        } else if (e.state.value.playWhenReady) e.pause() else e.play()
    }

    fun seekTo(ms: Long) {
        _engine.value?.seekTo(ms)
        sendProgress()
    }

    fun seekBy(deltaMs: Long) {
        val e = _engine.value ?: return
        val dur = e.state.value.durationMs
        val target = (e.state.value.positionMs + deltaMs).coerceAtLeast(0).let { if (dur > 0) it.coerceAtMost(dur - 1000) else it }
        seekTo(target)
    }

    fun setSpeed(speed: Float) {
        _engine.value?.setSpeed(speed)
    }

    fun skipSegment() {
        val seg = activeSegment ?: return
        skippedSegments += seg.key()
        activeSegment = null
        val end = seg.endTicks.ticksToMs()
        val dur = _engine.value?.state?.value?.durationMs ?: 0L
        if (seg.type == "Outro" && nextItem != null && (dur == 0L || end >= dur - 5_000)) playNext() else seekTo(end)
    }

    fun playNext() {
        val next = nextItem ?: return
        if (queue.getOrNull(queueIndex + 1) == next.id) {
            playIndex(queueIndex + 1, null)
        } else {
            queue = queue.take(queueIndex + 1) + next.id
            playIndex(queueIndex + 1, null)
        }
    }

    fun hasPrevious(): Boolean = queueIndex > 0
    fun playPrevious() {
        if (queueIndex > 0) playIndex(queueIndex - 1, null)
    }

    fun dismissUpNext() {
        showUpNext = false
        upNextDismissed = true
    }

    fun selectAudio(index: Int) {
        val pb = playback ?: return
        if (pb.audioIndex == index) return
        if (!pb.isTranscoding) {
            val ordinal = pb.audioStreams.filter { !it.isExternal }.indexOfFirst { it.index == index }
            if (ordinal >= 0) {
                _engine.value?.selectAudio(ordinal)
                playback = pb.copy(audioIndex = index, audioOrdinal = ordinal)
                sendProgress()
                return
            }
        }
        reload(audioIndex = index, subtitleIndex = pb.subtitleIndex ?: -1)
    }

    /** index = MediaStream index, or -1 for off. */
    fun selectSubtitle(index: Int) {
        val pb = playback ?: return
        val engine = _engine.value ?: return
        if (index == -1) {
            if (pb.isTranscoding && pb.subtitleIndex != null && pb.externalSubs.none { it.jellyfinIndex == pb.subtitleIndex }) {
                reload(audioIndex = pb.audioIndex, subtitleIndex = -1)
            } else {
                engine.selectSubtitle(SubSelection.Off)
                playback = pb.copy(subtitleIndex = null, subtitle = SubSelection.Off)
            }
            sendProgress()
            return
        }
        val ext = pb.externalSubs.firstOrNull { it.jellyfinIndex == index }
        if (ext != null) {
            engine.selectSubtitle(SubSelection.External(index))
            playback = pb.copy(subtitleIndex = index, subtitle = SubSelection.External(index))
            sendProgress()
            return
        }
        if (!pb.isTranscoding) {
            val ordinal = pb.subtitleStreams.filter { !it.isExternal }.indexOfFirst { it.index == index }
            if (ordinal >= 0) {
                engine.selectSubtitle(SubSelection.Embedded(ordinal))
                playback = pb.copy(subtitleIndex = index, subtitle = SubSelection.Embedded(ordinal))
                sendProgress()
                return
            }
        }
        // External file not yet delivered, or a transcode that needs the server to add it.
        reload(audioIndex = pb.audioIndex, subtitleIndex = index)
    }

    /** Subtitle search needs the server: not for downloads played offline or live TV. */
    val canSearchSubtitles: Boolean
        get() {
            val pb = playback ?: return false
            val it = item ?: return false
            return !pb.offline && client != null && !pb.source.isInfiniteStream && it.type !in setOf("TvChannel", "Program")
        }

    /**
     * A subtitle was just downloaded to the server for the current item ([stream] is its new MediaStream, or null
     * when the server hasn't listed it yet). Refreshes the item's streams and switches to the new subtitle: side-loaded
     * into the running engine when it's a direct stream and nothing else moved, otherwise by reloading at the same spot.
     */
    fun onSubtitleDownloaded(stream: MediaStream?) {
        val pb = playback ?: return
        val id = item?.id ?: return
        val cl = client ?: return
        viewModelScope.launch {
            try {
                val fresh = cl.item(id)
                item = fresh
                val freshSource = fresh.mediaSources.firstOrNull { it.id == pb.source.id } ?: fresh.mediaSources.firstOrNull()
                val target = stream?.let { s -> freshSource?.mediaStreams?.firstOrNull { it.type == "Subtitle" && it.index == s.index } }
                if (target == null) {
                    notice = str(R.string.subs_downloaded_not_ready)
                    return@launch
                }
                val oldStreams = pb.source.mediaStreams
                val freshStreams = freshSource!!.mediaStreams
                // Jellyfin may renumber streams when a file is added; then only a reload gets every index right.
                val unchanged = oldStreams.all { o -> freshStreams.any { f -> f.index == o.index && f.type == o.type && f.codec == o.codec && f.language == o.language && f.isExternal == o.isExternal } }
                val engine = _engine.value
                if (pb.isTranscoding || !unchanged || engine == null) {
                    notice = str(R.string.subs_downloaded_x, target.displayTitle ?: target.language ?: "")
                    reload(audioIndex = pb.audioIndex, subtitleIndex = target.index)
                    return@launch
                }
                val format = when (target.codec?.lowercase()) {
                    "vtt", "webvtt" -> "vtt"
                    "ass" -> "ass"
                    "ssa" -> "ssa"
                    else -> "srt"
                }
                val sub = ExternalSub(
                    jellyfinIndex = target.index,
                    url = cl.url("Videos/$id/${pb.source.id ?: id}/Subtitles/${target.index}/0/Stream.$format").toString(),
                    codec = format,
                    language = target.language,
                    title = target.displayTitle ?: target.title,
                )
                engine.addExternalSubtitle(sub)
                playback = pb.copy(
                    source = pb.source.copy(mediaStreams = freshStreams),
                    externalSubs = pb.externalSubs.filter { it.jellyfinIndex != sub.jellyfinIndex } + sub,
                    subtitleIndex = target.index,
                    subtitle = SubSelection.External(target.index),
                )
                notice = str(R.string.subs_downloaded_x, sub.title ?: sub.language ?: "")
                sendProgress()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                notice = e.friendlyMessage()
            }
        }
    }

    fun setMaxBitrate(bps: Long?) {
        maxBitrateOverride = bps
        val pb = playback ?: return
        reload(audioIndex = pb.audioIndex, subtitleIndex = pb.subtitleIndex ?: -1)
    }

    fun switchEngine() {
        val pb = playback ?: return
        engineOverride = if (pb.engine == EngineKind.MPV) PlayerEngineChoice.MEDIA3 else PlayerEngineChoice.MPV
        reload(audioIndex = pb.audioIndex, subtitleIndex = pb.subtitleIndex ?: -1)
    }

    fun retry() {
        val pb = playback
        reload(audioIndex = pb?.audioIndex, subtitleIndex = pb?.subtitleIndex)
    }

    /** Re-resolve the current item at the current position (track/quality/engine change). */
    private fun reload(audioIndex: Int?, subtitleIndex: Int?) {
        val pos = _engine.value?.state?.value?.positionMs ?: 0L
        val id = queue.getOrNull(queueIndex) ?: return
        val sourceId = playback?.source?.id?.takeIf { playback?.offline != true }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            reportStop()
            loading = true
            error = null
            try {
                val cl = client ?: throw IllegalStateException(str(R.string.not_signed_in))
                val resolved = PlaybackResolver.resolve(
                    cl, id, engineOverride ?: settings.engine, currentMaxBitrate(), pos,
                    sourceId, audioIndex, subtitleIndex,
                )
                startEngine(resolved, pos)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                error = e.friendlyMessage()
                loading = false
            }
        }
    }

    // ---------- engine monitoring, fallback, segments, up next ----------

    private suspend fun watchEngine() {
        engine.flatMapLatest { e -> e?.state ?: flowOf(EngineState()) }
            .distinctUntilChangedBy { Triple(it.error, it.playWhenReady, it.ended) }
            .collectLatest { st ->
                val pb = playback ?: return@collectLatest
                if (st.error != null) onEngineError(pb, st.error)
                else if (st.ended) onEnded()
                else sendProgress()
            }
    }

    private fun onEngineError(pb: ResolvedPlayback, message: String) {
        val pos = _engine.value?.state?.value?.positionMs ?: 0L
        val id = queue.getOrNull(queueIndex) ?: return
        if (triedFallback) {
            error = message
            return
        }
        triedFallback = true
        // Media3 couldn't decode it: try mpv. mpv failed on a direct stream: let the server transcode.
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                loading = true
                val cl = client ?: throw IllegalStateException(message)
                val resolved = when {
                    pb.offline -> PlaybackResolver.offline(item ?: throw IllegalStateException(message), pb.url.removePrefix("file://"),
                        if (pb.engine == EngineKind.MEDIA3) PlayerEngineChoice.MPV else PlayerEngineChoice.MEDIA3)
                    pb.engine == EngineKind.MEDIA3 && !pb.isTranscoding && settings.engine != PlayerEngineChoice.MEDIA3 -> {
                        notice = str(R.string.switched_to_mpv_for_this_file)
                        PlaybackResolver.resolve(cl, id, PlayerEngineChoice.MPV, currentMaxBitrate(), pos, pb.source.id, pb.audioIndex, pb.subtitleIndex ?: -1)
                    }
                    else -> {
                        notice = str(R.string.direct_play_failed_transcoding)
                        PlaybackResolver.resolve(cl, id, PlayerEngineChoice.MEDIA3, currentMaxBitrate(), pos, pb.source.id, pb.audioIndex, pb.subtitleIndex ?: -1, forceTranscode = true)
                    }
                }
                startEngine(resolved, pos)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                error = "$message\n${e.friendlyMessage()}"
                loading = false
            }
        }
    }

    private fun onEnded() {
        reportStop()
        if (nextItem != null && settings.autoplayNext) playNext() else finished = true
    }

    private suspend fun tickLoop() {
        var lastReport = 0L
        while (viewModelScope.isActive) {
            delay(500)
            val e = _engine.value ?: continue
            val st = e.state.value
            if (playback == null || loading) continue

            if (!reportedStart && !st.buffering && st.durationMs > 0 || !reportedStart && st.positionMs > 0) {
                reportedStart = true
                report { reportStart(it) }
            }

            val posTicks = st.positionMs.msToTicks()
            val seg = segments.firstOrNull { s ->
                (s.type == "Intro" || s.type == "Outro" || s.type == "Recap" || s.type == "Preview" || s.type == "Commercial") &&
                    posTicks >= s.startTicks && posTicks < s.endTicks - 20_000_000 && s.key() !in skippedSegments
            }
            val mode = when (seg?.type) {
                "Intro", "Recap", "Preview", "Commercial" -> settings.skipIntro
                "Outro" -> settings.skipOutro
                else -> SkipSegmentMode.OFF
            }
            when {
                seg == null -> activeSegment = null
                mode == SkipSegmentMode.AUTO -> {
                    activeSegment = seg
                    skipSegment()
                }
                mode == SkipSegmentMode.ASK -> activeSegment = seg
                else -> activeSegment = null
            }

            val remaining = st.durationMs - st.positionMs
            val outroStarted = segments.any { s -> s.type == "Outro" && posTicks >= s.startTicks }
            showUpNext = nextItem != null && !upNextDismissed && st.durationMs > 0 &&
                ((remaining in 1L..30_000L) || outroStarted)

            val now = System.currentTimeMillis()
            if (now - lastReport > 10_000 && st.playWhenReady) {
                lastReport = now
                sendProgress()
            }
        }
    }

    private fun MediaSegment.key() = id ?: "$type-$startTicks"

    // ---------- reporting ----------

    private fun currentReport(): ProgressReport? {
        val pb = playback ?: return null
        val it = item ?: return null
        val st = _engine.value?.state?.value ?: return null
        return ProgressReport(
            itemId = it.id,
            mediaSourceId = pb.source.id,
            playSessionId = pb.playSessionId,
            positionTicks = st.positionMs.msToTicks(),
            isPaused = !st.playWhenReady,
            playMethod = pb.playMethod,
            audioStreamIndex = pb.audioIndex,
            subtitleStreamIndex = pb.subtitleIndex ?: -1,
            liveStreamId = pb.source.liveStreamId,
            playbackRate = st.speed,
        )
    }

    private fun report(block: suspend com.vknxworks.jellyctrl.data.api.JellyfinClient.(ProgressReport) -> Unit) {
        val r = currentReport() ?: return
        val cl = client ?: return
        if (playback?.offline == true && !c.isOnline()) return
        c.appScope.launch { runCatching { cl.block(r) } }
    }

    private fun sendProgress() {
        if (reportedStart) report { reportProgress(it) }
    }

    private fun reportStop() {
        if (!reportedStart) return
        reportedStart = false
        val pb = playback
        report { r ->
            reportStopped(r)
            if (pb != null && pb.isTranscoding) pb.playSessionId?.let { stopEncoding(it) }
            pb?.source?.liveStreamId?.let { closeLiveStream(it) }
        }
    }

    override fun onCleared() {
        reportStop()
        _engine.value?.release()
        _engine.value = null
        super.onCleared()
    }
}

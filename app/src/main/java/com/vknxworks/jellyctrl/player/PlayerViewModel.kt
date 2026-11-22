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
import com.vknxworks.jellyctrl.data.api.ChapterInfo
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
    /** An explicit list of videos to play in order (another device asked for them), starting at [queueIndex]. */
    val queue: List<String>? = null,
    val queueIndex: Int = 0,
    /** Opened because the SyncPlay group started something: play what the group plays. */
    val sync: Boolean = false,
)

/** How many redirects on the signed-in server are followed for mpv before giving up. */
private const val MAX_MPV_REDIRECTS = 5

/** In a SyncPlay group an episode brings at most this many following episodes along as the group's queue. */
private const val MAX_SYNC_QUEUE = 50

/** Positions closer than this to where the group is are left alone. */
private const val SYNC_TOLERANCE_MS = 400L

/** Waiting for data this long while it should be playing is reported to the group, which then waits. */
private const val SYNC_STALL_MS = 1_500L

/** [PlayerViewModel.setSleepTimer]: nothing plays after the current video. */
const val SLEEP_AT_END = -1

/** Subtitle timing can be moved at most this far either way. */
const val MAX_SUBTITLE_DELAY_MS = 60_000L

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

    /** Sleep timer: when it pauses, as a [android.os.SystemClock.elapsedRealtime] time; null when not counting down. */
    var sleepAt by mutableStateOf<Long?>(null)
        private set
    /** Sleep timer set to "when this video ends": nothing plays after it. */
    var sleepAtEnd by mutableStateOf(false)
        private set
    /** The sleep timer paused playback: the screen may turn off until something plays again. */
    var sleepPaused by mutableStateOf(false)
        private set

    /** Subtitles shown this much later (positive) or earlier than their own timing; for this video only. */
    var subtitleDelayMs by mutableStateOf(0L)
        private set

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

    // ---------- SyncPlay state ----------
    private val sync get() = c.syncPlay

    /**
     * This player follows a SyncPlay group: play, pause and seeking are asked of the group, and the server's
     * answer (a command for everyone, with a time to carry it out) is what moves the player.
     */
    var syncActive by mutableStateOf(false)
        private set
    /** The group's id for what is loaded here (one entry of its queue). */
    private var syncItem: String? = null
    /** The group waits for this phone to say "ready" (after loading or a seek it was told to do). */
    private var syncReadyOwed = false
    private var syncReadyNotBefore = 0L
    /** The group was told this phone is waiting for data. */
    private var syncStallReported = false
    private var stallSince = 0L
    /** A command waiting for its moment. */
    private var syncTimed: Job? = null

    init {
        PlayerBridge.active = this
        syncActive = args.sync
        viewModelScope.launch { sync.queue.collect { q -> if (syncActive && q != null) syncLoad(q) } }
        viewModelScope.launch { sync.commands.collect { onSyncCommand(it) } }
        viewModelScope.launch {
            sync.group.collect { g ->
                if (g == null && syncActive) {
                    // Left the group (or it ended): what's loaded keeps playing, on its own from here.
                    syncActive = false
                    syncTimed?.cancel()
                    notice = str(R.string.syncplay_left_notice)
                }
            }
        }
        viewModelScope.launch { buildQueueAndStart() }
        viewModelScope.launch { tickLoop() }
        viewModelScope.launch { watchEngine() }
        // Subtitle size and colour follow the settings while the player is open (the sheet changes them live).
        viewModelScope.launch {
            c.settings.settings.collect { st -> _engine.value?.setSubtitleStyle(st.subtitleSizePercent / 100f, st.subtitleColor.argb) }
        }
    }

    // ---------- queue ----------

    private suspend fun buildQueueAndStart() {
        val cl = client
        val containerId = args.containerId
        try {
            if (args.sync) {
                // The group's queue is loaded by the collector in init; from now on the group waits for this phone again.
                sync.setIgnoreWait(false)
                if (sync.queue.value?.current == null) throw IllegalStateException(str(R.string.syncplay_nothing_playing))
                return
            }
            val inGroup = sync.group.value != null && cl != null && c.isOnline()
            val explicit = args.queue.orEmpty()
            if (explicit.isNotEmpty()) {
                queue = explicit
                queueIndex = args.queueIndex.coerceIn(0, explicit.lastIndex)
                if (inGroup) startInGroup(queue, queueIndex, args.startMs)
                else playIndex(queueIndex, args.startMs, args.mediaSourceId, args.audioIndex, args.subtitleIndex)
            } else if (containerId != null && cl != null) {
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
                if (inGroup) startInGroup(queue.take(1000), queueIndex, null) else playIndex(queueIndex, null)
            } else {
                queue = listOfNotNull(args.itemId)
                if (inGroup && queue.isNotEmpty()) startInGroup(queue, 0, args.startMs)
                else playIndex(0, args.startMs, args.mediaSourceId, args.audioIndex, args.subtitleIndex)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            error = e.friendlyMessage()
            loading = false
        }
    }

    /**
     * In a SyncPlay group: instead of playing here, the group gets [ids] as its queue. The server then tells
     * everyone, this phone included, what to load; [syncLoad] does that.
     */
    private suspend fun startInGroup(ids: List<String>, index: Int, startMs: Long?) {
        val cl = client ?: throw IllegalStateException(str(R.string.not_signed_in))
        syncActive = true
        loading = true
        val first = cl.item(ids[index])
        var list = ids
        var at = index
        // One episode: the following episodes come along, so "next episode" moves the whole group on.
        if (ids.size == 1 && first.type == "Episode" && first.seriesId != null) {
            val all = runCatching { cl.episodes(first.seriesId) }.getOrDefault(emptyList())
            val i = all.indexOfFirst { it.id == first.id }
            if (i >= 0) {
                list = all.drop(i).take(MAX_SYNC_QUEUE).map { it.id }
                at = 0
            }
        }
        val resume = first.userData?.playbackPositionTicks ?: 0L
        val runtime = first.runTimeTicks ?: 0L
        val startTicks = startMs?.msToTicks() ?: if (resume > 0 && (runtime == 0L || resume < runtime * 0.95)) resume else 0L
        sync.setIgnoreWait(false)
        sync.setQueue(list, at, startTicks)
        viewModelScope.launch {
            delay(12_000)
            if (syncActive && syncItem == null && playback == null && error == null) {
                error = str(R.string.syncplay_no_answer)
                loading = false
            }
        }
    }

    /** The group's queue changed: load its current item (paused; the group says when to play), or just keep the list. */
    private fun syncLoad(q: com.vknxworks.jellyctrl.data.realtime.SyncQueue) {
        val current = q.current ?: return
        queue = q.playlist.map { it.itemId }
        if (com.vknxworks.jellyctrl.data.realtime.sameId(current.playlistItemId, syncItem) && (playback != null || loadJob?.isActive == true)) {
            queueIndex = q.playingItemIndex
            // The same item announced again (after a reconnect): the group waits to hear that this phone is ready.
            if (q.reason == "NewPlaylist" || q.reason == "SetCurrentItem") syncReadyOwed = true
            return
        }
        syncItem = current.playlistItemId
        syncTimed?.cancel()
        syncReadyOwed = true
        syncReadyNotBefore = 0
        syncStallReported = false
        stallSince = 0
        playIndex(q.playingItemIndex, sync.positionNowTicks(q).ticksToMs())
    }

    /** Tells the group this phone is waiting for data ([buffering]) or ready, with where it is. */
    private fun syncSend(buffering: Boolean) {
        val id = syncItem ?: return
        val st = _engine.value?.state?.value ?: EngineState()
        val ticks = st.positionMs.msToTicks()
        val playing = st.playWhenReady
        viewModelScope.launch { runCatching { sync.reportState(buffering, ticks, playing, id) } }
    }

    /** Something here is about to reload (quality, track or player change): the group waits meanwhile. */
    private fun syncReportStall() {
        if (!syncActive || syncReadyOwed || syncStallReported) return
        syncStallReported = true
        syncSend(buffering = true)
    }

    private fun syncTick(st: EngineState) {
        val ready = !st.buffering && (st.durationMs > 0 || st.positionMs > 0)
        val now = android.os.SystemClock.elapsedRealtime()
        if (syncReadyOwed) {
            if (ready && now >= syncReadyNotBefore) {
                syncReadyOwed = false
                syncStallReported = false
                stallSince = 0
                syncSend(buffering = false)
            }
            return
        }
        if (st.playWhenReady && st.buffering && !st.ended) {
            if (stallSince == 0L) stallSince = now
            else if (!syncStallReported && now - stallSince > SYNC_STALL_MS) {
                syncStallReported = true
                syncSend(buffering = true)
            }
        } else {
            stallSince = 0
            if (syncStallReported && ready) {
                syncStallReported = false
                syncSend(buffering = false)
            }
        }
    }

    /** A command for everyone in the group: carried out at the moment the server names, on this phone's clock. */
    private fun onSyncCommand(cmd: com.vknxworks.jellyctrl.data.realtime.SyncCommand) {
        if (!syncActive || !com.vknxworks.jellyctrl.data.realtime.sameId(cmd.playlistItemId, syncItem)) return
        if (_engine.value == null) return
        val at = sync.localTimeOf(cmd.`when`) ?: System.currentTimeMillis()
        val target = cmd.positionTicks?.ticksToMs()
        fun position() = _engine.value?.state?.value?.positionMs ?: 0L
        syncTimed?.cancel()
        when (cmd.command) {
            "Unpause" -> syncTimed = viewModelScope.launch {
                val wait = at - System.currentTimeMillis()
                if (wait > 0) {
                    if (target != null && kotlin.math.abs(position() - target) > SYNC_TOLERANCE_MS) _engine.value?.seekTo(target)
                    // At most 10 s, like Pause: before the first clock measurement, or with a phone clock that is
                    // minutes off, the wait could otherwise leave this phone paused while the group plays.
                    delay(wait.coerceAtMost(10_000))
                } else if (target != null) {
                    // Late: go to where the group is by now.
                    val nowAt = target - wait
                    if (kotlin.math.abs(position() - nowAt) > SYNC_TOLERANCE_MS) _engine.value?.seekTo(nowAt)
                }
                _engine.value?.play()
            }
            "Pause" -> syncTimed = viewModelScope.launch {
                val wait = at - System.currentTimeMillis()
                if (wait > 0) delay(wait.coerceAtMost(10_000))
                _engine.value?.pause()
                if (target != null && kotlin.math.abs(position() - target) > SYNC_TOLERANCE_MS) _engine.value?.seekTo(target)
            }
            "Seek" -> {
                _engine.value?.pause()
                if (target != null) _engine.value?.seekTo(target)
                // The group waits until every phone says it is ready at the new position.
                syncReadyOwed = true
                syncReadyNotBefore = android.os.SystemClock.elapsedRealtime() + 700
            }
            "Stop" -> _engine.value?.pause()
        }
    }

    /** Asks the group for something; a refusal (no permission, connection lost) shows as a notice. */
    private fun syncRequest(block: suspend com.vknxworks.jellyctrl.data.realtime.SyncPlayManager.() -> Unit) {
        viewModelScope.launch {
            try {
                sync.block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                notice = e.friendlyMessage()
            }
        }
    }

    /**
     * The player left the screen without picture-in-picture, which pauses it: the group must not wait for a
     * phone nobody is watching, so it stops following. "Watch together" opens the group's video again.
     */
    fun leftScreen() {
        if (!syncActive) return
        stopFollowing()
        notice = str(R.string.syncplay_stopped_following)
    }

    /** Stops following the group (it no longer waits for this phone); what's loaded stays. */
    private fun stopFollowing() {
        if (!syncActive) return
        syncActive = false
        syncTimed?.cancel()
        c.appScope.launch { sync.setIgnoreWait(true) }
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
            // Another video: its subtitles have their own timing.
            subtitleDelayMs = 0
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

                refreshPassthrough()
                val resolved = if (local != null && mediaSourceId == null) {
                    offlinePlayback(it, local.absolutePath, engineOverride ?: settings.engine)
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

    /** A downloaded file, with the subtitles saved next to it. */
    private fun offlinePlayback(it: BaseItemDto, path: String, choice: PlayerEngineChoice): ResolvedPlayback {
        val entry = c.downloads.find(it.id)
        val subs = entry?.let { e -> c.downloads.subtitleFiles(e).map { (s, f) -> s to f.absolutePath } }.orEmpty()
        return PlaybackResolver.offline(it, path, choice, converted = entry?.converted == true, subtitles = subs)
    }

    private fun currentMaxBitrate(): Long = maxBitrateOverride ?: settings.maxBitrateBps(c.isOnCellular())

    private suspend fun startEngine(resolved: ResolvedPlayback, startMs: Long) {
        // Following a group: a reload in the middle (quality, track, player) makes the group wait, and
        // everything loads paused, because the group says when to play.
        syncReportStall()
        val paused = syncActive
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
                externalSubs = if (engine is MpvEngine && !resolved.offline) subsForMpv(resolved.externalSubs) else resolved.externalSubs,
                audioOrdinal = resolved.audioOrdinal,
                subtitle = resolved.subtitle,
                title = item?.name.orEmpty(),
            )
        )
        if (paused) engine.pause()
        engine.setSpeed(1f)
        engine.setSubtitleStyle(settings.subtitleSizePercent / 100f, settings.subtitleColor.argb)
        // mpv keeps the delay across files, so it is always set; Media3 has none (the delay stays 0 there).
        if (!engine.supportsSubtitleDelay) subtitleDelayMs = 0
        engine.setSubtitleDelay(subtitleDelayMs)
        loading = false
    }

    private fun createEngine(kind: EngineKind): PlayerEngine = when (kind) {
        EngineKind.MEDIA3 -> Media3Engine(appContext, c.http, settings.audioPassthrough)
        EngineKind.MPV -> try {
            val spdif = if (settings.audioPassthrough) AudioPassthrough.supported(appContext).map { it.mpv } else emptyList()
            MpvEngine(appContext, settings.hwDecoding, ::mpvAuthHeader, spdif)
        } catch (e: Throwable) {
            notice = e.message?.let { str(R.string.mpv_unavailable_x_using_media3, it) } ?: str(R.string.mpv_unavailable_using_media3)
            Media3Engine(appContext, c.http, settings.audioPassthrough)
        }
    }

    /**
     * Tells the format check which undecoded sound formats the output takes right now, before the server is
     * asked how to play something. Empty while "Audio passthrough" is off.
     */
    private fun refreshPassthrough() {
        CodecSupport.passthrough = if (settings.audioPassthrough) AudioPassthrough.jellyfinCodecs(appContext) else emptySet()
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

    /**
     * Subtitle files mpv loads separately get the same redirect check as the video: mpv gets each one's final
     * address, so a redirect away from the signed-in server never carries the token. A subtitle whose address
     * can't be checked is left out rather than handed to mpv unchecked.
     */
    private suspend fun subsForMpv(subs: List<ExternalSub>): List<ExternalSub> = subs.mapNotNull { s ->
        try {
            s.copy(url = finalUrlForMpv(s.url))
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            null
        }
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
        // In a group only the group's own queue decides what comes next.
        if (syncActive) return null
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
        if (syncActive) {
            val playing = e.state.value.playWhenReady
            syncRequest { if (playing) requestPause() else requestUnpause() }
            return
        }
        if (e.state.value.ended) {
            e.seekTo(0); e.play()
        } else if (e.state.value.playWhenReady) e.pause() else e.play()
    }

    fun seekTo(ms: Long) {
        if (syncActive) {
            // The whole group seeks: the server tells every phone, this one included.
            syncRequest { requestSeek(ms.coerceAtLeast(0).msToTicks()) }
            return
        }
        _engine.value?.seekTo(ms)
        sendProgress()
    }

    /** Play or pause as asked (another device controlling this one); nothing happens when it already is. */
    fun setPlaying(playing: Boolean) {
        val e = _engine.value ?: return
        if (e.state.value.playWhenReady != playing || (playing && e.state.value.ended)) togglePlay()
    }

    /** Closes the player (a "stop" from another device). */
    fun close() {
        finished = true
    }

    /** More videos for the queue (another device asked): right after the current one ([next]) or at the end. */
    fun addToQueue(ids: List<String>, next: Boolean) {
        if (syncActive || ids.isEmpty()) return
        queue = if (next) queue.take(queueIndex + 1) + ids + queue.drop(queueIndex + 1) else queue + ids
        val current = item ?: return
        viewModelScope.launch { nextItem = findNext(current) }
    }

    fun seekBy(deltaMs: Long) {
        val e = _engine.value ?: return
        val dur = e.state.value.durationMs
        val target = (e.state.value.positionMs + deltaMs).coerceAtLeast(0).let { if (dur > 0) it.coerceAtMost(dur - 1000) else it }
        seekTo(target)
    }

    fun setSpeed(speed: Float) {
        // A group plays at normal speed: anything else would drift away from the others.
        if (syncActive) return
        _engine.value?.setSpeed(speed)
    }

    // ---------- chapters ----------

    /** The chapters of what's playing, in order; empty for live TV and for files with fewer than two. */
    val chapters: List<ChapterInfo>
        get() {
            val it = item ?: return emptyList()
            if (playback?.source?.isInfiniteStream == true || it.type == "TvChannel" || it.type == "Program") return emptyList()
            return it.chapters.takeIf { ch -> ch.size > 1 }.orEmpty()
        }

    fun seekToChapter(index: Int) {
        val ch = chapters.getOrNull(index) ?: return
        seekTo(ch.startPositionTicks.ticksToMs())
    }

    fun nextChapter() {
        val pos = _engine.value?.state?.value?.positionMs ?: return
        chapterAfter(chapters.map { it.startPositionTicks.ticksToMs() }, pos)?.let { seekToChapter(it) }
    }

    /** Back to the start of this chapter, or to the one before when it has only just begun. */
    fun previousChapter() {
        val pos = _engine.value?.state?.value?.positionMs ?: return
        chapterBefore(chapters.map { it.startPositionTicks.ticksToMs() }, pos)?.let { seekToChapter(it) }
    }

    // ---------- sleep timer ----------

    /** [minutes] > 0: pause after that long; [SLEEP_AT_END]: nothing plays after this video; null: off. */
    fun setSleepTimer(minutes: Int?) {
        sleepAt = minutes?.takeIf { it > 0 }?.let { android.os.SystemClock.elapsedRealtime() + it * 60_000L }
        sleepAtEnd = minutes == SLEEP_AT_END
    }

    private fun sleepNow() {
        sleepAt = null
        sleepAtEnd = false
        // In a group the sleep timer pauses this phone only: it stops following, and the others carry on.
        stopFollowing()
        _engine.value?.pause()
        sleepPaused = true
        notice = str(R.string.sleep_paused)
    }

    // ---------- subtitle timing ----------

    fun setSubtitleDelay(ms: Long) {
        val e = _engine.value ?: return
        if (!e.supportsSubtitleDelay) return
        subtitleDelayMs = ms.coerceIn(-MAX_SUBTITLE_DELAY_MS, MAX_SUBTITLE_DELAY_MS)
        e.setSubtitleDelay(subtitleDelayMs)
    }

    /** [auto]: skipped by the "skip automatically" setting, not by a tap. */
    fun skipSegment(auto: Boolean = false) {
        val seg = activeSegment ?: return
        skippedSegments += seg.key()
        activeSegment = null
        val end = seg.endTicks.ticksToMs()
        val dur = _engine.value?.state?.value?.durationMs ?: 0L
        if (seg.type == "Outro" && nextItem != null && (dur == 0L || end >= dur - 5_000)) {
            // Credits that run to the end: skipping them starts the next episode, unless the sleep timer says
            // nothing plays after this video (a tap still goes on).
            if (auto && sleepAtEnd) sleepNow() else playNext()
        } else seekTo(end)
    }

    fun playNext() {
        if (syncActive) {
            val id = syncItem ?: return
            if (queue.getOrNull(queueIndex + 1) != null) syncRequest { nextItem(id) }
            return
        }
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
        if (syncActive) {
            val id = syncItem ?: return
            if (queueIndex > 0) syncRequest { previousItem(id) }
            return
        }
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
                engine.addExternalSubtitle(if (engine is MpvEngine) sub.copy(url = finalUrlForMpv(sub.url)) else sub)
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
                refreshPassthrough()
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
                // Playing again (by any button, the notification or a remote): the sleep timer's pause is over.
                if (st.playWhenReady && !st.ended) sleepPaused = false
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
                    pb.offline -> offlinePlayback(item ?: throw IllegalStateException(message), pb.url.removePrefix("file://"),
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
        if (sleepAtEnd) {
            // "When this video ends": nothing plays next, and the screen may turn off.
            sleepAtEnd = false
            sleepAt = null
            sleepPaused = true
            finished = true
            return
        }
        if (syncActive) {
            // The group moves on together; the server only acts on the first phone that asks.
            val id = syncItem
            if (id != null && queue.getOrNull(queueIndex + 1) != null && settings.autoplayNext) syncRequest { nextItem(id) } else finished = true
            return
        }
        if (nextItem != null && settings.autoplayNext) playNext() else finished = true
    }

    private suspend fun tickLoop() {
        var lastReport = 0L
        while (viewModelScope.isActive) {
            delay(500)
            val e = _engine.value ?: continue
            val st = e.state.value
            if (playback == null || loading) continue

            sleepAt?.let { at -> if (android.os.SystemClock.elapsedRealtime() >= at) sleepNow() }
            if (syncActive) syncTick(st)

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
                // In a group nothing skips by itself (every phone would ask the group to seek at once): the button shows.
                mode == SkipSegmentMode.AUTO && !syncActive -> {
                    activeSegment = seg
                    skipSegment(auto = true)
                }
                mode == SkipSegmentMode.AUTO -> activeSegment = seg
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

    /**
     * Sends a playback report. A download watched offline is noted on the phone instead (its position,
     * and "watched" near the end), and sent to the server once the phone is online again.
     */
    private fun report(block: suspend com.vknxworks.jellyctrl.data.api.JellyfinClient.(ProgressReport) -> Unit) {
        val r = currentReport() ?: return
        val offlinePlay = playback?.offline == true
        val it = item
        fun keepForLater() {
            if (!offlinePlay || it == null) return
            val account = c.downloads.find(it.id)?.accountKey ?: c.sessions.active.value?.account?.key ?: return
            c.offlineProgress.record(account, it.id, r.positionTicks, it.runTimeTicks)
        }
        val cl = client
        if (cl == null || (offlinePlay && !c.isOnline())) {
            keepForLater()
            return
        }
        c.appScope.launch { runCatching { cl.block(r) }.onFailure { keepForLater() } }
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
        if (PlayerBridge.active === this) PlayerBridge.active = null
        // Still in the group, but with the player closed it must not wait for this phone.
        if (syncActive) c.appScope.launch { c.syncPlay.setIgnoreWait(true) }
        reportStop()
        _engine.value?.release()
        _engine.value = null
        super.onCleared()
    }
}

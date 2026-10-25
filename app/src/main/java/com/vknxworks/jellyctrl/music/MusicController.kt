package com.vknxworks.jellyctrl.music

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.vknxworks.jellyctrl.AppContainer
import com.vknxworks.jellyctrl.data.api.BaseItemDto
import com.vknxworks.jellyctrl.data.api.ItemQuery
import com.vknxworks.jellyctrl.data.api.instantMix
import com.vknxworks.jellyctrl.data.api.items
import com.vknxworks.jellyctrl.data.api.playlistItems
import com.vknxworks.jellyctrl.data.api.universalAudioUrl
import com.vknxworks.jellyctrl.ui.components.friendlyMessage
import com.vknxworks.jellyctrl.ui.components.primaryImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.vknxworks.jellyctrl.R
import com.vknxworks.jellyctrl.str

data class QueueEntry(val itemId: String, val title: String, val artist: String?, val artworkUri: String?)

data class MusicState(
    val connected: Boolean = false,
    val itemId: String? = null,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val artworkUri: String? = null,
    val isPlaying: Boolean = false,
    val buffering: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val queue: List<QueueEntry> = emptyList(),
    val index: Int = 0,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
) {
    val hasMedia: Boolean get() = itemId != null
}

/** App-side handle on the music service: builds queues from Jellyfin items and exposes playback state. */
class MusicController(private val context: Context, private val c: AppContainer) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var controller: MediaController? = null
    private val _state = MutableStateFlow(MusicState())
    val state: StateFlow<MusicState> = _state.asStateFlow()

    private val connecting = Mutex()

    private suspend fun controller(): MediaController = connecting.withLock {
        controller?.let { existing ->
            if (existing.isConnected) return@withLock existing
            // The music service stopped (closed from Recents, or ended by Android) while the app stayed in
            // memory: this connection is dead, so make a new one, which starts the service again.
            controller = null
            runCatching { existing.release() }
        }
        val token = SessionToken(context, ComponentName(context, MusicService::class.java))
        val future = MediaController.Builder(context, token)
            .setListener(object : MediaController.Listener {
                override fun onDisconnected(controller: MediaController) = onServiceGone(controller)
            })
            .buildAsync()
        val mc = suspendCancellableCoroutine<MediaController> { cont ->
            cont.invokeOnCancellation { MediaController.releaseFuture(future) }
            future.addListener({
                runCatching { future.get() }
                    .onSuccess { if (cont.isActive) cont.resume(it) else it.release() }
                    .onFailure { if (cont.isActive) cont.resumeWithException(it) }
            }, ContextCompat.getMainExecutor(context))
        }
        controller = mc
        mc.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) = refresh()
        })
        startPolling()
        mc
    }

    /** The music service went away: forget the connection and the mini player; the next action connects again. */
    private fun onServiceGone(gone: MediaController) {
        if (controller !== gone) return
        controller = null
        runCatching { gone.release() }
        _state.value = MusicState()
    }

    private var polling = false
    private fun startPolling() {
        if (polling) return
        polling = true
        scope.launch {
            while (true) {
                if (controller?.isPlaying != true) {
                    // Nothing playing: sleep until something does, instead of waking up twice a second in the background.
                    _state.first { it.isPlaying }
                    delay(500)
                    continue
                }
                refresh()
                delay(500)
            }
        }
    }

    private fun refresh() {
        val p = controller ?: return
        val md = p.mediaMetadata
        val queue = (0 until p.mediaItemCount).map { i ->
            val m = p.getMediaItemAt(i)
            QueueEntry(m.mediaId, m.mediaMetadata.title?.toString().orEmpty(), m.mediaMetadata.artist?.toString(), m.mediaMetadata.artworkUri?.toString())
        }
        _state.value = MusicState(
            connected = true,
            itemId = p.currentMediaItem?.mediaId,
            title = md.title?.toString(),
            artist = md.artist?.toString(),
            album = md.albumTitle?.toString(),
            artworkUri = md.artworkUri?.toString(),
            isPlaying = p.isPlaying,
            buffering = p.playbackState == Player.STATE_BUFFERING,
            positionMs = p.currentPosition.coerceAtLeast(0),
            durationMs = p.duration.coerceAtLeast(0),
            queue = queue,
            index = p.currentMediaItemIndex,
            shuffle = p.shuffleModeEnabled,
            repeatMode = p.repeatMode,
        )
    }

    private fun mediaItem(item: BaseItemDto): MediaItem {
        val client = c.api
        val local = c.downloads.completedFile(item.id)
        val uri = local?.let { Uri.fromFile(it).toString() }
            ?: client.universalAudioUrl(item.id, c.settings.settings.value.musicBitrateKbps * 1000L)
        val art = client.primaryImage(item, 600)
        return MediaItem.Builder()
            .setMediaId(item.id)
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(item.name)
                    .setArtist(item.artists.joinToString().ifBlank { item.albumArtist })
                    .setAlbumTitle(item.album)
                    .setAlbumArtist(item.albumArtist)
                    .setTrackNumber(item.indexNumber)
                    .setDiscNumber(item.parentIndexNumber)
                    .setArtworkUri(art?.let { Uri.parse(it) })
                    .setIsPlayable(true)
                    .setIsBrowsable(false)
                    .build()
            )
            .build()
    }

    private fun launch(block: suspend () -> Unit) {
        scope.launch {
            try {
                block()
            } catch (e: Throwable) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Toast.makeText(context, e.friendlyMessage(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun playItems(items: List<BaseItemDto>, startIndex: Int, shuffle: Boolean = false) = launch {
        val playable = items.filter { it.mediaType == "Audio" || it.type == "Audio" }
        if (playable.isEmpty()) return@launch
        val mc = controller()
        mc.setMediaItems(playable.map(::mediaItem), startIndex.coerceIn(0, playable.lastIndex), 0)
        mc.shuffleModeEnabled = shuffle
        mc.prepare()
        mc.play()
    }

    /** Resolve albums, artists, playlists and genres into their tracks. */
    private suspend fun tracksOf(item: BaseItemDto, shuffle: Boolean): List<BaseItemDto> {
        val api = c.api
        val sort = if (shuffle) listOf("Random") else listOf("Album", "ParentIndexNumber", "IndexNumber", "SortName")
        return when (item.type) {
            "Audio" -> listOf(item)
            "Playlist" -> api.playlistItems(item.id)
            "MusicArtist" -> api.items(ItemQuery(artistIds = listOf(item.id), includeItemTypes = listOf("Audio"), sortBy = sort), limit = 1000).items
            "MusicGenre" -> api.items(ItemQuery(genres = listOfNotNull(item.name), includeItemTypes = listOf("Audio"), sortBy = sort), limit = 1000).items
            else -> api.items(
                ItemQuery(parentId = item.id, includeItemTypes = listOf("Audio"), recursive = true, sortBy = if (shuffle) sort else listOf("ParentIndexNumber", "IndexNumber", "SortName")),
                limit = 2000,
            ).items
        }
    }

    fun playItem(item: BaseItemDto) = playContainer(item, shuffle = false)

    fun playContainer(item: BaseItemDto, shuffle: Boolean) = launch {
        val tracks = tracksOf(item, shuffle)
        if (tracks.isEmpty()) throw IllegalStateException(str(R.string.no_tracks_found))
        val mc = controller()
        mc.setMediaItems(tracks.map(::mediaItem), 0, 0)
        mc.shuffleModeEnabled = false
        mc.prepare()
        mc.play()
    }

    fun enqueue(item: BaseItemDto) = launch {
        val tracks = tracksOf(item, false)
        val mc = controller()
        val wasEmpty = mc.mediaItemCount == 0
        mc.addMediaItems(tracks.map(::mediaItem))
        if (wasEmpty) { mc.prepare(); mc.play() }
        Toast.makeText(context, str(R.string.added_x_to_queue, tracks.size), Toast.LENGTH_SHORT).show()
    }

    fun playNextInQueue(item: BaseItemDto) = launch {
        val tracks = tracksOf(item, false)
        val mc = controller()
        mc.addMediaItems((mc.currentMediaItemIndex + 1).coerceAtMost(mc.mediaItemCount), tracks.map(::mediaItem))
        if (mc.mediaItemCount == tracks.size) { mc.prepare(); mc.play() }
    }

    fun instantMix(item: BaseItemDto) = launch {
        val mix = c.api.instantMix(item.id)
        if (mix.isEmpty()) throw IllegalStateException(str(R.string.no_instant_mix_available))
        val mc = controller()
        mc.setMediaItems(mix.map(::mediaItem), 0, 0)
        mc.prepare()
        mc.play()
    }

    fun togglePlay() = launch { controller().let { if (it.isPlaying) it.pause() else { if (it.playbackState == Player.STATE_ENDED) it.seekTo(0, 0); it.play() } } }
    fun next() = launch { controller().seekToNextMediaItem() }
    fun previous() = launch { controller().seekToPrevious() }
    fun seekTo(ms: Long) = launch { controller().seekTo(ms) }
    fun skipTo(index: Int) = launch { controller().seekTo(index, 0) }
    fun removeAt(index: Int) = launch { controller().removeMediaItem(index) }
    fun move(from: Int, to: Int) = launch { controller().moveMediaItem(from, to) }
    fun toggleShuffle() = launch { controller().let { it.shuffleModeEnabled = !it.shuffleModeEnabled } }
    fun cycleRepeat() = launch {
        controller().let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }
    fun stop() = launch {
        controller().let { it.stop(); it.clearMediaItems() }
        refresh()
    }

    /** Reconnect to a still-running service after the app process was restarted. */
    fun connectIfRunning() {
        if (MusicService.running) launch { controller(); refresh() }
    }
}

package com.vknxworks.jellyctrl.data.realtime

import android.content.Context
import android.media.AudioManager
import android.widget.Toast
import com.vknxworks.jellyctrl.AppContainer
import com.vknxworks.jellyctrl.data.api.BaseItemDto
import com.vknxworks.jellyctrl.data.api.item
import com.vknxworks.jellyctrl.data.api.itemsByIds
import com.vknxworks.jellyctrl.data.api.ticksToMs
import com.vknxworks.jellyctrl.music.MusicService
import com.vknxworks.jellyctrl.player.PlayerActivity
import com.vknxworks.jellyctrl.player.PlayerBridge
import com.vknxworks.jellyctrl.ui.lock.AppLock
import com.vknxworks.jellyctrl.ui.nav.Navigator
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import kotlin.coroutines.cancellation.CancellationException

/**
 * This phone as a remote-control target: carries out what another Jellyfin device (the web app, another
 * phone) asks through the server. The server sends "Play", "Playstate" and "GeneralCommand" messages over
 * the WebSocket; only the commands listed in [COMMANDS] are announced to it.
 *
 * Nothing new starts while the app is locked, and a message is never shown then: only what is already
 * playing can be paused, moved or stopped, the same as from its notification.
 */
class RemoteControlTarget(private val c: AppContainer) {
    private val context: Context get() = c.context
    private val locked: Boolean get() = c.vault.enabled && !AppLock.unlocked

    /** Called on the main thread for every WebSocket message. */
    fun handle(message: SocketMessage) {
        if (!c.settings.settings.value.remoteControl) return
        val data = message.data as? JsonObject ?: return
        when (message.type) {
            "Play" -> if (!locked) play(data)
            "Playstate" -> playstate(data.text("Command") ?: return, data.long("SeekPositionTicks"))
            "GeneralCommand" -> general(data.text("Name") ?: return, data["Arguments"] as? JsonObject)
        }
    }

    // ---------- Play ----------

    private fun play(data: JsonObject) {
        val ids = (data["ItemIds"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.content }
            ?.filter { ID.matches(it) }?.take(MAX_ITEMS).orEmpty()
        if (ids.isEmpty()) return
        val command = data.text("PlayCommand") ?: "PlayNow"
        val startIndex = (data.int("StartIndex") ?: 0).coerceIn(0, ids.lastIndex)
        val startMs = data.long("StartPositionTicks")?.takeIf { it > 0 }?.ticksToMs()
        val sourceId = data.text("MediaSourceId")?.takeIf { ID.matches(it) }
        val audio = data.int("AudioStreamIndex")
        val subtitle = data.int("SubtitleStreamIndex")
        c.appScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            try {
                val api = c.sessions.active.value?.client ?: return@launch
                val first = api.item(ids[startIndex])
                when {
                    first.mediaType == "Audio" -> {
                        val tracks = if (ids.size == 1) listOf(first) else api.itemsByIds(ids)
                        when (command) {
                            "PlayNext" -> c.music.addToQueue(tracks, next = true)
                            "PlayLast" -> c.music.addToQueue(tracks, next = false)
                            "PlayShuffle" -> c.music.playItems(tracks, 0, shuffle = true)
                            "PlayInstantMix" -> c.music.instantMix(first)
                            else -> c.music.playItems(tracks, tracks.indexOfFirst { it.id == first.id }.coerceAtLeast(0))
                        }
                    }
                    first.type in Navigator.AUDIO_CONTAINERS || first.collectionType == "music" -> when (command) {
                        "PlayNext" -> c.music.playNextInQueue(first)
                        "PlayLast" -> c.music.enqueue(first)
                        "PlayInstantMix" -> c.music.instantMix(first)
                        else -> c.music.playContainer(first, shuffle = command == "PlayShuffle")
                    }
                    first.mediaType == "Video" || first.type in BaseItemDto.PLAYABLE_TYPES -> {
                        val open = PlayerBridge.active
                        if (open != null && (command == "PlayNext" || command == "PlayLast")) {
                            open.addToQueue(ids, next = command == "PlayNext")
                        } else {
                            PlayerActivity.startQueue(context, ids, startIndex, startMs, sourceId, audio, subtitle)
                        }
                    }
                    // A series, season, collection or video playlist: the player works out its episodes itself.
                    first.isFolder -> PlayerActivity.startContainer(context, first.id, shuffle = command == "PlayShuffle")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                // The controlling device sees that nothing started; there is nothing useful to show here.
            }
        }
    }

    // ---------- Playstate ----------

    private fun playstate(command: String, seekTicks: Long?) {
        val video = PlayerBridge.active
        if (video != null) {
            when (command) {
                "Stop" -> video.close()
                "Pause" -> video.setPlaying(false)
                "Unpause" -> video.setPlaying(true)
                "PlayPause" -> video.togglePlay()
                "Seek" -> seekTicks?.let { video.seekTo(it.ticksToMs()) }
                "NextTrack" -> video.playNext()
                "PreviousTrack" -> video.playPrevious()
                "Rewind" -> video.seekBy(-SKIP_MS)
                "FastForward" -> video.seekBy(SKIP_MS)
            }
            return
        }
        // Music: only when it is running, so a command never starts the music service by itself.
        if (!MusicService.running) return
        val music = c.music
        when (command) {
            "Stop" -> music.stop()
            "Pause" -> music.setPlaying(false)
            "Unpause" -> music.setPlaying(true)
            "PlayPause" -> music.togglePlay()
            "Seek" -> seekTicks?.let { music.seekTo(it.ticksToMs()) }
            "NextTrack" -> music.next()
            "PreviousTrack" -> music.previous()
            "Rewind" -> music.seekTo((music.state.value.positionMs - SKIP_MS).coerceAtLeast(0))
            "FastForward" -> music.seekTo(music.state.value.positionMs + SKIP_MS)
        }
    }

    // ---------- GeneralCommand ----------

    private fun general(name: String, args: JsonObject?) {
        val audio = context.getSystemService(AudioManager::class.java)
        when (name) {
            "VolumeUp" -> audio?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0)
            "VolumeDown" -> audio?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0)
            "Mute" -> audio?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0)
            "Unmute" -> audio?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0)
            "ToggleMute" -> audio?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, 0)
            "SetVolume" -> {
                val percent = args?.text("Volume")?.toIntOrNull()?.coerceIn(0, 100) ?: return
                val max = audio?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: return
                audio.setStreamVolume(AudioManager.STREAM_MUSIC, Math.round(percent * max / 100f), 0)
            }
            "SetAudioStreamIndex" -> args?.text("Index")?.toIntOrNull()?.let { PlayerBridge.active?.selectAudio(it) }
            "SetSubtitleStreamIndex" -> args?.text("Index")?.toIntOrNull()?.let { PlayerBridge.active?.selectSubtitle(it) }
            "DisplayMessage" -> {
                if (locked || !c.foreground.value) return
                val text = listOfNotNull(args?.text("Header"), args?.text("Text")).filter { it.isNotBlank() }.joinToString(": ")
                if (text.isNotBlank()) Toast.makeText(context, text.take(300), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
    private fun JsonObject.long(key: String): Long? = (this[key] as? JsonPrimitive)?.longOrNull
    private fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.intOrNull

    companion object {
        /** The general commands this app carries out, as announced to the server (POST /Sessions/Capabilities). */
        val COMMANDS = listOf(
            "VolumeUp", "VolumeDown", "Mute", "Unmute", "ToggleMute", "SetVolume",
            "SetAudioStreamIndex", "SetSubtitleStreamIndex", "DisplayMessage",
        )
        private val ID = Regex("^[0-9a-fA-F-]{32,36}$")
        private const val MAX_ITEMS = 300
        private const val SKIP_MS = 10_000L
    }
}

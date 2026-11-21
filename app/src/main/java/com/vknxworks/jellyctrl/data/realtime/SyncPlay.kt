package com.vknxworks.jellyctrl.data.realtime

import android.widget.Toast
import com.vknxworks.jellyctrl.AppContainer
import com.vknxworks.jellyctrl.R
import com.vknxworks.jellyctrl.data.api.JellyfinClient
import com.vknxworks.jellyctrl.data.api.JellyfinJson
import com.vknxworks.jellyctrl.player.PlayerActivity
import com.vknxworks.jellyctrl.player.PlayerBridge
import com.vknxworks.jellyctrl.str
import com.vknxworks.jellyctrl.ui.components.isoToEpochMs
import com.vknxworks.jellyctrl.ui.lock.AppLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.put
import java.time.Instant
import kotlin.coroutines.cancellation.CancellationException

/** A SyncPlay group as the server lists it (GET /SyncPlay/List, and the "GroupJoined" update). */
@Serializable
data class SyncGroup(
    val groupId: String = "",
    val groupName: String = "",
    /** Idle, Waiting, Paused or Playing. */
    val state: String = "Idle",
    val participants: List<String> = emptyList(),
)

@Serializable
data class SyncQueueItem(val itemId: String = "", val playlistItemId: String = "")

/** The group's play queue ("PlayQueue" update): what plays, from where, and whether it was playing at [lastUpdate]. */
@Serializable
data class SyncQueue(
    val reason: String = "",
    val lastUpdate: String? = null,
    val playlist: List<SyncQueueItem> = emptyList(),
    val playingItemIndex: Int = 0,
    val startPositionTicks: Long = 0,
    val isPlaying: Boolean = false,
) {
    val current: SyncQueueItem? get() = playlist.getOrNull(playingItemIndex)
}

/** A playback command for everyone in the group ("SyncPlayCommand"): Unpause, Pause, Seek or Stop, to carry out at [when]. */
@Serializable
data class SyncCommand(
    val groupId: String = "",
    val playlistItemId: String = "",
    val `when`: String? = null,
    val positionTicks: Long? = null,
    val command: String = "",
)

@Serializable
private data class UtcTime(val requestReceptionTime: String = "", val responseTransmissionTime: String = "")

/** Ids compare without dashes or case: the server writes them in more than one way. */
fun sameId(a: String?, b: String?): Boolean =
    a != null && b != null && a.replace("-", "").equals(b.replace("-", ""), ignoreCase = true)

/**
 * SyncPlay ("watch together"): being in one of the server's groups, keeping the phone's clock lined up with
 * the server's, and passing the group's queue and commands on to the video player. The player carries the
 * commands out; this class only talks to the server (the /SyncPlay endpoints and the WebSocket updates).
 */
class SyncPlayManager(private val c: AppContainer) {
    private val _group = MutableStateFlow<SyncGroup?>(null)
    /** The group this phone is in, or null. */
    val group: StateFlow<SyncGroup?> = _group.asStateFlow()

    private val _queue = MutableStateFlow<SyncQueue?>(null)
    /** The group's latest play queue. */
    val queue: StateFlow<SyncQueue?> = _queue.asStateFlow()

    private val _commands = MutableSharedFlow<SyncCommand>(extraBufferCapacity = 16)
    val commands: SharedFlow<SyncCommand> = _commands.asSharedFlow()

    /** Server clock minus this phone's clock, in ms (from the round trip with the least delay). */
    @Volatile private var offsetMs = 0L
    private var bestRoundTripMs = Long.MAX_VALUE
    private var clockJob: Job? = null

    private val client: JellyfinClient? get() = c.sessions.active.value?.client

    /** The server's time now, in ms since 1970. */
    fun serverNow(): Long = System.currentTimeMillis() + offsetMs

    /** A server time (ISO text, as in a command's "When") as a time on this phone's clock, or null when unreadable. */
    fun localTimeOf(serverIso: String?): Long? = isoToEpochMs(serverIso)?.let { it - offsetMs }

    /** Where the queue's current item is now, in ticks: its start position plus the time it has been playing. */
    fun positionNowTicks(q: SyncQueue): Long {
        val updated = isoToEpochMs(q.lastUpdate)
        val elapsedMs = if (q.isPlaying && updated != null) (serverNow() - updated).coerceAtLeast(0) else 0L
        return q.startPositionTicks + elapsedMs * 10_000
    }

    // ---------- groups ----------

    suspend fun listGroups(): List<SyncGroup> = api().get("SyncPlay/List")

    /**
     * The SyncPlay screen is open: the live connection is wanted even before a group is joined, because the
     * server's "you joined" answer and everything after it arrive over that connection.
     */
    val screenOpen = MutableStateFlow(false)

    suspend fun create(name: String) {
        awaitConnection()
        val answer = api().post<JsonElement>("SyncPlay/New", body = buildJsonObject { put("GroupName", name) })
        // Current servers answer with the new group; the "GroupJoined" update over the connection says the same.
        if (_group.value == null) decode<SyncGroup>(answer as? JsonObject)?.takeIf { it.groupId.isNotBlank() }?.let { joined(it) }
    }

    suspend fun join(groupId: String) {
        awaitConnection()
        api().postUnit("SyncPlay/Join", body = buildJsonObject { put("GroupId", groupId) })
        // Normally "GroupJoined" has arrived by now; if it got lost, the group list says whether it worked.
        if (!sameId(_group.value?.groupId, groupId)) {
            delay(1_500)
            if (!sameId(_group.value?.groupId, groupId)) {
                runCatching { listGroups().firstOrNull { sameId(it.groupId, groupId) } }.getOrNull()?.let { joined(it) }
            }
        }
    }

    /** When this phone last (re)joined its group, so a group list fetched just before that can't undo it. */
    private var joinedAt = 0L

    private fun joined(g: SyncGroup) {
        if (!sameId(g.groupId, _group.value?.groupId)) _queue.value = null
        joinedAt = android.os.SystemClock.elapsedRealtime()
        _group.value = g
        startClock()
    }

    /** Waits a few seconds for the live connection; without it the group's updates can't arrive. */
    private suspend fun awaitConnection() {
        if (c.socket.connected.value) return
        val ok = kotlinx.coroutines.withTimeoutOrNull(8_000) { c.socket.connected.first { it } }
        if (ok == null) throw java.io.IOException(str(R.string.syncplay_no_connection))
    }

    suspend fun leave() {
        runCatching { api().postUnit("SyncPlay/Leave") }
        // The server confirms with "GroupLeft"; don't depend on it (the connection may be down).
        left()
    }

    // ---------- playback requests (the server answers everyone in the group with a command) ----------

    suspend fun requestUnpause() = api().postUnit("SyncPlay/Unpause")
    suspend fun requestPause() = api().postUnit("SyncPlay/Pause")
    suspend fun requestSeek(positionTicks: Long) =
        api().postUnit("SyncPlay/Seek", body = buildJsonObject { put("PositionTicks", positionTicks) })

    suspend fun setQueue(itemIds: List<String>, index: Int, startTicks: Long) = api().postUnit(
        "SyncPlay/SetNewQueue",
        body = buildJsonObject {
            put("PlayingQueue", buildJsonArray { itemIds.forEach { add(JsonPrimitive(it)) } })
            put("PlayingItemPosition", index)
            put("StartPositionTicks", startTicks)
        },
    )

    /** Only acts when [playlistItemId] is still the group's current item, so several phones asking at once move on once. */
    suspend fun nextItem(playlistItemId: String) =
        api().postUnit("SyncPlay/NextItem", body = buildJsonObject { put("PlaylistItemId", playlistItemId) })

    suspend fun previousItem(playlistItemId: String) =
        api().postUnit("SyncPlay/PreviousItem", body = buildJsonObject { put("PlaylistItemId", playlistItemId) })

    /** This phone is loading or stalled ([buffering]) or ready again: the group waits for everyone to be ready. */
    suspend fun reportState(buffering: Boolean, positionTicks: Long, isPlaying: Boolean, playlistItemId: String) = api().postUnit(
        if (buffering) "SyncPlay/Buffering" else "SyncPlay/Ready",
        body = buildJsonObject {
            put("When", Instant.ofEpochMilli(serverNow()).toString())
            put("PositionTicks", positionTicks)
            put("IsPlaying", isPlaying)
            put("PlaylistItemId", playlistItemId)
        },
    )

    /** [ignore]: the group doesn't wait for this phone (its player is closed); it still gets every update. */
    suspend fun setIgnoreWait(ignore: Boolean) {
        if (_group.value == null) return
        runCatching { api().postUnit("SyncPlay/SetIgnoreWait", body = buildJsonObject { put("IgnoreWait", ignore) }) }
    }

    private fun api(): JellyfinClient = client ?: throw IllegalStateException(str(R.string.not_signed_in))

    // ---------- updates from the server ----------

    /** Called on the main thread for every WebSocket message. */
    fun handle(message: SocketMessage) {
        when (message.type) {
            "SyncPlayCommand" -> {
                val command = decode<SyncCommand>(message.data) ?: return
                if (sameId(command.groupId, _group.value?.groupId)) _commands.tryEmit(command)
            }
            "SyncPlayGroupUpdate" -> {
                val update = message.data as? JsonObject ?: return
                val type = (update["Type"] as? JsonPrimitive)?.takeIf { it.isString }?.content ?: return
                onGroupUpdate(type, update["Data"])
            }
        }
    }

    private fun onGroupUpdate(type: String, data: JsonElement?) {
        when (type) {
            "GroupJoined" -> {
                joined(decode<SyncGroup>(data) ?: return)
            }
            "UserJoined", "UserLeft" -> {
                val name = (data as? JsonPrimitive)?.takeIf { it.isString }?.content.orEmpty()
                _group.value?.let { g ->
                    _group.value = g.copy(participants = if (type == "UserJoined") (g.participants + name).distinct() else g.participants - name)
                }
                if (name.isNotBlank()) toast(str(if (type == "UserJoined") R.string.syncplay_x_joined else R.string.syncplay_x_left, name))
            }
            "StateUpdate" -> {
                val state = ((data as? JsonObject)?.get("State") as? JsonPrimitive)?.takeIf { it.isString }?.content ?: return
                _group.value?.let { _group.value = it.copy(state = state) }
            }
            "PlayQueue" -> {
                val q = decode<SyncQueue>(data) ?: return
                if (_group.value == null) return
                _queue.value = q
                openPlayerFor(q)
            }
            "GroupLeft", "NotInGroup", "GroupDoesNotExist" -> {
                val was = _group.value != null
                left()
                if (was && type == "GroupDoesNotExist") toast(str(R.string.syncplay_group_gone))
            }
            "LibraryAccessDenied" -> toast(str(R.string.syncplay_access_denied))
        }
    }

    /**
     * The group started something and no player is open here: open it, if JellyCtrl is on screen and unlocked.
     * Otherwise the group is told not to wait for this phone.
     */
    private fun openPlayerFor(q: SyncQueue) {
        if (q.current == null || PlayerBridge.active != null) return
        if (q.reason !in STARTS_AN_ITEM) return
        val locked = c.vault.enabled && !AppLock.unlocked
        if (c.foreground.value && !locked) {
            runCatching { PlayerActivity.startSync(c.context) }
        } else {
            c.appScope.launch { setIgnoreWait(true) }
        }
    }

    private fun left() {
        _group.value = null
        _queue.value = null
        clockJob?.cancel()
        clockJob = null
        bestRoundTripMs = Long.MAX_VALUE
    }

    /** Signed out, another account, or locked: the group belongs to the session that is gone. */
    fun reset() = left()

    /**
     * The connection came back while in a group. Joining the same group again makes the server send the group
     * and its queue once more, and makes the group wait until this phone is ready again: right when the
     * player here is following the group, and unwanted when it isn't (the others would be paused for a phone
     * nobody is watching), so then nothing is asked. "Open what the group is watching" catches up by itself:
     * the server corrects a phone that reports an old queue entry.
     */
    fun onReconnected() {
        val id = _group.value?.groupId ?: return
        if (PlayerBridge.active?.syncActive != true) return
        c.appScope.launch { runCatching { join(id) } }
    }

    /**
     * The server's list of groups, as just fetched: when the group this phone thinks it is in is gone, or this
     * account is no longer one of its members (the server drops a session that ended), forget it here too.
     */
    fun reconcile(groups: List<SyncGroup>) {
        val mine = _group.value ?: return
        if (android.os.SystemClock.elapsedRealtime() - joinedAt < 10_000) return
        val me = c.sessions.active.value?.account?.userName
        val listed = groups.firstOrNull { sameId(it.groupId, mine.groupId) }
        if (listed == null || (me != null && listed.participants.isNotEmpty() && listed.participants.none { it.equals(me, ignoreCase = true) })) {
            left()
        } else {
            _group.value = mine.copy(groupName = listed.groupName, state = listed.state, participants = listed.participants)
        }
    }

    // ---------- clock ----------

    /** Measures the server's clock a few times quickly, then once a minute, and tells the server the delay. */
    private fun startClock() {
        if (clockJob?.isActive == true) return
        clockJob = c.appScope.launch(Dispatchers.IO) {
            var round = 0
            while (isActive && _group.value != null) {
                try {
                    measureClock()
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Throwable) {
                }
                round++
                delay(if (round < 4) 1_500 else 60_000)
            }
        }
    }

    private suspend fun measureClock() {
        val api = client ?: return
        val sent = System.currentTimeMillis()
        val time = api.get<UtcTime>("GetUtcTime")
        val received = System.currentTimeMillis()
        val serverIn = isoToEpochMs(time.requestReceptionTime) ?: return
        val serverOut = isoToEpochMs(time.responseTransmissionTime) ?: return
        val roundTrip = SyncClock.roundTripMs(sent, serverIn, serverOut, received)
        // The measurement with the least delay on the way is the most exact one: keep that.
        if (roundTrip <= bestRoundTripMs) {
            bestRoundTripMs = roundTrip
            offsetMs = SyncClock.offsetMs(sent, serverIn, serverOut, received)
        }
        runCatching { api.postUnit("SyncPlay/Ping", body = buildJsonObject { put("Ping", roundTrip / 2) }) }
    }

    private inline fun <reified T> decode(data: JsonElement?): T? =
        data?.let { runCatching { JellyfinJson.decodeFromJsonElement<T>(it) }.getOrNull() }

    private fun toast(text: String) {
        if (c.foreground.value) Toast.makeText(c.context, text.take(200), Toast.LENGTH_SHORT).show()
    }

    private companion object {
        /** Queue updates after which a (new) item starts; the others only reorder or extend the list. */
        val STARTS_AN_ITEM = setOf("NewPlaylist", "SetCurrentItem", "NextItem", "PreviousItem")
    }
}

package com.vknxworks.jellyctrl.data.download

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.vknxworks.jellyctrl.container
import com.vknxworks.jellyctrl.data.api.ApiException
import com.vknxworks.jellyctrl.data.api.JellyfinClient
import com.vknxworks.jellyctrl.data.api.PlainJson
import com.vknxworks.jellyctrl.data.crypto.DataKeyStore
import com.vknxworks.jellyctrl.data.crypto.SealedStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.time.Instant

/** Where a download watched offline got to, waiting to be sent to the server. */
@Serializable
data class PendingProgress(
    val accountKey: String,
    val itemId: String,
    val positionTicks: Long,
    val runtimeTicks: Long? = null,
    /** When it was watched, ms since 1970. */
    val at: Long,
)

/** What to tell the server for a [PendingProgress]: a resume position, or "watched". */
data class UserDataUpdate(val positionTicks: Long, val played: Boolean)

/** Jellyfin's own resume rules, so offline viewing ends up the same as watching online. */
object ProgressRules {
    /** Jellyfin's defaults: under 5 % isn't a resume point, from 90 % on it counts as watched. */
    const val MIN_RESUME_PCT = 5
    const val MAX_RESUME_PCT = 90

    /** The update to send, or null when there's nothing worth sending (just started). */
    fun toUpdate(p: PendingProgress): UserDataUpdate? {
        val runtime = p.runtimeTicks?.takeIf { it > 0 } ?: return UserDataUpdate(p.positionTicks.coerceAtLeast(0), played = false)
        val pct = p.positionTicks * 100.0 / runtime
        return when {
            pct >= MAX_RESUME_PCT -> UserDataUpdate(0, played = true)
            pct < MIN_RESUME_PCT -> null
            else -> UserDataUpdate(p.positionTicks, played = false)
        }
    }

    /** Adds [new] to [list]: only the latest per account and item is kept. */
    fun merge(list: List<PendingProgress>, new: PendingProgress): List<PendingProgress> {
        val old = list.firstOrNull { it.accountKey == new.accountKey && it.itemId == new.itemId }
        if (old != null && old.at > new.at) return list
        return list.filterNot { it.accountKey == new.accountKey && it.itemId == new.itemId } + new
    }

    /** At most this many waiting updates are kept (the oldest go first). */
    const val MAX_PENDING = 500
}

/**
 * Playback of downloads while offline: positions and "watched" are kept on the phone and sent to
 * Jellyfin by [ProgressSyncWorker] as soon as there's a connection.
 */
class OfflineProgressStore(private val context: Context, private val scope: CoroutineScope, keys: DataKeyStore) {
    private val file = File(context.filesDir, "offline-progress.json")

    /** The file is encrypted at rest with the app's data key; one from before encryption is still read. */
    private val store = SealedStore(file, keys)
    private val _pending = MutableStateFlow<List<PendingProgress>>(emptyList())
    val pending: StateFlow<List<PendingProgress>> = _pending.asStateFlow()
    private val loaded = MutableStateFlow(false)

    private val _unreadable = MutableStateFlow(false)

    /** The saved file is encrypted and can't be opened: it's kept as it is and nothing is written over it. */
    val unreadable: StateFlow<Boolean> = _unreadable.asStateFlow()

    init {
        scope.launch(Dispatchers.IO) { load() }
    }

    private fun load() {
        val bytes = synchronized(file) { store.read() }
        val saved = bytes?.let {
            runCatching { PlainJson.decodeFromString(ListSerializer(PendingProgress.serializer()), String(it, Charsets.UTF_8)) }.getOrNull()
        }.orEmpty()
        _pending.update { current -> current.fold(saved) { acc, p -> ProgressRules.merge(acc, p) } }
        _unreadable.value = store.unreadable
        loaded.value = true
        if (_pending.value.isNotEmpty()) scheduleSync()
    }

    /** "Try again" after [unreadable]: reads the file once more. */
    suspend fun reload() = withContext(Dispatchers.IO) { load() }

    /** "Start empty" after [unreadable]: the file is renamed to `offline-progress.json.unreadable`, not deleted. */
    suspend fun setAside() = withContext(Dispatchers.IO) {
        synchronized(file) {
            if (store.setAside()) _unreadable.value = false
        }
    }

    fun record(accountKey: String, itemId: String, positionTicks: Long, runtimeTicks: Long?) {
        val p = PendingProgress(accountKey, itemId, positionTicks, runtimeTicks, System.currentTimeMillis())
        _pending.update { ProgressRules.merge(it, p).takeLast(ProgressRules.MAX_PENDING) }
        save()
        scheduleSync()
    }

    /** Sends everything waiting for [accountKey] with [client]. Returns false when something has to be tried again later. */
    suspend fun flush(accountKey: String, client: JellyfinClient): Boolean {
        loaded.filter { it }.first()
        var ok = true
        _pending.value.filter { it.accountKey == accountKey }.forEach { p ->
            val update = ProgressRules.toUpdate(p)
            val sent = if (update == null) true else try {
                client.postUnit(
                    "UserItems/${p.itemId}/UserData",
                    mapOf("userId" to client.requireUserId()),
                    buildJsonObject {
                        put("PlaybackPositionTicks", update.positionTicks)
                        if (update.played) put("Played", true)
                        put("LastPlayedDate", Instant.ofEpochMilli(p.at).toString())
                    },
                )
                true
            } catch (e: ApiException) {
                // The item is gone from the server, or this account can't change it: nothing more to do.
                e.code == 400 || e.code == 403 || e.code == 404
            } catch (e: Exception) {
                false
            }
            if (sent) _pending.update { list -> list.filterNot { it.accountKey == p.accountKey && it.itemId == p.itemId && it.at == p.at } }
            else ok = false
        }
        save()
        return ok
    }

    /** Accounts with something waiting. */
    fun accounts(): Set<String> = _pending.value.map { it.accountKey }.toSet()

    /** Signing out of [accountKey], or "Forgot PIN?" (null: everything). */
    fun forget(accountKey: String?) {
        _pending.update { list -> if (accountKey == null) emptyList() else list.filterNot { it.accountKey == accountKey } }
        // "Forgot PIN?" removes everything on purpose, also a file that couldn't be opened.
        save(wipe = accountKey == null)
    }

    private fun save(wipe: Boolean = false) {
        val snapshot = _pending.value
        scope.launch(Dispatchers.IO) {
            synchronized(file) {
                runCatching {
                    if (wipe) {
                        store.clear()
                        _unreadable.value = store.unreadable
                    }
                    // Encrypted when there's a data key; nothing is written while the file can't be opened.
                    store.write(PlainJson.encodeToString(ListSerializer(PendingProgress.serializer()), snapshot).toByteArray(Charsets.UTF_8))
                }
            }
        }
    }

    private fun scheduleSync() {
        val req = OneTimeWorkRequestBuilder<ProgressSyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .addTag(ProgressSyncWorker.TAG)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(ProgressSyncWorker.NAME, ExistingWorkPolicy.REPLACE, req)
    }
}

/** Sends the positions of downloads watched offline once the phone is online. */
class ProgressSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val c = applicationContext.container
        c.sessions.loaded.filter { it }.first()
        var retry = false
        for (key in c.offlineProgress.accounts()) {
            val account = c.sessions.accounts.value.firstOrNull { it.key == key }
            if (account == null) {
                c.offlineProgress.forget(key)
                continue
            }
            // The app lock keeps the sign-in sealed until you unlock: try again later.
            if (c.sessions.isSealedNow(account)) { retry = true; continue }
            val client = c.sessions.clientFor(account) ?: continue
            if (!c.offlineProgress.flush(key, client)) retry = true
        }
        return if (retry && runAttemptCount < 20) Result.retry() else Result.success()
    }

    companion object {
        const val NAME = "jellyctrl-progress-sync"
        const val TAG = "jellyctrl-progress-sync"
    }
}

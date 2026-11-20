package com.vknxworks.jellyctrl.data.download

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.vknxworks.jellyctrl.data.api.BaseItemDto
import com.vknxworks.jellyctrl.data.api.JellyfinJson
import com.vknxworks.jellyctrl.data.api.PlainJson
import com.vknxworks.jellyctrl.data.crypto.DataKeyStore
import com.vknxworks.jellyctrl.data.crypto.SealedStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import java.io.File
import com.vknxworks.jellyctrl.R
import com.vknxworks.jellyctrl.str

enum class DownloadStatus { QUEUED, RUNNING, DONE, FAILED, PAUSED }

@Serializable
data class DownloadEntry(
    val itemId: String,
    val accountKey: String,
    val name: String,
    val type: String?,
    val seriesName: String? = null,
    val itemJson: String,
    val fileName: String,
    val posterFile: String? = null,
    val totalBytes: Long = 0,
    val downloadedBytes: Long = 0,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val error: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
    /** Bits per second the server converted it to; 0 = the original file. */
    val bitrate: Long = 0,
    /** Subtitles saved next to it, for offline playback. */
    val subtitles: List<DownloadedSub> = emptyList(),
) {
    val converted: Boolean get() = bitrate > 0
    val item: BaseItemDto
        get() = runCatching { JellyfinJson.decodeFromString(BaseItemDto.serializer(), itemJson) }.getOrElse { BaseItemDto(id = itemId, name = name) }
    val progress: Float
        get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
}

/**
 * Offline copies of media, stored in app-private storage with a small JSON index. The index (titles and
 * details of what was downloaded) is encrypted at rest with the app's data key ([SealedStore]); the media
 * files, posters and subtitles next to it are ordinary files, because the players open them directly.
 */
class DownloadRepository(private val context: Context, private val scope: CoroutineScope, keys: DataKeyStore) {
    val dir: File = File(context.getExternalFilesDir(null) ?: context.filesDir, "downloads").apply { mkdirs() }
    private val store = SealedStore(File(dir, "index.json"), keys)
    private val mutex = Mutex()

    private val _entries = MutableStateFlow<List<DownloadEntry>>(emptyList())
    val entries: StateFlow<List<DownloadEntry>> = _entries.asStateFlow()

    private val _loaded = MutableStateFlow(false)

    /** The index was read from disk (or found unreadable): until then [entries] is empty because nothing is known yet. */
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    private val _unreadable = MutableStateFlow(false)

    /**
     * The index is encrypted and can't be opened (the key in the phone's secure storage is gone or changed, or
     * the file is damaged). It is left exactly as it is, nothing is written over it, and the app says so.
     */
    val unreadable: StateFlow<Boolean> = _unreadable.asStateFlow()

    init {
        scope.launch(Dispatchers.IO) { load() }
    }

    /** Reads the index (off the main thread: the first read asks the Keystore for the data key). */
    private suspend fun load() = mutex.withLock {
        val bytes = store.read()
        if (bytes != null) {
            val saved = runCatching {
                PlainJson.decodeFromString(ListSerializer(DownloadEntry.serializer()), String(bytes, Charsets.UTF_8))
            }.getOrDefault(emptyList())
            // Anything that was mid-download when the app died is re-queued by WorkManager; show it as queued.
            _entries.value = saved.map { if (it.status == DownloadStatus.RUNNING) it.copy(status = DownloadStatus.QUEUED) else it }
        }
        _unreadable.value = store.unreadable
        _loaded.value = true
    }

    /** Waits until the index was read. A worker can start the app cold and must not look before that. */
    suspend fun awaitLoaded() {
        _loaded.first { it }
    }

    /** "Try again" after [unreadable]: reads the index once more. */
    suspend fun reload() = withContext(Dispatchers.IO) { load() }

    /**
     * "Start empty" after [unreadable]: the index that can't be opened is renamed to `index.json.unreadable`
     * (kept, not deleted) and the list starts empty. The media files stay where they are.
     */
    suspend fun setAside() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (store.setAside()) {
                _unreadable.value = false
            }
        }
    }

    fun file(entry: DownloadEntry): File = safeFile(entry.fileName) ?: File(dir, "invalid")
    fun poster(entry: DownloadEntry): File? = entry.posterFile?.let { safeFile(it) }

    /**
     * Resolves a stored file name inside [dir], or null if it would escape it. Existing downloads were saved
     * as "<itemId>.<ext>" and keep working as long as that name stays inside the downloads folder.
     */
    fun safeFile(name: String): File? {
        if (name.isBlank() || name == "." || name == ".." || name.contains('/') || name.contains('\\') || name.contains('\u0000')) return null
        val f = File(dir, name)
        return f.takeIf { it.canonicalPath.startsWith(dir.canonicalPath + File.separator) }
    }

    fun find(itemId: String): DownloadEntry? = _entries.value.firstOrNull { it.itemId == itemId }

    fun completedFile(itemId: String): File? =
        find(itemId)?.takeIf { it.status == DownloadStatus.DONE }?.let { file(it) }?.takeIf { it.exists() }

    /** Saved subtitle files of [entry] that are still there, with their files. */
    fun subtitleFiles(entry: DownloadEntry): List<Pair<DownloadedSub, File>> =
        entry.subtitles.mapNotNull { s -> safeFile(s.fileName)?.takeIf { it.isFile }?.let { s to it } }

    /**
     * Queues [item]. [bitrate] (bits per second) asks the server to convert it; 0 downloads the
     * original file.
     */
    fun enqueue(item: BaseItemDto, accountKey: String, wifiOnly: Boolean, bitrate: Long = 0) {
        // The list can't be opened, so a new download couldn't be saved in it either.
        if (_unreadable.value) return
        val ext = if (bitrate > 0) DownloadPlan.convertedExtension(item) else (item.container ?: item.mediaSources.firstOrNull()?.container ?: "mkv").substringBefore(',')
            .trim().lowercase().takeIf { EXT_RE.matches(it) } ?: "mkv"
        // Local names come from a hash of the server's item id, never from the raw id, so a hostile server can't
        // point a download outside the folder. Re-downloads of an existing entry keep its stored names.
        val existing = find(item.id)
        val base = localBaseName(item.id)
        val entry = DownloadEntry(
            itemId = item.id,
            accountKey = accountKey,
            name = item.name ?: str(R.string.item),
            type = item.type,
            seriesName = item.seriesName,
            itemJson = JellyfinJson.encodeToString(BaseItemDto.serializer(), item),
            fileName = existing?.fileName?.takeIf { safeFile(it) != null && it.endsWith(".$ext") } ?: "$base.$ext",
            posterFile = existing?.posterFile?.takeIf { safeFile(it) != null } ?: "$base.jpg",
            bitrate = bitrate.coerceAtLeast(0),
        )
        // A finished download at another quality, or another file name, is replaced.
        if (existing != null && existing.fileName != entry.fileName) safeFile(existing.fileName)?.delete()
        scope.launch {
            upsert(entry)
            val request = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(workDataOf(DownloadWorker.KEY_ITEM_ID to item.id))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
                        .setRequiresStorageNotLow(true)
                        .build()
                )
                .addTag(DownloadWorker.TAG)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(workName(item.id), ExistingWorkPolicy.KEEP, request)
        }
    }

    fun retry(itemId: String, wifiOnly: Boolean) {
        val e = find(itemId) ?: return
        enqueue(e.item, e.accountKey, wifiOnly, e.bitrate)
    }

    fun cancel(itemId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(itemId))
        delete(itemId)
    }

    fun delete(itemId: String) {
        scope.launch {
            // The list can't be opened: it isn't known which files belong to what, so nothing is removed.
            if (_unreadable.value) return@launch
            val e = find(itemId) ?: return@launch
            WorkManager.getInstance(context).cancelUniqueWork(workName(itemId))
            safeFile(e.fileName)?.delete()
            deleteParts(e)
            e.subtitles.forEach { s -> safeFile(s.fileName)?.delete() }
            e.posterFile?.let { safeFile(it)?.delete() }
            mutex.withLock {
                _entries.update { list -> list.filterNot { it.itemId == itemId } }
                save()
            }
        }
    }

    suspend fun upsert(entry: DownloadEntry) = mutex.withLock {
        _entries.update { list -> list.filterNot { it.itemId == entry.itemId } + entry }
        save()
    }

    suspend fun modify(itemId: String, persist: Boolean = true, change: (DownloadEntry) -> DownloadEntry) = mutex.withLock {
        _entries.update { list -> list.map { if (it.itemId == itemId) change(it) else it } }
        if (persist) save()
    }

    /** "Forgot PIN?" in the app lock: every download, its files and anything still queued. */
    suspend fun deleteAll() {
        WorkManager.getInstance(context).cancelAllWorkByTag(DownloadWorker.TAG)
        mutex.withLock {
            _entries.value = emptyList()
            withContext(Dispatchers.IO) {
                dir.listFiles()?.forEach { it.deleteRecursively() }
                // Everything goes on purpose here, also an index that couldn't be opened.
                store.clear()
                _unreadable.value = store.unreadable
                save()
            }
        }
    }

    /** Something is queued or downloading. */
    fun busy(): Boolean = _entries.value.any { it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.RUNNING }

    fun usedBytes(): Long = dir.listFiles()?.sumOf { it.length() } ?: 0L

    /** Half-finished files of [e] (any quality). */
    fun deleteParts(e: DownloadEntry, keep: File? = null) {
        val prefix = e.fileName.substringBeforeLast('.') + "."
        dir.listFiles { f -> f.name.startsWith(prefix) && f.name.endsWith(".part") && f != keep }?.forEach { it.delete() }
    }

    /** Writes the index (encrypted when there's a data key). Never on the main thread; does nothing while it's unreadable. */
    private fun save() {
        runCatching {
            store.write(PlainJson.encodeToString(ListSerializer(DownloadEntry.serializer()), _entries.value).toByteArray(Charsets.UTF_8))
        }
    }

    companion object {
        private val EXT_RE = Regex("^[a-z0-9]{1,5}$")

        fun localBaseName(itemId: String): String =
            java.security.MessageDigest.getInstance("SHA-256").digest(itemId.toByteArray())
                .joinToString("") { "%02x".format(it) }.take(32)

        fun workName(itemId: String) = "download-$itemId"
    }
}

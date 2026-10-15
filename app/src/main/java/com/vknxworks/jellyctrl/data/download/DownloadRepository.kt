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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import java.io.File

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
) {
    val item: BaseItemDto
        get() = runCatching { JellyfinJson.decodeFromString(BaseItemDto.serializer(), itemJson) }.getOrElse { BaseItemDto(id = itemId, name = name) }
    val progress: Float
        get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
}

/** Offline copies of media, stored in app-private storage with a small JSON index. */
class DownloadRepository(private val context: Context, private val scope: CoroutineScope) {
    val dir: File = File(context.getExternalFilesDir(null) ?: context.filesDir, "downloads").apply { mkdirs() }
    private val indexFile = File(dir, "index.json")
    private val mutex = Mutex()

    private val _entries = MutableStateFlow<List<DownloadEntry>>(emptyList())
    val entries: StateFlow<List<DownloadEntry>> = _entries.asStateFlow()

    init {
        scope.launch(Dispatchers.IO) {
            val loaded = runCatching {
                PlainJson.decodeFromString(ListSerializer(DownloadEntry.serializer()), indexFile.readText())
            }.getOrDefault(emptyList())
            // Anything that was mid-download when the app died is re-queued by WorkManager; show it as queued.
            _entries.value = loaded.map { if (it.status == DownloadStatus.RUNNING) it.copy(status = DownloadStatus.QUEUED) else it }
        }
    }

    fun file(entry: DownloadEntry): File = File(dir, entry.fileName)
    fun poster(entry: DownloadEntry): File? = entry.posterFile?.let { File(dir, it) }

    fun find(itemId: String): DownloadEntry? = _entries.value.firstOrNull { it.itemId == itemId }

    fun completedFile(itemId: String): File? =
        find(itemId)?.takeIf { it.status == DownloadStatus.DONE }?.let { file(it) }?.takeIf { it.exists() }

    fun enqueue(item: BaseItemDto, accountKey: String, wifiOnly: Boolean) {
        val ext = (item.container ?: item.mediaSources.firstOrNull()?.container ?: "mkv").substringBefore(',')
        val entry = DownloadEntry(
            itemId = item.id,
            accountKey = accountKey,
            name = item.name ?: "Item",
            type = item.type,
            seriesName = item.seriesName,
            itemJson = JellyfinJson.encodeToString(BaseItemDto.serializer(), item),
            fileName = "${item.id}.$ext",
            posterFile = "${item.id}.jpg",
        )
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
        enqueue(e.item, e.accountKey, wifiOnly)
    }

    fun cancel(itemId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(itemId))
        delete(itemId)
    }

    fun delete(itemId: String) {
        scope.launch {
            val e = find(itemId) ?: return@launch
            WorkManager.getInstance(context).cancelUniqueWork(workName(itemId))
            File(dir, e.fileName).delete()
            File(dir, e.fileName + ".part").delete()
            e.posterFile?.let { File(dir, it).delete() }
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

    fun usedBytes(): Long = dir.listFiles()?.sumOf { it.length() } ?: 0L

    private fun save() {
        runCatching {
            val tmp = File(dir, "index.json.tmp")
            tmp.writeText(PlainJson.encodeToString(ListSerializer(DownloadEntry.serializer()), _entries.value))
            tmp.renameTo(indexFile)
        }
    }

    companion object {
        fun workName(itemId: String) = "download-$itemId"
    }
}

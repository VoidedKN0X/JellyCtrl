package com.vknxworks.jellyctrl.data.download

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.vknxworks.jellyctrl.JellyCtrlApp
import com.vknxworks.jellyctrl.R
import com.vknxworks.jellyctrl.container
import com.vknxworks.jellyctrl.data.api.await
import com.vknxworks.jellyctrl.data.api.downloadUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class DownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val c = context.container
    private val repo = c.downloads
    private val notificationId = (params.inputData.getString(KEY_ITEM_ID)?.hashCode() ?: 0) and 0x0FFFFFFF

    override suspend fun getForegroundInfo(): ForegroundInfo = foregroundInfo("Preparing download", 0f)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val itemId = inputData.getString(KEY_ITEM_ID) ?: return@withContext Result.failure()
        c.sessions.ready.filter { it }.first()
        val entry = repo.find(itemId) ?: return@withContext Result.failure()
        val account = c.sessions.accounts.value.firstOrNull { it.key == entry.accountKey }
            ?: return@withContext fail(itemId, "Account was signed out")
        val client = c.sessions.clientFor(account) ?: return@withContext fail(itemId, "Account token unavailable")

        runCatching { setForeground(foregroundInfo(entry.name, 0f)) }
        repo.modify(itemId) { it.copy(status = DownloadStatus.RUNNING, error = null) }

        // Poster, best effort.
        runCatching {
            val poster = repo.safeFile(entry.posterFile ?: "${DownloadRepository.localBaseName(itemId)}.jpg")
            if (poster != null && !poster.exists()) {
                val req = Request.Builder().url(client.imageUrl(itemId, maxWidth = 500)).header("Authorization", client.authHeader).build()
                client.http.newCall(req).await().use { r ->
                    if (r.isSuccessful) r.body?.byteStream()?.use { input -> poster.outputStream().use { input.copyTo(it) } }
                }
            }
        }

        val target = repo.safeFile(entry.fileName) ?: return@withContext fail(itemId, "Invalid file name")
        val part = repo.safeFile(entry.fileName + ".part") ?: return@withContext fail(itemId, "Invalid file name")
        try {
            val existing = if (part.exists()) part.length() else 0L
            val reqBuilder = Request.Builder()
                .url(client.downloadUrl(itemId))
                .header("Authorization", client.authHeader)
            if (existing > 0) reqBuilder.header("Range", "bytes=$existing-")
            client.http.newCall(reqBuilder.build()).await().use { resp ->
                if (!resp.isSuccessful) return@withContext fail(itemId, "Server returned ${resp.code}")
                val resumed = resp.code == 206
                val body = resp.body ?: return@withContext fail(itemId, "Empty response")
                val startAt = if (resumed) existing else 0L
                val total = body.contentLength().let { if (it > 0) it + startAt else 0L }
                repo.modify(itemId) { it.copy(totalBytes = total, downloadedBytes = startAt) }

                FileOutputStream(part, resumed).use { out ->
                    val input = body.byteStream()
                    val buf = ByteArray(256 * 1024)
                    var written = startAt
                    var lastUpdate = 0L
                    while (isActive) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        written += n
                        val now = System.currentTimeMillis()
                        if (now - lastUpdate > 1000) {
                            lastUpdate = now
                            val w = written
                            repo.modify(itemId, persist = false) { it.copy(downloadedBytes = w) }
                            val p = if (total > 0) w.toFloat() / total else 0f
                            runCatching { setForeground(foregroundInfo(entry.name, p)) }
                        }
                    }
                    if (!isActive) {
                        repo.modify(itemId) { it.copy(status = DownloadStatus.PAUSED, downloadedBytes = written) }
                        return@withContext Result.retry()
                    }
                }
            }
            target.delete()
            if (!part.renameTo(target)) return@withContext fail(itemId, "Could not save file")
            repo.modify(itemId) { it.copy(status = DownloadStatus.DONE, downloadedBytes = target.length(), totalBytes = target.length()) }
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                repo.modify(itemId) { it.copy(status = DownloadStatus.QUEUED, error = e.message) }
                Result.retry()
            } else {
                fail(itemId, e.message ?: "Download failed")
            }
        }
    }

    private suspend fun fail(itemId: String, msg: String): Result {
        repo.modify(itemId) { it.copy(status = DownloadStatus.FAILED, error = msg) }
        return Result.failure()
    }

    private fun foregroundInfo(title: String, progress: Float): ForegroundInfo {
        val n: Notification = NotificationCompat.Builder(applicationContext, JellyCtrlApp.CHANNEL_DOWNLOADS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Downloading $title")
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(100, (progress * 100).toInt(), progress <= 0f)
            .build()
        return ForegroundInfo(notificationId, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }

    companion object {
        const val KEY_ITEM_ID = "item_id"
        const val TAG = "jellyctrl-download"
    }
}

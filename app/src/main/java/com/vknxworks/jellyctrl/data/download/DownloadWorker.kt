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
import com.vknxworks.jellyctrl.data.api.BaseItemDto
import com.vknxworks.jellyctrl.data.api.JellyfinClient
import com.vknxworks.jellyctrl.data.api.stopEncoding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import com.vknxworks.jellyctrl.str

class DownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val c = context.container
    private val repo = c.downloads
    private val notificationId = (params.inputData.getString(KEY_ITEM_ID)?.hashCode() ?: 0) and 0x0FFFFFFF

    override suspend fun getForegroundInfo(): ForegroundInfo = foregroundInfo(str(R.string.preparing_download), 0f)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val itemId = inputData.getString(KEY_ITEM_ID) ?: return@withContext Result.failure()
        c.sessions.loaded.filter { it }.first()
        val entry = repo.find(itemId) ?: return@withContext Result.failure()
        val account = c.sessions.accounts.value.firstOrNull { it.key == entry.accountKey }
            ?: return@withContext fail(itemId, str(R.string.account_was_signed_out))
        // JellyCtrl restarted while the app lock is on: the token stays sealed until you unlock, so wait.
        if (c.sessions.isSealedNow(account)) return@withContext Result.retry()
        val client = c.sessions.clientFor(account) ?: return@withContext fail(itemId, str(R.string.account_token_unavailable))

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

        val target = repo.safeFile(entry.fileName) ?: return@withContext fail(itemId, str(R.string.invalid_file_name))
        val base = entry.fileName.substringBeforeLast('.')
        val part = repo.safeFile("$base.${DownloadPlan.partTag(entry.bitrate)}.part") ?: return@withContext fail(itemId, str(R.string.invalid_file_name))
        // Parts saved at another quality (or by an older version) can't be continued.
        repo.deleteParts(entry, keep = part)
        val item = entry.item
        val converted = entry.converted
        // A conversion is made on the fly, so it can't continue mid-file: it starts again.
        if (converted) part.delete()
        val playSessionId = java.util.UUID.randomUUID().toString().replace("-", "")

        val limit = c.settings.settings.value.downloadLimitGb * GIB
        val othersBytes = repo.entries.value.filter { it.itemId != itemId && it.status == DownloadStatus.DONE }.sumOf { it.totalBytes }
        val expected = DownloadPlan.expectedBytes(item, entry.bitrate)
        if (DownloadPlan.exceedsLimit(othersBytes, expected, limit)) {
            return@withContext fail(itemId, str(R.string.download_limit_reached, str(R.string.x_gb, c.settings.settings.value.downloadLimitGb)))
        }
        try {
            val existing = if (part.exists()) part.length() else 0L
            val reqBuilder = Request.Builder()
                .url(DownloadPlan.mediaUrl(client, item, entry.bitrate, playSessionId))
                .header("Authorization", client.authHeader)
            if (existing > 0) reqBuilder.header("Range", "bytes=$existing-")
            client.http.newCall(reqBuilder.build()).await().use { resp ->
                if (!resp.isSuccessful) return@withContext fail(itemId, str(R.string.server_returned_x, resp.code))
                val resumed = resp.code == 206 && existing > 0
                val body = resp.body ?: return@withContext fail(itemId, str(R.string.empty_response))
                val startAt = if (resumed) existing else 0L
                // Conversions have no known length: estimate it from the bitrate.
                val total = body.contentLength().let { if (it > 0) it + startAt else expected }
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
                        if (limit > 0 && othersBytes + written > limit) {
                            out.close()
                            part.delete()
                            return@withContext fail(itemId, str(R.string.download_limit_reached, str(R.string.x_gb, c.settings.settings.value.downloadLimitGb)))
                        }
                        val now = System.currentTimeMillis()
                        if (now - lastUpdate > 1000) {
                            lastUpdate = now
                            val w = written
                            repo.modify(itemId, persist = false) { it.copy(downloadedBytes = w, totalBytes = maxOf(it.totalBytes, w)) }
                            val p = if (total > 0) (w.toFloat() / total).coerceAtMost(0.99f) else 0f
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
            if (!part.renameTo(target)) return@withContext fail(itemId, str(R.string.could_not_save_file))
            val subs = if (DownloadPlan.isAudio(item)) emptyList() else saveSubtitles(client, item, base, converted)
            repo.modify(itemId) {
                it.copy(status = DownloadStatus.DONE, downloadedBytes = target.length(), totalBytes = target.length(), subtitles = subs, error = null)
            }
            Result.success()
        } catch (e: kotlin.coroutines.cancellation.CancellationException) {
            // Stopped by WorkManager (cancelled, or the network went away): not a failure. It shows as waiting,
            // so a stopped download doesn't keep counting as running (and keep the sign-in while locked).
            withContext(NonCancellable) { runCatching { repo.modify(itemId) { it.copy(status = DownloadStatus.QUEUED) } } }
            throw e
        } catch (e: Exception) {
            if (runAttemptCount < MAX_ATTEMPTS) {
                repo.modify(itemId) { it.copy(status = DownloadStatus.QUEUED, error = e.message) }
                Result.retry()
            } else {
                fail(itemId, e.message ?: str(R.string.download_failed))
            }
        } finally {
            // The server stops converting as soon as the download ends (or stops).
            if (converted) withContext(NonCancellable) { client.stopEncoding(playSessionId) }
        }
    }

    /**
     * Saves the subtitles that go with a video (best effort: one that fails is skipped). Only text
     * subtitles; local names come from the download's hashed name.
     */
    private suspend fun saveSubtitles(client: JellyfinClient, item: BaseItemDto, base: String, converted: Boolean): List<DownloadedSub> {
        val source = item.mediaSources.firstOrNull() ?: return emptyList()
        val sourceId = source.id ?: item.id
        return DownloadPlan.subtitlesToFetch(source, converted).mapNotNull { s ->
            val format = DownloadPlan.subtitleFormat(s.codec) ?: return@mapNotNull null
            val name = DownloadPlan.subtitleFileName(base, s.index, format)
            val file = repo.safeFile(name) ?: return@mapNotNull null
            runCatching {
                val req = Request.Builder().url(DownloadPlan.subtitleUrl(client, item.id, sourceId, s.index, format))
                    .header("Authorization", client.authHeader).build()
                client.http.newCall(req).await().use { r ->
                    if (!r.isSuccessful) return@mapNotNull null
                    val body = r.body ?: return@mapNotNull null
                    if (body.contentLength() > DownloadPlan.MAX_SUBTITLE_BYTES) return@mapNotNull null
                    val src = body.source()
                    src.request(DownloadPlan.MAX_SUBTITLE_BYTES + 1)
                    if (src.buffer.size > DownloadPlan.MAX_SUBTITLE_BYTES) return@mapNotNull null
                    file.outputStream().use { out -> src.buffer.copyTo(out) }
                }
                DownloadedSub(
                    index = s.index,
                    codec = format,
                    language = s.language,
                    title = s.displayTitle ?: s.title,
                    fileName = name,
                    isDefault = s.isDefault,
                    isForced = s.isForced,
                )
            }.getOrNull()
        }
    }
    private suspend fun fail(itemId: String, msg: String): Result {
        repo.modify(itemId) { it.copy(status = DownloadStatus.FAILED, error = msg) }
        return Result.failure()
    }

    private fun foregroundInfo(title: String, progress: Float): ForegroundInfo {
        val n: Notification = NotificationCompat.Builder(applicationContext, JellyCtrlApp.CHANNEL_DOWNLOADS)
            .setSmallIcon(R.drawable.ic_notification)
            // Signal accent: lavender in dark mode, the darker violet in light mode (see values-night).
            .setColor(applicationContext.getColor(R.color.notification_accent))
            .setContentTitle(str(R.string.downloading_x, title))
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(100, (progress * 100).toInt(), progress <= 0f)
            // What's being downloaded stays off the lock screen.
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(
                NotificationCompat.Builder(applicationContext, JellyCtrlApp.CHANNEL_DOWNLOADS)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(str(R.string.downloading))
                    .setOnlyAlertOnce(true)
                    .setProgress(100, (progress * 100).toInt(), progress <= 0f)
                    .build(),
            )
            .build()
        return ForegroundInfo(notificationId, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }

    companion object {
        const val KEY_ITEM_ID = "item_id"
        const val TAG = "jellyctrl-download"
        private const val MAX_ATTEMPTS = 8
        private const val GIB = 1024L * 1024 * 1024
    }
}

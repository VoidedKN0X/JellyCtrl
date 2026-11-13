package com.vknxworks.jellyctrl.data.download

import com.vknxworks.jellyctrl.data.api.BaseItemDto
import com.vknxworks.jellyctrl.data.api.JellyfinClient
import com.vknxworks.jellyctrl.data.api.MediaSourceInfo
import com.vknxworks.jellyctrl.data.api.MediaStream
import com.vknxworks.jellyctrl.data.api.downloadUrl
import kotlinx.serialization.Serializable

/** A subtitle saved next to a download (a separate file on the server, or every text subtitle of a converted video). */
@Serializable
data class DownloadedSub(
    /** The stream's index on the server, so playback can pick the same subtitle as when streaming. */
    val index: Int,
    val codec: String,
    val language: String? = null,
    val title: String? = null,
    val fileName: String,
    val isDefault: Boolean = false,
    val isForced: Boolean = false,
)

/**
 * The rules behind downloads, free of Android so they can be unit-tested: what to fetch, from which
 * address, which subtitles go with it, which episodes "next N unwatched" means, and the space limit.
 */
object DownloadPlan {
    /** Audio bitrate inside converted videos. */
    const val VIDEO_AUDIO_BPS = 192_000L
    /** Biggest subtitle file kept (real ones are well under 1 MB). */
    const val MAX_SUBTITLE_BYTES = 5L * 1024 * 1024

    fun isAudio(item: BaseItemDto): Boolean = item.mediaType == "Audio" || item.type == "Audio"

    /** Bitrate to convert [item] to, in bits per second; 0 = download the original file. */
    fun bitrateFor(item: BaseItemDto, videoMbps: Int, musicKbps: Int): Long =
        if (isAudio(item)) musicKbps.coerceAtLeast(0) * 1000L else videoMbps.coerceAtLeast(0) * 1_000_000L

    /** File extension of a converted download. */
    fun convertedExtension(item: BaseItemDto): String = if (isAudio(item)) "mp3" else "mp4"

    /** Tag in the half-finished file's name, so a part saved at another quality is never continued. */
    fun partTag(bitrate: Long): String = if (bitrate <= 0) "orig" else "b$bitrate"

    /**
     * Where to download [item] from: the original file, or a conversion by the server (H.264 and AAC
     * in MP4 for video, MP3 for music) at [bitrate]. The token goes in a header, never in the address.
     */
    fun mediaUrl(client: JellyfinClient, item: BaseItemDto, bitrate: Long, playSessionId: String): String {
        if (bitrate <= 0) return client.downloadUrl(item.id)
        val source = item.mediaSources.firstOrNull()
        return if (isAudio(item)) {
            client.url(
                "Audio/${item.id}/stream.mp3",
                mapOf(
                    "static" to false,
                    "container" to "mp3",
                    "audioCodec" to "mp3",
                    "audioBitRate" to bitrate,
                    "maxAudioChannels" to 2,
                    "mediaSourceId" to source?.id,
                    "deviceId" to client.deviceId,
                    "playSessionId" to playSessionId,
                ),
            ).toString()
        } else {
            val videoBps = (bitrate - VIDEO_AUDIO_BPS).coerceAtLeast(bitrate / 2)
            client.url(
                "Videos/${item.id}/stream.mp4",
                mapOf(
                    "static" to false,
                    "container" to "mp4",
                    "videoCodec" to "h264",
                    "audioCodec" to "aac",
                    "videoBitRate" to videoBps,
                    "audioBitRate" to VIDEO_AUDIO_BPS,
                    "maxAudioChannels" to 2,
                    "mediaSourceId" to source?.id,
                    "audioStreamIndex" to source?.defaultAudioStreamIndex,
                    "deviceId" to client.deviceId,
                    "playSessionId" to playSessionId,
                    // Subtitles are saved as separate files; nothing is burned into the picture.
                    "subtitleMethod" to "Drop",
                ),
            ).toString()
        }
    }

    /** The format to ask the server for a text subtitle in, or null for picture subtitles (PGS, VobSub) that can't be saved as text. */
    fun subtitleFormat(codec: String?): String? = when (codec?.lowercase()) {
        "ass", "ssa" -> "ass"
        "vtt", "webvtt" -> "vtt"
        "srt", "subrip", "mov_text", "tx3g", "text", "ttml", "smi", "sami", "microdvd" -> "srt"
        // Picture subtitles (pgs, dvdsub, vobsub, dvbsub …) and anything unknown.
        else -> null
    }

    /**
     * Subtitles to save with a video: the ones in separate files on the server (an original download
     * already carries its embedded ones), or every text subtitle when the video is converted
     * (conversion leaves them out).
     */
    fun subtitlesToFetch(source: MediaSourceInfo?, converted: Boolean): List<MediaStream> =
        source?.mediaStreams.orEmpty()
            .filter { it.type == "Subtitle" && subtitleFormat(it.codec) != null && (converted || it.isExternal) }
            .distinctBy { it.index }
            .take(40)

    fun subtitleUrl(client: JellyfinClient, itemId: String, sourceId: String, index: Int, format: String): String =
        client.url("Videos/$itemId/$sourceId/Subtitles/$index/0/Stream.$format").toString()

    /** Local file name of a saved subtitle: from the download's own (hashed) name, never from server text. */
    fun subtitleFileName(base: String, index: Int, format: String): String = "$base.sub$index.$format"

    /** About how big the download will be: the file size for originals, bitrate × length for conversions; 0 when unknown. */
    fun expectedBytes(item: BaseItemDto, bitrate: Long): Long {
        val runtimeSec = (item.runTimeTicks ?: item.mediaSources.firstOrNull()?.runTimeTicks ?: 0L) / 10_000_000L
        return if (bitrate <= 0) item.mediaSources.firstOrNull()?.size ?: 0L
        else bitrate / 8 * runtimeSec
    }

    /** True when [expected] more bytes don't fit next to [used] under [limit] (0 = no limit). */
    fun exceedsLimit(used: Long, expected: Long, limit: Long): Boolean = limit > 0 && used + expected > limit

    /**
     * "Next N unwatched": the first [n] unplayed episodes after the last one you watched, in
     * season and episode order. Specials (season 0) are left out. If you've watched nothing, it
     * starts at the first episode.
     */
    fun nextUnwatched(episodes: List<BaseItemDto>, n: Int): List<BaseItemDto> {
        if (n <= 0) return emptyList()
        val ordered = episodes
            .filter { it.type == "Episode" && (it.parentIndexNumber ?: 1) != 0 && it.locationType != "Virtual" }
            .sortedWith(compareBy({ it.parentIndexNumber ?: Int.MAX_VALUE }, { it.indexNumber ?: Int.MAX_VALUE }))
        val lastWatched = ordered.indexOfLast { it.userData?.played == true }
        return ordered.drop(lastWatched + 1).filter { it.userData?.played != true }.take(n)
    }
}

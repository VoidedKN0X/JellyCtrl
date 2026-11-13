package com.vknxworks.jellyctrl.data.api

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class PlaybackRequest(
    val itemId: String,
    val deviceProfile: JsonObject,
    val maxBitrate: Long,
    val startTicks: Long = 0,
    val mediaSourceId: String? = null,
    val audioStreamIndex: Int? = null,
    val subtitleStreamIndex: Int? = null,
    val enableDirectPlay: Boolean = true,
    val enableDirectStream: Boolean = true,
    val enableTranscoding: Boolean = true,
    val liveStreamId: String? = null,
)

suspend fun JellyfinClient.playbackInfo(req: PlaybackRequest): PlaybackInfoResponse =
    post(
        "Items/${req.itemId}/PlaybackInfo",
        mapOf("userId" to requireUserId()),
        buildJsonObject {
            put("UserId", requireUserId())
            put("DeviceProfile", req.deviceProfile)
            put("MaxStreamingBitrate", req.maxBitrate)
            put("StartTimeTicks", req.startTicks)
            req.mediaSourceId?.let { put("MediaSourceId", it) }
            req.audioStreamIndex?.let { put("AudioStreamIndex", it) }
            req.subtitleStreamIndex?.let { put("SubtitleStreamIndex", it) }
            req.liveStreamId?.let { put("LiveStreamId", it) }
            put("EnableDirectPlay", req.enableDirectPlay)
            put("EnableDirectStream", req.enableDirectStream)
            put("EnableTranscoding", req.enableTranscoding)
            put("AllowVideoStreamCopy", true)
            put("AllowAudioStreamCopy", true)
            put("AutoOpenLiveStream", true)
        },
    )

suspend fun JellyfinClient.mediaSegments(itemId: String): List<MediaSegment> =
    runCatching { get<MediaSegmentsResult>("MediaSegments/$itemId").items }.getOrDefault(emptyList())

data class ProgressReport(
    val itemId: String,
    val mediaSourceId: String?,
    val playSessionId: String?,
    val positionTicks: Long,
    val isPaused: Boolean,
    val playMethod: String,
    val audioStreamIndex: Int?,
    val subtitleStreamIndex: Int?,
    val liveStreamId: String? = null,
    val playbackRate: Float = 1f,
)

private fun ProgressReport.toJson(failed: Boolean = false) = buildJsonObject {
    put("ItemId", itemId)
    mediaSourceId?.let { put("MediaSourceId", it) }
    playSessionId?.let { put("PlaySessionId", it) }
    liveStreamId?.let { put("LiveStreamId", it) }
    put("PositionTicks", positionTicks)
    put("IsPaused", isPaused)
    put("IsMuted", false)
    put("CanSeek", true)
    put("PlayMethod", playMethod)
    put("PlaybackRate", playbackRate.toDouble())
    audioStreamIndex?.let { put("AudioStreamIndex", it) }
    subtitleStreamIndex?.let { put("SubtitleStreamIndex", it) }
    if (failed) put("Failed", true)
}

suspend fun JellyfinClient.reportStart(r: ProgressReport) = postUnit("Sessions/Playing", body = r.toJson())
suspend fun JellyfinClient.reportProgress(r: ProgressReport) = postUnit("Sessions/Playing/Progress", body = r.toJson())
suspend fun JellyfinClient.reportStopped(r: ProgressReport) = postUnit("Sessions/Playing/Stopped", body = r.toJson())

suspend fun JellyfinClient.stopEncoding(playSessionId: String) =
    runCatching { deleteUnit("Videos/ActiveEncodings", mapOf("deviceId" to deviceId, "playSessionId" to playSessionId)) }

suspend fun JellyfinClient.closeLiveStream(liveStreamId: String) =
    runCatching { postUnit("LiveStreams/Close", mapOf("liveStreamId" to liveStreamId)) }

/** Tell the server what this client supports, so it shows up as controllable and gets remote commands. */
suspend fun JellyfinClient.postCapabilities() = runCatching {
    postUnit(
        "Sessions/Capabilities",
        mapOf(
            "playableMediaTypes" to listOf("Video", "Audio"),
            "supportedCommands" to listOf("DisplayMessage", "SetAudioStreamIndex", "SetSubtitleStreamIndex"),
            "supportsMediaControl" to true,
        ),
    )
}

fun JellyfinClient.directStreamUrl(
    itemId: String,
    source: MediaSourceInfo,
    playSessionId: String?,
    audio: Boolean = false,
): String {
    val path = if (audio) "Audio/$itemId/stream" else "Videos/$itemId/stream"
    return url(
        path,
        mapOf(
            "static" to true,
            "mediaSourceId" to source.id,
            "deviceId" to deviceId,
            "playSessionId" to playSessionId,
            "tag" to source.eTag,
            "liveStreamId" to source.liveStreamId,
        ),
    ).toString()
}

/** Audio stream for the music player: the server direct-plays or transcodes to what the device supports. */
fun JellyfinClient.universalAudioUrl(itemId: String, maxBitrate: Long): String = url(
    "Audio/$itemId/universal",
    mapOf(
        "userId" to userId,
        "deviceId" to deviceId,
        "maxStreamingBitrate" to maxBitrate,
        "container" to "opus,webm|opus,mp3,aac,m4a|aac,m4b|aac,flac,webma,webm|webma,wav,ogg",
        "transcodingContainer" to "mp3",
        "transcodingProtocol" to "http",
        "audioCodec" to "mp3",
        "enableRedirection" to true,
        "enableRemoteMedia" to false,
    ),
).toString()

// ---------- remote control of other sessions ----------

suspend fun JellyfinClient.sessions(controllableOnly: Boolean, activeWithinSeconds: Int? = 960): List<SessionInfo> =
    get(
        "Sessions",
        mapOf(
            "controllableByUserId" to if (controllableOnly) userId else null,
            "activeWithinSeconds" to activeWithinSeconds,
        ),
    )

suspend fun JellyfinClient.sessionPlaystate(sessionId: String, command: String, seekTicks: Long? = null) =
    postUnit("Sessions/$sessionId/Playing/$command", mapOf("seekPositionTicks" to seekTicks))

suspend fun JellyfinClient.sessionPlay(sessionId: String, itemIds: List<String>, startTicks: Long? = null) =
    postUnit(
        "Sessions/$sessionId/Playing",
        mapOf("playCommand" to "PlayNow", "itemIds" to itemIds, "startPositionTicks" to startTicks),
    )

suspend fun JellyfinClient.sessionMessage(sessionId: String, header: String, text: String) =
    postUnit(
        "Sessions/$sessionId/Message",
        body = buildJsonObject {
            put("Header", header)
            put("Text", text)
            put("TimeoutMs", 8000)
        },
    )

suspend fun JellyfinClient.sessionGeneralCommand(sessionId: String, command: String) =
    postUnit("Sessions/$sessionId/Command/$command")

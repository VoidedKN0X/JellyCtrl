package com.vknxworks.jellyctrl.player

import com.vknxworks.jellyctrl.data.api.BaseItemDto
import com.vknxworks.jellyctrl.data.api.JellyfinClient
import com.vknxworks.jellyctrl.data.api.MediaSourceInfo
import com.vknxworks.jellyctrl.data.api.MediaStream
import com.vknxworks.jellyctrl.data.api.PlaybackRequest
import com.vknxworks.jellyctrl.data.api.directStreamUrl
import com.vknxworks.jellyctrl.data.api.msToTicks
import com.vknxworks.jellyctrl.data.api.playbackInfo
import com.vknxworks.jellyctrl.data.settings.PlayerEngineChoice

enum class EngineKind { MEDIA3, MPV }

data class ResolvedPlayback(
    val engine: EngineKind,
    val url: String,
    val isHls: Boolean,
    /** DirectPlay, DirectStream or Transcode - reported back to the server. */
    val playMethod: String,
    val source: MediaSourceInfo,
    val playSessionId: String?,
    val audioIndex: Int?,
    val subtitleIndex: Int?,
    val externalSubs: List<ExternalSub>,
    /** Track choices expressed in engine terms. */
    val audioOrdinal: Int?,
    val subtitle: SubSelection,
    val offline: Boolean = false,
) {
    val audioStreams: List<MediaStream> get() = source.mediaStreams.filter { it.type == "Audio" }
    val subtitleStreams: List<MediaStream> get() = source.mediaStreams.filter { it.type == "Subtitle" }
    val isTranscoding: Boolean get() = playMethod == "Transcode"
}

/**
 * Works out how to play an item: asks the server for playback info with a device profile,
 * then picks Media3 or mpv and the stream URL.
 */
object PlaybackResolver {

    suspend fun resolve(
        client: JellyfinClient,
        itemId: String,
        choice: PlayerEngineChoice,
        maxBitrate: Long,
        startMs: Long,
        mediaSourceId: String?,
        audioIndex: Int?,
        /** null = server default, -1 = off */
        subtitleIndex: Int?,
        forceTranscode: Boolean = false,
    ): ResolvedPlayback {
        val profile = if (choice == PlayerEngineChoice.MEDIA3) DeviceProfiles.media3(maxBitrate) else DeviceProfiles.universal(maxBitrate)
        val info = client.playbackInfo(
            PlaybackRequest(
                itemId = itemId,
                deviceProfile = profile,
                maxBitrate = maxBitrate,
                startTicks = startMs.msToTicks(),
                mediaSourceId = mediaSourceId,
                audioStreamIndex = audioIndex,
                subtitleStreamIndex = subtitleIndex,
                enableDirectPlay = !forceTranscode,
                enableDirectStream = !forceTranscode,
            )
        )
        if (info.errorCode != null) throw IllegalStateException(playbackError(info.errorCode))
        val source = info.mediaSources.firstOrNull { mediaSourceId == null || it.id == mediaSourceId }
            ?: info.mediaSources.firstOrNull()
            ?: throw IllegalStateException("The server returned no playable media")

        val audio = audioIndex ?: source.defaultAudioStreamIndex
        val sub = when (subtitleIndex) {
            null -> source.defaultSubtitleStreamIndex?.takeIf { it >= 0 }
            -1 -> null
            else -> subtitleIndex
        }

        val direct = (source.supportsDirectPlay || source.supportsDirectStream) && !forceTranscode
        val transcodeUrl = source.transcodingUrl

        val (engine, url, hls, method) = when {
            direct -> {
                val engine = when (choice) {
                    PlayerEngineChoice.MPV -> EngineKind.MPV
                    PlayerEngineChoice.MEDIA3 -> EngineKind.MEDIA3
                    PlayerEngineChoice.AUTO -> if (CodecSupport.media3CanPlay(source, audio, sub)) EngineKind.MEDIA3 else EngineKind.MPV
                }
                val u = if (source.isRemote && source.path?.startsWith("http") == true && source.protocol == "Http") {
                    source.path.orEmpty()
                } else client.directStreamUrl(itemId, source, info.playSessionId)
                Quad(engine, u, false, if (source.supportsDirectPlay) "DirectPlay" else "DirectStream")
            }
            transcodeUrl != null -> Quad(
                if (choice == PlayerEngineChoice.MPV) EngineKind.MPV else EngineKind.MEDIA3,
                client.absolute(transcodeUrl),
                source.transcodingSubProtocol.equals("hls", true) || transcodeUrl.contains(".m3u8"),
                "Transcode",
            )
            else -> throw IllegalStateException("This file can't be played or transcoded for this device")
        }

        // Track choices in engine terms. Transcodes already have the chosen tracks baked in by the server.
        val embeddedAudio = source.mediaStreams.filter { it.type == "Audio" && !it.isExternal }
        val embeddedSubs = source.mediaStreams.filter { it.type == "Subtitle" && !it.isExternal }
        val externals = source.mediaStreams
            .filter { it.type == "Subtitle" && it.deliveryMethod == "External" && it.deliveryUrl != null }
            .map { ExternalSub(it.index, client.absolute(it.deliveryUrl!!), it.codec, it.language, it.displayTitle ?: it.title) }

        val audioOrdinal = if (method == "Transcode") null else embeddedAudio.indexOfFirst { it.index == audio }.takeIf { it >= 0 }
        val subSel: SubSelection = when {
            sub == null -> SubSelection.Off
            externals.any { it.jellyfinIndex == sub } -> SubSelection.External(sub)
            method == "Transcode" -> SubSelection.Off // burned in or not delivered
            else -> embeddedSubs.indexOfFirst { it.index == sub }.takeIf { it >= 0 }?.let { SubSelection.Embedded(it) } ?: SubSelection.Off
        }
        // For transcodes, only external text subtitles are side-loaded; embedded ones arrive in the stream.
        val sideLoad = if (method == "Transcode") externals.filter { it.jellyfinIndex == sub } else externals

        return ResolvedPlayback(
            engine = engine,
            url = url,
            isHls = hls,
            playMethod = method,
            source = source,
            playSessionId = info.playSessionId,
            audioIndex = audio,
            subtitleIndex = sub,
            externalSubs = sideLoad,
            audioOrdinal = audioOrdinal,
            subtitle = subSel,
        )
    }

    /** Play a downloaded file. Engine choice uses the codecs recorded when it was downloaded. */
    fun offline(item: BaseItemDto, filePath: String, choice: PlayerEngineChoice): ResolvedPlayback {
        val source = item.mediaSources.firstOrNull() ?: MediaSourceInfo(id = item.id, mediaStreams = item.mediaStreams)
        val audio = source.defaultAudioStreamIndex
        val sub = source.defaultSubtitleStreamIndex?.takeIf { it >= 0 }
        val engine = when (choice) {
            PlayerEngineChoice.MPV -> EngineKind.MPV
            PlayerEngineChoice.MEDIA3 -> EngineKind.MEDIA3
            PlayerEngineChoice.AUTO -> if (CodecSupport.media3CanPlay(source, audio, sub)) EngineKind.MEDIA3 else EngineKind.MPV
        }
        val embeddedAudio = source.mediaStreams.filter { it.type == "Audio" && !it.isExternal }
        val embeddedSubs = source.mediaStreams.filter { it.type == "Subtitle" && !it.isExternal }
        return ResolvedPlayback(
            engine = engine,
            url = if (engine == EngineKind.MPV) filePath else "file://$filePath",
            isHls = false,
            playMethod = "DirectPlay",
            source = source.copy(mediaStreams = source.mediaStreams.filter { !it.isExternal }),
            playSessionId = null,
            audioIndex = audio,
            subtitleIndex = sub,
            externalSubs = emptyList(),
            audioOrdinal = embeddedAudio.indexOfFirst { it.index == audio }.takeIf { it >= 0 },
            subtitle = embeddedSubs.indexOfFirst { it.index == sub }.takeIf { it >= 0 }?.let { SubSelection.Embedded(it) } ?: SubSelection.Off,
            offline = true,
        )
    }

    private fun playbackError(code: String) = when (code) {
        "NotAllowed" -> "This account isn't allowed to play this"
        "NoCompatibleStream" -> "No compatible stream - try another player engine in settings"
        "RateLimitExceeded" -> "Too many streams on this server right now"
        else -> "Playback error: $code"
    }

    private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
}

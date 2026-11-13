package com.vknxworks.jellyctrl.player

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import com.vknxworks.jellyctrl.data.api.MediaSourceInfo
import com.vknxworks.jellyctrl.data.api.MediaStream
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** What this phone's hardware/software decoders can handle, for deciding between Media3 and mpv. */
object CodecSupport {
    private val decoders: List<MediaCodecInfo> by lazy {
        MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.filter { !it.isEncoder }
    }

    private fun hasDecoder(mime: String): Boolean =
        decoders.any { info -> info.supportedTypes.any { it.equals(mime, ignoreCase = true) } }

    private fun hasProfile(mime: String, profile: Int): Boolean = decoders.any { info ->
        info.supportedTypes.any { it.equals(mime, true) } &&
            runCatching { info.getCapabilitiesForType(mime).profileLevels.any { it.profile == profile } }.getOrDefault(false)
    }

    val hevc by lazy { hasDecoder("video/hevc") }
    val hevc10 by lazy { hasProfile("video/hevc", MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10) }
    val av1 by lazy { hasDecoder("video/av01") }
    val vp9 by lazy { hasDecoder("video/x-vnd.on2.vp9") }
    val vp8 by lazy { hasDecoder("video/x-vnd.on2.vp8") }
    val mpeg2 by lazy { hasDecoder("video/mpeg2") }
    val mpeg4 by lazy { hasDecoder("video/mp4v-es") }
    val dolbyVision by lazy { hasDecoder("video/dolby-vision") }
    val ac3 by lazy { hasDecoder("audio/ac3") }
    val eac3 by lazy { hasDecoder("audio/eac3") }
    val dts by lazy { hasDecoder("audio/vnd.dts") }
    val truehd by lazy { hasDecoder("audio/true-hd") }

    fun media3VideoCodecs(): List<String> = buildList {
        add("h264")
        if (hevc) add("hevc")
        if (vp9) add("vp9")
        if (vp8) add("vp8")
        if (av1) add("av1")
        if (mpeg2) add("mpeg2video")
        if (mpeg4) add("mpeg4")
    }

    fun media3AudioCodecs(): List<String> = buildList {
        addAll(listOf("aac", "mp3", "mp2", "opus", "vorbis", "flac", "alac", "pcm_s16le", "pcm_s24le", "pcm_s16be", "pcm_u8", "pcm_mulaw", "pcm_alaw"))
        if (ac3) add("ac3")
        if (eac3) add("eac3")
        if (dts) { add("dts"); add("dca") }
        if (truehd) add("truehd")
    }

    private val media3Containers = setOf("mp4", "m4v", "mov", "mkv", "webm", "matroska", "ts", "mpegts", "m2ts", "mts", "flv", "avi", "mpeg", "mpg", "ogg", "mp3", "aac", "flac", "wav", "m4a", "3gp", "hls")

    /** True when Media3 can direct play this source with the chosen tracks, so mpv isn't needed. */
    fun media3CanPlay(source: MediaSourceInfo, audioIndex: Int?, subtitleIndex: Int?): Boolean {
        val containers = source.container?.lowercase()?.split(',').orEmpty()
        if (containers.isNotEmpty() && containers.none { it in media3Containers }) return false
        val video = source.mediaStreams.firstOrNull { it.type == "Video" }
        if (video != null && !videoOk(video)) return false
        val audio = source.mediaStreams.firstOrNull { it.type == "Audio" && it.index == (audioIndex ?: source.defaultAudioStreamIndex) }
            ?: source.mediaStreams.firstOrNull { it.type == "Audio" }
        if (audio != null && audio.codec?.lowercase() !in media3AudioCodecs()) return false
        val sub = subtitleIndex?.let { idx -> source.mediaStreams.firstOrNull { it.type == "Subtitle" && it.index == idx } }
        if (sub != null && sub.codec?.lowercase() in setOf("ass", "ssa") && !sub.isExternal) return false
        return true
    }

    private fun videoOk(v: MediaStream): Boolean {
        val codec = v.codec?.lowercase() ?: return true
        if (codec !in media3VideoCodecs() && codec != "h265") return false
        if ((codec == "hevc" || codec == "h265") && (v.bitDepth ?: 8) > 8 && !hevc10) return false
        val range = v.videoRangeType?.uppercase().orEmpty()
        // Pure Dolby Vision profile 5 has no fallback layer; it needs a DV decoder.
        if (range == "DOVI" && !dolbyVision) return false
        return true
    }
}

/** Builds Jellyfin DeviceProfile JSON. */
object DeviceProfiles {

    private val TEXT_SUBS = listOf("srt", "subrip", "vtt", "webvtt", "ass", "ssa", "ttml", "sub", "smi", "mov_text")
    private val IMAGE_SUBS = listOf("pgssub", "pgs", "dvdsub", "dvbsub", "vobsub", "sup")

    /**
     * Profile that claims every format is direct-playable (mpv can play them), so the server only
     * transcodes when the bitrate limit requires it.
     */
    fun universal(maxBitrate: Long): JsonObject = buildJsonObject {
        put("Name", "JellyCtrl (mpv)")
        put("MaxStreamingBitrate", maxBitrate)
        put("MaxStaticBitrate", maxBitrate)
        put("MusicStreamingTranscodingBitrate", 384_000)
        put("DirectPlayProfiles", buildJsonArray {
            add(buildJsonObject { put("Type", "Video") })
            add(buildJsonObject { put("Type", "Audio") })
        })
        put("TranscodingProfiles", transcodingProfiles(listOf("h264", "hevc"), listOf("aac", "mp3", "ac3", "eac3", "opus", "flac")))
        put("SubtitleProfiles", buildJsonArray {
            (TEXT_SUBS + IMAGE_SUBS).forEach { f -> add(sub(f, "Embed")) }
            TEXT_SUBS.forEach { f -> add(sub(f, "External")) }
            IMAGE_SUBS.forEach { f -> add(sub(f, "Encode")) }
        })
        put("CodecProfiles", buildJsonArray { })
        put("ContainerProfiles", buildJsonArray { })
    }

    /** Profile limited to what Media3 and this device's decoders handle; anything else is transcoded. */
    fun media3(maxBitrate: Long): JsonObject = buildJsonObject {
        val video = CodecSupport.media3VideoCodecs()
        val audio = CodecSupport.media3AudioCodecs()
        put("Name", "JellyCtrl (Media3)")
        put("MaxStreamingBitrate", maxBitrate)
        put("MaxStaticBitrate", maxBitrate)
        put("MusicStreamingTranscodingBitrate", 384_000)
        put("DirectPlayProfiles", buildJsonArray {
            add(buildJsonObject {
                put("Type", "Video")
                put("Container", "mp4,m4v,mov,mkv,webm,ts,m2ts,mpegts,avi,flv,mpeg,mpg,3gp")
                put("VideoCodec", video.joinToString(","))
                put("AudioCodec", audio.joinToString(","))
            })
            add(buildJsonObject {
                put("Type", "Audio")
                put("Container", "mp3,aac,m4a,m4b,flac,ogg,oga,opus,webm,webma,wav,mka")
            })
        })
        put("TranscodingProfiles", transcodingProfiles(
            listOfNotNull("h264", if (CodecSupport.hevc) "hevc" else null),
            listOfNotNull("aac", "mp3", if (CodecSupport.ac3) "ac3" else null, if (CodecSupport.eac3) "eac3" else null),
        ))
        put("SubtitleProfiles", buildJsonArray {
            listOf("srt", "subrip", "vtt", "webvtt", "ttml", "mov_text", "pgssub", "dvdsub", "dvbsub").forEach { add(sub(it, "Embed")) }
            listOf("srt", "subrip", "vtt", "webvtt", "ass", "ssa", "ttml").forEach { add(sub(it, "External")) }
            listOf("pgssub", "dvdsub", "dvbsub", "vobsub").forEach { add(sub(it, "Encode")) }
        })
        put("CodecProfiles", buildJsonArray {
            if (!CodecSupport.hevc10) {
                add(buildJsonObject {
                    put("Type", "Video")
                    put("Codec", "hevc")
                    put("Conditions", buildJsonArray {
                        add(buildJsonObject {
                            put("Condition", "LessThanEqual")
                            put("Property", "VideoBitDepth")
                            put("Value", "8")
                            put("IsRequired", false)
                        })
                    })
                })
            }
        })
        put("ContainerProfiles", buildJsonArray { })
    }

    private fun transcodingProfiles(video: List<String>, audio: List<String>) = buildJsonArray {
        add(buildJsonObject {
            put("Container", "ts")
            put("Type", "Video")
            put("VideoCodec", video.joinToString(","))
            put("AudioCodec", audio.joinToString(","))
            put("Protocol", "hls")
            put("Context", "Streaming")
            put("MaxAudioChannels", "6")
            put("MinSegments", 1)
            put("BreakOnNonKeyFrames", true)
            put("CopyTimestamps", false)
        })
        add(buildJsonObject {
            put("Container", "mp3")
            put("Type", "Audio")
            put("AudioCodec", "mp3")
            put("Protocol", "http")
            put("Context", "Streaming")
            put("MaxAudioChannels", "2")
        })
    }

    private fun sub(format: String, method: String) = buildJsonObject {
        put("Format", format)
        put("Method", method)
    }
}

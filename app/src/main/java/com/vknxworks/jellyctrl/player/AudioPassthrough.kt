package com.vknxworks.jellyctrl.player

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager

/**
 * Which undecoded (Dolby, DTS) sound formats the sound output connected right now takes directly, as Android
 * reports it for media playback. A phone's own speaker, wired and Bluetooth headphones take none; an HDMI or
 * USB connection to a TV, receiver or soundbar can.
 */
object AudioPassthrough {
    /** [codec]: Jellyfin's codec name; [mpv]: mpv's name for `--audio-spdif`; [label]: what people call it. */
    data class Format(val codec: String, val encoding: Int, val mpv: String, val label: String)

    private val FORMATS = listOf(
        Format("ac3", AudioFormat.ENCODING_AC3, "ac3", "Dolby Digital"),
        Format("eac3", AudioFormat.ENCODING_E_AC3, "eac3", "Dolby Digital Plus"),
        Format("dts", AudioFormat.ENCODING_DTS, "dts", "DTS"),
        Format("dts-hd", AudioFormat.ENCODING_DTS_HD, "dts-hd", "DTS-HD"),
        Format("truehd", AudioFormat.ENCODING_DOLBY_TRUEHD, "truehd", "Dolby TrueHD"),
    )

    /** The formats the current output takes; empty when it takes none or Android can't say. */
    fun supported(context: Context): List<Format> = runCatching {
        val audio = context.getSystemService(AudioManager::class.java) ?: return emptyList()
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
            .build()
        val encodings = audio.getDirectProfilesForAttributes(attributes).map { it.format }.toSet()
        FORMATS.filter { it.encoding in encodings }
    }.getOrDefault(emptyList())

    /** Jellyfin codec names Media3 can pass through right now ("dca" is Jellyfin's other name for DTS). */
    fun jellyfinCodecs(context: Context): Set<String> =
        supported(context).flatMap { if (it.codec == "dts") listOf("dts", "dca") else listOf(it.codec) }
            .filter { it != "dts-hd" }.toSet()
}

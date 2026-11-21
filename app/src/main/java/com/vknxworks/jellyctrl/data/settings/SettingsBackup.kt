package com.vknxworks.jellyctrl.data.settings

import androidx.annotation.StringRes
import com.vknxworks.jellyctrl.R

/**
 * Which of JellyCtrl's settings go into a settings file ([SettingsBackupFile]) and how they come back.
 * Left out on purpose: the app lock and when it locks again (a file can't turn the lock on, off or loosen
 * it), and everything that isn't a setting: accounts, sign-ins, the PIN, keys and downloads.
 */
object SettingsBackup {
    /** One setting in the file: its name there, the label shown in the "what changes" list, and how to read and write it. */
    private class Field(
        val name: String,
        @StringRes val label: Int,
        val get: (AppSettings) -> Any,
        /** [AppSettings] with the file's value, or unchanged when the value isn't one this version offers. */
        val set: (AppSettings, Any) -> AppSettings,
    )

    private fun bool(name: String, @StringRes label: Int, get: (AppSettings) -> Boolean, set: (AppSettings, Boolean) -> AppSettings) =
        Field(name, label, get) { s, v -> if (v is Boolean) set(s, v) else s }

    /** A whole number that must be one of [allowed] (the choices the settings screen offers). */
    private fun int(name: String, @StringRes label: Int, allowed: List<Int>, get: (AppSettings) -> Int, set: (AppSettings, Int) -> AppSettings) =
        Field(name, label, get) { s, v -> if (v is Int && v in allowed) set(s, v) else s }

    private inline fun <reified E : Enum<E>> choice(name: String, @StringRes label: Int, crossinline get: (AppSettings) -> E, crossinline set: (AppSettings, E) -> AppSettings) =
        Field(name, label, { get(it).name }) { s, v -> enumValues<E>().firstOrNull { it.name == v }?.let { set(s, it) } ?: s }

    private val MBPS = listOf(0, 80, 40, 20, 10, 8, 4, 2, 1)
    private val KBPS = listOf(1411, 320, 256, 192, 128)

    private val FIELDS = listOf(
        choice<PlayerEngineChoice>("engine", R.string.video_player, { it.engine }) { s, v -> s.copy(engine = v) },
        int("maxBitrateMbps", R.string.streaming_quality, MBPS, { it.maxBitrateMbps }) { s, v -> s.copy(maxBitrateMbps = v) },
        int("cellularBitrateMbps", R.string.quality_on_mobile_data, MBPS, { it.cellularBitrateMbps }) { s, v -> s.copy(cellularBitrateMbps = v) },
        choice<SkipSegmentMode>("skipIntro", R.string.intros_and_recaps, { it.skipIntro }) { s, v -> s.copy(skipIntro = v) },
        choice<SkipSegmentMode>("skipOutro", R.string.credits, { it.skipOutro }) { s, v -> s.copy(skipOutro = v) },
        bool("autoplayNext", R.string.play_the_next_episode_automatically, { it.autoplayNext }) { s, v -> s.copy(autoplayNext = v) },
        int("seekStepSeconds", R.string.skip_forward_and_back_by, listOf(5, 10, 15, 30), { it.seekStepSeconds }) { s, v -> s.copy(seekStepSeconds = v) },
        bool("gestures", R.string.swipe_gestures, { it.gestures }) { s, v -> s.copy(gestures = v) },
        bool("autoPip", R.string.picture_in_picture_when_you_leave, { it.autoPip }) { s, v -> s.copy(autoPip = v) },
        bool("hwDecoding", R.string.hardware_decoding_in_mpv, { it.hwDecoding }) { s, v -> s.copy(hwDecoding = v) },
        bool("audioPassthrough", R.string.audio_passthrough, { it.audioPassthrough }) { s, v -> s.copy(audioPassthrough = v) },
        int("subtitleSizePercent", R.string.subtitle_size, SUBTITLE_SIZES, { it.subtitleSizePercent }) { s, v -> s.copy(subtitleSizePercent = v) },
        choice<SubtitleColor>("subtitleColor", R.string.subtitle_colour, { it.subtitleColor }) { s, v -> s.copy(subtitleColor = v) },
        int("musicBitrateKbps", R.string.music_quality, KBPS, { it.musicBitrateKbps }) { s, v -> s.copy(musicBitrateKbps = v) },
        bool("downloadWifiOnly", R.string.download_on_wi_fi_only, { it.downloadWifiOnly }) { s, v -> s.copy(downloadWifiOnly = v) },
        int("downloadVideoMbps", R.string.download_video_quality, listOf(0, 20, 10, 8, 6, 4, 3, 2, 1), { it.downloadVideoMbps }) { s, v -> s.copy(downloadVideoMbps = v) },
        int("downloadMusicKbps", R.string.download_music_quality, listOf(0, 320, 256, 192, 128), { it.downloadMusicKbps }) { s, v -> s.copy(downloadMusicKbps = v) },
        int("downloadLimitGb", R.string.download_limit, listOf(0, 2, 4, 8, 16, 32, 64, 128), { it.downloadLimitGb }) { s, v -> s.copy(downloadLimitGb = v) },
        Field("homeOrder", R.string.home_layout, { s -> s.homeOrder.joinToString(",") { it.name } }) { s, v ->
            if (v is String) s.copy(homeOrder = homeOrderFrom(v)) else s
        },
        Field("homeHidden", R.string.home_layout, { s -> s.homeHidden.sortedBy { it.ordinal }.joinToString(",") { it.name } }) { s, v ->
            if (v is String) s.copy(homeHidden = homeHiddenFrom(v)) else s
        },
        // The two privacy switches can be turned on by a file, never off.
        bool("hideScreenshots", R.string.hide_screenshots, { it.hideScreenshots }) { s, v -> s.copy(hideScreenshots = s.hideScreenshots || v) },
        bool("hideRecents", R.string.hide_recents, { it.hideRecents }) { s, v -> s.copy(hideRecents = s.hideRecents || v) },
    )

    /** The settings as they go into a file. */
    fun valuesOf(settings: AppSettings): Map<String, Any> = FIELDS.associate { it.name to it.get(settings) }

    /** [current] with every usable setting from [file]. The app lock and its timeout always stay as they are. */
    fun apply(current: AppSettings, file: SettingsBackupFile): AppSettings {
        val result = FIELDS.fold(current) { s, field -> file.values[field.name]?.let { field.set(s, it) } ?: s }
        return result.copy(appLock = current.appLock, lockTimeout = current.lockTimeout)
    }

    /** Labels (string resources) of the settings [file] would change, in the order of the settings screen, each once. */
    fun changedLabels(current: AppSettings, file: SettingsBackupFile): List<Int> {
        val after = apply(current, file)
        return FIELDS.filter { it.get(current) != it.get(after) }.map { it.label }.distinct()
    }
}

package com.vknxworks.jellyctrl.data.settings

import android.content.Context
import androidx.annotation.StringRes
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.vknxworks.jellyctrl.R
import com.vknxworks.jellyctrl.data.appDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.vknxworks.jellyctrl.str

enum class PlayerEngineChoice(@StringRes private val labelRes: Int) {
    AUTO(R.string.engine_auto),
    MEDIA3(R.string.engine_media3),
    MPV(R.string.engine_mpv);

    val label: String get() = str(labelRes)
}

enum class ThemeChoice(@StringRes private val labelRes: Int) {
    SYSTEM(R.string.theme_system), DARK(R.string.theme_dark), LIGHT(R.string.theme_light), BLACK(R.string.theme_black);

    val label: String get() = str(labelRes)
}

enum class SkipSegmentMode(@StringRes private val labelRes: Int) {
    ASK(R.string.skip_mode_ask), AUTO(R.string.skip_mode_auto), OFF(R.string.skip_mode_off);

    val label: String get() = str(labelRes)
}

/** How long the app can be in the background before the app lock asks again. -1: only after a restart. */
enum class LockTimeout(@StringRes val label: Int, val millis: Long) {
    IMMEDIATELY(R.string.lock_again_immediately, 0L),
    MINUTE_1(R.string.lock_again_1m, 60_000L),
    MINUTES_5(R.string.lock_again_5m, 5 * 60_000L),
    MINUTES_15(R.string.lock_again_15m, 15 * 60_000L),
    HOUR_1(R.string.lock_again_1h, 60 * 60_000L),
    RESTART(R.string.lock_again_restart, -1L),
}

data class AppSettings(
    val engine: PlayerEngineChoice = PlayerEngineChoice.AUTO,
    /** 0 = no limit (direct play whenever possible). */
    val maxBitrateMbps: Int = 0,
    val cellularBitrateMbps: Int = 0,
    val theme: ThemeChoice = ThemeChoice.SYSTEM,
    val dynamicColor: Boolean = false,
    val skipIntro: SkipSegmentMode = SkipSegmentMode.ASK,
    val skipOutro: SkipSegmentMode = SkipSegmentMode.ASK,
    val autoplayNext: Boolean = true,
    val seekStepSeconds: Int = 10,
    val gestures: Boolean = true,
    val autoPip: Boolean = true,
    val downloadWifiOnly: Boolean = true,
    val musicBitrateKbps: Int = 320,
    val hwDecoding: Boolean = true,
    /** The app lock is on (the PIN itself lives in LockVault). */
    val appLock: Boolean = false,
    val lockTimeout: LockTimeout = LockTimeout.IMMEDIATELY,
    /** Hide in screenshots: FLAG_SECURE on the app's windows (which also blanks the recent-apps preview). */
    val hideScreenshots: Boolean = false,
    /** Hide in recent apps: the recent-apps view shows a blank card. Screenshots still work. */
    val hideRecents: Boolean = false,
) {
    fun maxBitrateBps(onCellular: Boolean): Long {
        val mbps = if (onCellular && cellularBitrateMbps > 0) cellularBitrateMbps else maxBitrateMbps
        return if (mbps <= 0) 200_000_000L else mbps * 1_000_000L
    }
}

class SettingsRepository(private val context: Context, scope: CoroutineScope) {
    private object K {
        val engine = stringPreferencesKey("engine")
        val maxBitrate = intPreferencesKey("max_bitrate")
        val cellBitrate = intPreferencesKey("cell_bitrate")
        val theme = stringPreferencesKey("theme")
        val dynamic = booleanPreferencesKey("dynamic_color")
        val skipIntro = stringPreferencesKey("skip_intro")
        val skipOutro = stringPreferencesKey("skip_outro")
        val autoplay = booleanPreferencesKey("autoplay_next")
        val seekStep = intPreferencesKey("seek_step")
        val gestures = booleanPreferencesKey("gestures")
        val autoPip = booleanPreferencesKey("auto_pip")
        val wifiOnly = booleanPreferencesKey("download_wifi_only")
        val musicBitrate = intPreferencesKey("music_bitrate")
        val hwDec = booleanPreferencesKey("hw_decoding")
        val appLock = booleanPreferencesKey("app_lock")
        val lockTimeout = stringPreferencesKey("lock_timeout")
        /** The single "Hide in screenshots and recent apps" switch of the previous version: "on" carries over to both. */
        val hideScreens = booleanPreferencesKey("hide_screens")
        val hideScreenshots = booleanPreferencesKey("hide_screenshots")
        val hideRecents = booleanPreferencesKey("hide_recents")
        /** Left over from the optional stats removed in 0.1.24; deleted at start and on every save. */
        val legacy = listOf(booleanPreferencesKey("usage_stats"), booleanPreferencesKey("usage_stats_asked"))
    }

    val settings: StateFlow<AppSettings> = context.appDataStore.data
        .map { it.toSettings() }
        .stateIn(scope, SharingStarted.Eagerly, AppSettings())

    init {
        // Delete what the removed optional stats stored, so nothing of them stays on the phone.
        scope.launch { context.appDataStore.edit { p -> K.legacy.forEach { p.remove(it) } } }
    }

    /** The stored settings as they're read and changed, without [settings]' placeholder defaults at start-up. */
    val stored: Flow<AppSettings> = context.appDataStore.data.map { it.toSettings() }

    private fun Preferences.toSettings() = AppSettings(
        engine = enumOr(this[K.engine], PlayerEngineChoice.AUTO),
        maxBitrateMbps = this[K.maxBitrate] ?: 0,
        cellularBitrateMbps = this[K.cellBitrate] ?: 0,
        theme = enumOr(this[K.theme], ThemeChoice.SYSTEM),
        dynamicColor = this[K.dynamic] ?: false,
        skipIntro = enumOr(this[K.skipIntro], SkipSegmentMode.ASK),
        skipOutro = enumOr(this[K.skipOutro], SkipSegmentMode.ASK),
        autoplayNext = this[K.autoplay] ?: true,
        seekStepSeconds = this[K.seekStep] ?: 10,
        gestures = this[K.gestures] ?: true,
        autoPip = this[K.autoPip] ?: true,
        downloadWifiOnly = this[K.wifiOnly] ?: true,
        musicBitrateKbps = this[K.musicBitrate] ?: 320,
        hwDecoding = this[K.hwDec] ?: true,
        appLock = this[K.appLock] ?: false,
        lockTimeout = enumOr(this[K.lockTimeout], LockTimeout.IMMEDIATELY),
        hideScreenshots = this[K.hideScreenshots] ?: this[K.hideScreens] ?: false,
        hideRecents = this[K.hideRecents] ?: this[K.hideScreens] ?: false,
    )

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.appDataStore.edit { p ->
            val s = transform(p.toSettings())
            p[K.engine] = s.engine.name
            p[K.maxBitrate] = s.maxBitrateMbps
            p[K.cellBitrate] = s.cellularBitrateMbps
            p[K.theme] = s.theme.name
            p[K.dynamic] = s.dynamicColor
            p[K.skipIntro] = s.skipIntro.name
            p[K.skipOutro] = s.skipOutro.name
            p[K.autoplay] = s.autoplayNext
            p[K.seekStep] = s.seekStepSeconds
            p[K.gestures] = s.gestures
            p[K.autoPip] = s.autoPip
            p[K.wifiOnly] = s.downloadWifiOnly
            p[K.musicBitrate] = s.musicBitrateKbps
            p[K.hwDec] = s.hwDecoding
            // "Protect the server dashboard" (before 0.2.3) is covered by the two switches now.
            p.remove(booleanPreferencesKey("protect_admin_screens"))
            p[K.appLock] = s.appLock
            p[K.lockTimeout] = s.lockTimeout.name
            p[K.hideScreenshots] = s.hideScreenshots
            p[K.hideRecents] = s.hideRecents
            p.remove(K.hideScreens)
            K.legacy.forEach { p.remove(it) }
        }
    }

    private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
        name?.let { n -> enumValues<E>().firstOrNull { it.name == n } } ?: default
}

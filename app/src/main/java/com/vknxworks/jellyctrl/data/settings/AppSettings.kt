package com.vknxworks.jellyctrl.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.vknxworks.jellyctrl.data.appDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class PlayerEngineChoice(val label: String) {
    AUTO("Automatic (Media3, mpv when needed)"),
    MEDIA3("Always Media3 (server transcodes the rest)"),
    MPV("Always mpv"),
}

enum class ThemeChoice(val label: String) { SYSTEM("Follow system"), DARK("Dark"), LIGHT("Light"), BLACK("Pure black") }

enum class SkipSegmentMode(val label: String) { ASK("Show skip button"), AUTO("Skip automatically"), OFF("Off") }

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
    /** Server dashboard screens are hidden from screenshots, screen recording and the recent-apps preview. */
    val protectAdminScreens: Boolean = true,
    /** Opt-in anonymous usage stats (off until the user says yes). */
    val usageStats: Boolean = false,
    /** Whether the one-time usage stats question has been answered. */
    val usageStatsAsked: Boolean = false,
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
        val protectAdmin = booleanPreferencesKey("protect_admin_screens")
        val usageStats = booleanPreferencesKey("usage_stats")
        val usageStatsAsked = booleanPreferencesKey("usage_stats_asked")
    }

    val settings: StateFlow<AppSettings> = context.appDataStore.data
        .map { it.toSettings() }
        .stateIn(scope, SharingStarted.Eagerly, AppSettings())

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
        protectAdminScreens = this[K.protectAdmin] ?: true,
        usageStats = this[K.usageStats] ?: false,
        usageStatsAsked = this[K.usageStatsAsked] ?: false,
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
            p[K.protectAdmin] = s.protectAdminScreens
            p[K.usageStats] = s.usageStats
            p[K.usageStatsAsked] = s.usageStatsAsked
        }
    }

    private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
        name?.let { n -> enumValues<E>().firstOrNull { it.name == n } } ?: default
}

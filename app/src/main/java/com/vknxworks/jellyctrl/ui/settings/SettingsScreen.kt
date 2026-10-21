package com.vknxworks.jellyctrl.ui.settings

import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.vknxworks.jellyctrl.R
import com.vknxworks.jellyctrl.data.lock.LockVault
import com.vknxworks.jellyctrl.data.settings.LockTimeout
import com.vknxworks.jellyctrl.ui.components.GroupRow
import com.vknxworks.jellyctrl.ui.lock.AppLock
import com.vknxworks.jellyctrl.ui.lock.PinRequest
import com.vknxworks.jellyctrl.ui.lock.findActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.jellyctrl.container
import com.vknxworks.jellyctrl.data.api.currentUserRaw
import com.vknxworks.jellyctrl.data.api.str
import com.vknxworks.jellyctrl.data.api.updateUserConfiguration
import com.vknxworks.jellyctrl.data.api.withValues
import com.vknxworks.jellyctrl.data.settings.AppSettings
import com.vknxworks.jellyctrl.data.settings.PlayerEngineChoice
import com.vknxworks.jellyctrl.data.settings.SkipSegmentMode
import com.vknxworks.jellyctrl.data.settings.ThemeChoice
import com.vknxworks.jellyctrl.ui.components.Group
import com.vknxworks.jellyctrl.ui.components.GroupChoice
import com.vknxworks.jellyctrl.ui.components.GroupSwitch
import com.vknxworks.jellyctrl.ui.components.ScreenTopBar
import com.vknxworks.jellyctrl.ui.components.friendlyMessage
import com.vknxworks.jellyctrl.ui.components.rememberLoader
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

private val BITRATES = listOf(
    0 to "Best available", 80 to "80 Mbps", 40 to "40 Mbps", 20 to "20 Mbps", 10 to "10 Mbps",
    8 to "8 Mbps", 4 to "4 Mbps", 2 to "2 Mbps", 1 to "1 Mbps",
)
private val LANGS = listOf(
    "" to "Whatever the file uses", "eng" to "English", "spa" to "Spanish", "fre" to "French", "ger" to "German",
    "ita" to "Italian", "por" to "Portuguese", "dut" to "Dutch", "jpn" to "Japanese", "kor" to "Korean", "chi" to "Chinese",
    "rus" to "Russian", "ara" to "Arabic", "hin" to "Hindi", "tur" to "Turkish", "pol" to "Polish", "swe" to "Swedish",
    "nor" to "Norwegian", "dan" to "Danish", "fin" to "Finnish",
)
private val SUB_MODES = listOf(
    "Default" to "As set in the file",
    "Always" to "Always show",
    "OnlyForced" to "Only forced subtitles",
    "Smart" to "When the audio is in another language",
    "None" to "Never show",
)

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val c = context.container
    val scope = rememberCoroutineScope()
    val s by c.settings.settings.collectAsStateWithLifecycle()
    fun update(t: (AppSettings) -> AppSettings) { scope.launch { c.settings.update(t) } }

    // Language and subtitle preferences live on the server, in the user's configuration.
    val userCfg = rememberLoader("user-config") { c.api.currentUserRaw()["Configuration"] as? JsonObject ?: JsonObject(emptyMap()) }
    fun saveCfg(changes: Map<String, Any?>) {
        val cfg = userCfg.value ?: return
        val updated = cfg.withValues(changes)
        userCfg.set(updated)
        scope.launch {
            runCatching { c.api.updateUserConfiguration(updated) }
                .onFailure { Toast.makeText(context, it.friendlyMessage(), Toast.LENGTH_SHORT).show(); userCfg.reload(silent = true) }
        }
    }
    fun JsonObject.flag(key: String, default: Boolean) = (this[key] as? JsonPrimitive)?.booleanOrNull ?: default

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ScreenTopBar("Settings") },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            Group("Appearance") {
                GroupChoice("Theme", ThemeChoice.entries.map { it to it.label }, s.theme) { v -> update { it.copy(theme = v) } }
                GroupSwitch("Use wallpaper colours", "Replaces the default colours with ones from your wallpaper", s.dynamicColor) { v ->
                    update { it.copy(dynamicColor = v) }
                }
            }

            PrivacyGroup(s) { t -> update(t) }

            Group("Watching") {
                GroupChoice("Streaming quality", BITRATES, s.maxBitrateMbps) { v -> update { it.copy(maxBitrateMbps = v) } }
                GroupChoice("Quality on mobile data", listOf(0 to "Same as on Wi-Fi") + BITRATES.drop(1), s.cellularBitrateMbps) { v ->
                    update { it.copy(cellularBitrateMbps = v) }
                }
                GroupSwitch("Play the next episode automatically", checked = s.autoplayNext) { v -> update { it.copy(autoplayNext = v) } }
                GroupChoice("Intros and recaps", SkipSegmentMode.entries.map { it to it.label }, s.skipIntro) { v -> update { it.copy(skipIntro = v) } }
                GroupChoice("Credits", SkipSegmentMode.entries.map { it to it.label }, s.skipOutro) { v -> update { it.copy(skipOutro = v) } }
            }

            Group("Player") {
                GroupChoice("Skip forward and back by", listOf(5, 10, 15, 30).map { it to "$it seconds" }, s.seekStepSeconds) { v ->
                    update { it.copy(seekStepSeconds = v) }
                }
                GroupSwitch("Swipe gestures", "Brightness on the left, volume on the right, sideways to seek", s.gestures) { v -> update { it.copy(gestures = v) } }
                GroupSwitch("Picture in picture when you leave", checked = s.autoPip) { v -> update { it.copy(autoPip = v) } }
                GroupChoice("Video player", PlayerEngineChoice.entries.map { it to it.label }, s.engine) { v -> update { it.copy(engine = v) } }
                GroupSwitch("Hardware decoding in mpv", "Turn off if mpv shows a black picture", s.hwDecoding) { v -> update { it.copy(hwDecoding = v) } }
            }

            val cfg = userCfg.value
            if (cfg != null) {
                Group("Audio and subtitles", footer = "Saved to your Jellyfin account, so other apps use them too.") {
                    GroupChoice("Audio language", LANGS, cfg.str("AudioLanguagePreference").orEmpty(), fallbackLabel = cfg.str("AudioLanguagePreference").orEmpty()) { v ->
                        saveCfg(mapOf("AudioLanguagePreference" to v))
                    }
                    GroupSwitch("Prefer the file's default audio", checked = cfg.flag("PlayDefaultAudioTrack", true)) { v -> saveCfg(mapOf("PlayDefaultAudioTrack" to v)) }
                    GroupChoice("Subtitle language", LANGS, cfg.str("SubtitleLanguagePreference").orEmpty(), fallbackLabel = cfg.str("SubtitleLanguagePreference").orEmpty()) { v ->
                        saveCfg(mapOf("SubtitleLanguagePreference" to v))
                    }
                    GroupChoice("Show subtitles", SUB_MODES, cfg.str("SubtitleMode") ?: "Default") { v -> saveCfg(mapOf("SubtitleMode" to v)) }
                    GroupSwitch("Remember my audio choices", checked = cfg.flag("RememberAudioSelections", true)) { v -> saveCfg(mapOf("RememberAudioSelections" to v)) }
                    GroupSwitch("Remember my subtitle choices", checked = cfg.flag("RememberSubtitleSelections", true)) { v -> saveCfg(mapOf("RememberSubtitleSelections" to v)) }
                    GroupSwitch("Hide watched items in New", checked = cfg.flag("HidePlayedInLatest", true)) { v -> saveCfg(mapOf("HidePlayedInLatest" to v)) }
                }
            }

            Group("Music and downloads") {
                GroupChoice(
                    "Music quality",
                    listOf(1411 to "Original files", 320 to "320 kbps", 256 to "256 kbps", 192 to "192 kbps", 128 to "128 kbps"),
                    s.musicBitrateKbps,
                ) { v -> update { it.copy(musicBitrateKbps = v) } }
                GroupSwitch("Download on Wi-Fi only", checked = s.downloadWifiOnly) { v -> update { it.copy(downloadWifiOnly = v) } }
            }

        }
    }
}

/** Privacy and security: the shared Ctrl series app lock, and hiding in screenshots and recent apps. */
@Composable
private fun PrivacyGroup(s: AppSettings, update: ((AppSettings) -> AppSettings) -> Unit) {
    val context = LocalContext.current
    val c = context.container
    val bioAvailable = remember { AppLock.biometricAvailable(context) }
    val lockOn = c.vault.enabled
    Group(
        stringResource(R.string.group_privacy_security),
        footer = if (lockOn) stringResource(R.string.app_lock_footer) else null,
    ) {
        GroupSwitch(
            stringResource(R.string.app_lock),
            stringResource(R.string.app_lock_subtitle),
            lockOn,
            icon = Icons.Outlined.Lock,
        ) { on -> AppLock.request = if (on) PinRequest.ENABLE else PinRequest.DISABLE }
        if (lockOn) {
            GroupRow(
                stringResource(R.string.lock_change_pin),
                stringResource(R.string.lock_change_pin_subtitle, LockVault.PIN_MIN),
                icon = Icons.Outlined.Password,
                showChevron = true,
                onClick = { AppLock.request = PinRequest.CHANGE },
            )
            GroupSwitch(
                stringResource(R.string.lock_fingerprint),
                stringResource(if (bioAvailable || c.vault.biometricEnabled) R.string.lock_fingerprint_subtitle else R.string.lock_fingerprint_unavailable),
                c.vault.biometricEnabled,
                icon = Icons.Outlined.Fingerprint,
            ) { on ->
                if (!on) {
                    c.vault.disableBiometric()
                } else if (bioAvailable) {
                    val activity = context.findActivity()
                    val cipher = c.vault.biometricEnrolCipher()
                    if (activity == null || cipher == null) {
                        Toast.makeText(context, context.getString(R.string.lock_fingerprint_setup_failed), Toast.LENGTH_SHORT).show()
                    } else {
                        AppLock.biometricPrompt(
                            activity,
                            context.getString(R.string.lock_fingerprint_enable_title),
                            context.getString(R.string.lock_fingerprint_enable_subtitle),
                            cipher,
                        ) { unlocked ->
                            if (!c.vault.finishBiometricEnrol(unlocked)) {
                                Toast.makeText(context, context.getString(R.string.lock_fingerprint_setup_failed), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
            GroupChoice(
                stringResource(R.string.lock_again),
                LockTimeout.entries.map { it to stringResource(it.label) },
                s.lockTimeout,
                icon = Icons.Outlined.Timer,
            ) { v -> update { it.copy(lockTimeout = v) } }
        }
        GroupSwitch(
            stringResource(R.string.hide_screenshots),
            stringResource(R.string.hide_screenshots_subtitle),
            s.hideScreenshots,
            icon = Icons.Outlined.VisibilityOff,
        ) { v -> update { it.copy(hideScreenshots = v) } }
        GroupSwitch(
            stringResource(R.string.hide_recents),
            stringResource(R.string.hide_recents_subtitle),
            s.hideRecents,
            icon = Icons.Outlined.Layers,
        ) { v -> update { it.copy(hideRecents = v) } }
    }
}

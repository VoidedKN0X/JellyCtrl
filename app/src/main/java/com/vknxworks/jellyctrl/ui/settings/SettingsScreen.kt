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
import com.vknxworks.jellyctrl.str
import androidx.compose.ui.res.pluralStringResource

private val BITRATES: List<Pair<Int, String>>
    get() = listOf(0 to str(R.string.best_available)) + listOf(80, 40, 20, 10, 8, 4, 2, 1).map { it to str(R.string.x_mbps, it) }
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
        topBar = { ScreenTopBar(stringResource(R.string.settings)) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            Group(stringResource(R.string.appearance)) {
                GroupChoice(stringResource(R.string.theme), ThemeChoice.entries.map { it to it.label }, s.theme) { v -> update { it.copy(theme = v) } }
                GroupSwitch(stringResource(R.string.use_wallpaper_colours), stringResource(R.string.replaces_the_default_colours_with_ones), s.dynamicColor) { v ->
                    update { it.copy(dynamicColor = v) }
                }
            }

            PrivacyGroup(s) { t -> update(t) }

            Group(stringResource(R.string.watching)) {
                GroupChoice(stringResource(R.string.streaming_quality), BITRATES, s.maxBitrateMbps) { v -> update { it.copy(maxBitrateMbps = v) } }
                GroupChoice(stringResource(R.string.quality_on_mobile_data), listOf(0 to stringResource(R.string.same_as_on_wi_fi)) + BITRATES.drop(1), s.cellularBitrateMbps) { v ->
                    update { it.copy(cellularBitrateMbps = v) }
                }
                GroupSwitch(stringResource(R.string.play_the_next_episode_automatically), checked = s.autoplayNext) { v -> update { it.copy(autoplayNext = v) } }
                GroupChoice(stringResource(R.string.intros_and_recaps), SkipSegmentMode.entries.map { it to it.label }, s.skipIntro) { v -> update { it.copy(skipIntro = v) } }
                GroupChoice(stringResource(R.string.credits), SkipSegmentMode.entries.map { it to it.label }, s.skipOutro) { v -> update { it.copy(skipOutro = v) } }
            }

            Group(stringResource(R.string.player)) {
                GroupChoice(stringResource(R.string.skip_forward_and_back_by), listOf(5, 10, 15, 30).map { it to pluralStringResource(R.plurals.x_seconds, it, it) }, s.seekStepSeconds) { v ->
                    update { it.copy(seekStepSeconds = v) }
                }
                GroupSwitch(stringResource(R.string.swipe_gestures), stringResource(R.string.brightness_on_the_left_volume_on), s.gestures) { v -> update { it.copy(gestures = v) } }
                GroupSwitch(stringResource(R.string.picture_in_picture_when_you_leave), checked = s.autoPip) { v -> update { it.copy(autoPip = v) } }
                GroupChoice(stringResource(R.string.video_player), PlayerEngineChoice.entries.map { it to it.label }, s.engine) { v -> update { it.copy(engine = v) } }
                GroupSwitch(stringResource(R.string.hardware_decoding_in_mpv), stringResource(R.string.turn_off_if_mpv_shows_a), s.hwDecoding) { v -> update { it.copy(hwDecoding = v) } }
            }

            val cfg = userCfg.value
            if (cfg != null) {
                Group(stringResource(R.string.audio_and_subtitles), footer = stringResource(R.string.saved_to_your_jellyfin_account_so)) {
                    GroupChoice(stringResource(R.string.audio_language), LANGS, cfg.str("AudioLanguagePreference").orEmpty(), fallbackLabel = cfg.str("AudioLanguagePreference").orEmpty()) { v ->
                        saveCfg(mapOf("AudioLanguagePreference" to v))
                    }
                    GroupSwitch("Prefer the file's default audio", checked = cfg.flag("PlayDefaultAudioTrack", true)) { v -> saveCfg(mapOf("PlayDefaultAudioTrack" to v)) }
                    GroupChoice(stringResource(R.string.subtitle_language), LANGS, cfg.str("SubtitleLanguagePreference").orEmpty(), fallbackLabel = cfg.str("SubtitleLanguagePreference").orEmpty()) { v ->
                        saveCfg(mapOf("SubtitleLanguagePreference" to v))
                    }
                    GroupChoice("Show subtitles", SUB_MODES, cfg.str("SubtitleMode") ?: "Default") { v -> saveCfg(mapOf("SubtitleMode" to v)) }
                    GroupSwitch("Remember my audio choices", checked = cfg.flag("RememberAudioSelections", true)) { v -> saveCfg(mapOf("RememberAudioSelections" to v)) }
                    GroupSwitch("Remember my subtitle choices", checked = cfg.flag("RememberSubtitleSelections", true)) { v -> saveCfg(mapOf("RememberSubtitleSelections" to v)) }
                    GroupSwitch("Hide watched items in New", checked = cfg.flag("HidePlayedInLatest", true)) { v -> saveCfg(mapOf("HidePlayedInLatest" to v)) }
                }
            }

            Group(stringResource(R.string.music_and_downloads)) {
                GroupChoice(
                    stringResource(R.string.music_quality),
                    listOf(1411 to stringResource(R.string.original_files), 320 to stringResource(R.string.s_320_kbps), 256 to stringResource(R.string.s_256_kbps), 192 to stringResource(R.string.s_192_kbps), 128 to stringResource(R.string.s_128_kbps)),
                    s.musicBitrateKbps,
                ) { v -> update { it.copy(musicBitrateKbps = v) } }
                GroupSwitch(stringResource(R.string.download_on_wi_fi_only), checked = s.downloadWifiOnly) { v -> update { it.copy(downloadWifiOnly = v) } }
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

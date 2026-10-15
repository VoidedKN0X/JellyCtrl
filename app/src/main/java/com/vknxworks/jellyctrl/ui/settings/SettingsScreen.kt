package com.vknxworks.jellyctrl.ui.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.vknxworks.jellyctrl.ui.components.friendlyMessage
import com.vknxworks.jellyctrl.ui.components.rememberLoader
import com.vknxworks.jellyctrl.ui.nav.LocalNavigator
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive

private val BITRATES = listOf(0 to "Maximum (direct play)", 80 to "80 Mbps", 40 to "40 Mbps", 20 to "20 Mbps", 10 to "10 Mbps", 8 to "8 Mbps", 4 to "4 Mbps", 2 to "2 Mbps", 1 to "1 Mbps")
private val LANGS = listOf("" to "Any / file default", "eng" to "English", "spa" to "Spanish", "fre" to "French", "ger" to "German", "ita" to "Italian", "por" to "Portuguese",
    "dut" to "Dutch", "jpn" to "Japanese", "kor" to "Korean", "chi" to "Chinese", "rus" to "Russian", "ara" to "Arabic", "hin" to "Hindi", "tur" to "Turkish",
    "pol" to "Polish", "swe" to "Swedish", "nor" to "Norwegian", "dan" to "Danish", "fin" to "Finnish")
private val SUB_MODES = listOf("Default" to "Default", "Always" to "Always on", "OnlyForced" to "Forced only", "None" to "Off", "Smart" to "Smart (when audio is foreign)")

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val c = context.container
    val nav = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val s by c.settings.settings.collectAsStateWithLifecycle()
    fun update(t: (AppSettings) -> AppSettings) { scope.launch { c.settings.update(t) } }

    // Server-side playback preferences live in the user's Configuration object.
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

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = nav::back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
            item { Header("Playback") }
            item {
                Choice("Player engine", s.engine.label, PlayerEngineChoice.entries.map { it to it.label }) { v -> update { it.copy(engine = v) } }
            }
            item {
                Choice("Max streaming quality", BITRATES.firstOrNull { it.first == s.maxBitrateMbps }?.second ?: "${s.maxBitrateMbps} Mbps", BITRATES) { v -> update { it.copy(maxBitrateMbps = v) } }
            }
            item {
                Choice("Quality on mobile data", BITRATES.firstOrNull { it.first == s.cellularBitrateMbps }?.second ?: "Same as above",
                    listOf(0 to "Same as above") + BITRATES.drop(1)) { v -> update { it.copy(cellularBitrateMbps = v) } }
            }
            item { Choice("Skip intros & recaps", s.skipIntro.label, SkipSegmentMode.entries.map { it to it.label }) { v -> update { it.copy(skipIntro = v) } } }
            item { Choice("Skip credits", s.skipOutro.label, SkipSegmentMode.entries.map { it to it.label }) { v -> update { it.copy(skipOutro = v) } } }
            item { Toggle("Play next episode automatically", null, s.autoplayNext) { v -> update { it.copy(autoplayNext = v) } } }
            item {
                Choice("Seek step", "${s.seekStepSeconds} seconds", listOf(5, 10, 15, 30).map { it to "$it seconds" }) { v -> update { it.copy(seekStepSeconds = v) } }
            }
            item { Toggle("Gesture controls", "Swipe for brightness, volume and seeking; double-tap to skip", s.gestures) { v -> update { it.copy(gestures = v) } } }
            item { Toggle("Picture-in-picture when leaving", null, s.autoPip) { v -> update { it.copy(autoPip = v) } } }
            item { Toggle("Hardware decoding in mpv", "Turn off if mpv shows a black screen", s.hwDecoding) { v -> update { it.copy(hwDecoding = v) } } }

            item { Header("Audio & subtitles (saved to your Jellyfin account)") }
            val cfg = userCfg.value
            if (cfg != null) {
                item {
                    val cur = cfg.str("AudioLanguagePreference").orEmpty()
                    Choice("Preferred audio language", LANGS.firstOrNull { it.first == cur }?.second ?: cur, LANGS) { v -> saveCfg(mapOf("AudioLanguagePreference" to v)) }
                }
                item {
                    val cur = (cfg["PlayDefaultAudioTrack"] as? JsonPrimitive)?.booleanOrNull ?: true
                    Toggle("Play default audio track", "Ignore the language preference and use the file's default", cur) { v -> saveCfg(mapOf("PlayDefaultAudioTrack" to v)) }
                }
                item {
                    val cur = cfg.str("SubtitleLanguagePreference").orEmpty()
                    Choice("Preferred subtitle language", LANGS.firstOrNull { it.first == cur }?.second ?: cur, LANGS) { v -> saveCfg(mapOf("SubtitleLanguagePreference" to v)) }
                }
                item {
                    val cur = cfg.str("SubtitleMode") ?: "Default"
                    Choice("Subtitle mode", SUB_MODES.firstOrNull { it.first == cur }?.second ?: cur, SUB_MODES) { v -> saveCfg(mapOf("SubtitleMode" to v)) }
                }
                item {
                    val cur = cfg["RememberAudioSelections"]?.jsonPrimitive?.booleanOrNull ?: true
                    Toggle("Remember audio choices", null, cur) { v -> saveCfg(mapOf("RememberAudioSelections" to v)) }
                }
                item {
                    val cur = cfg["RememberSubtitleSelections"]?.jsonPrimitive?.booleanOrNull ?: true
                    Toggle("Remember subtitle choices", null, cur) { v -> saveCfg(mapOf("RememberSubtitleSelections" to v)) }
                }
                item {
                    val cur = cfg["HidePlayedInLatest"]?.jsonPrimitive?.booleanOrNull ?: true
                    Toggle("Hide watched items in Latest", null, cur) { v -> saveCfg(mapOf("HidePlayedInLatest" to v)) }
                }
            }

            item { Header("Music") }
            item {
                Choice("Music streaming quality", "${s.musicBitrateKbps} kbps", listOf(1411 to "Lossless (direct)", 320 to "320 kbps", 256 to "256 kbps", 192 to "192 kbps", 128 to "128 kbps")) { v ->
                    update { it.copy(musicBitrateKbps = v) }
                }
            }

            item { Header("Downloads") }
            item { Toggle("Download on Wi-Fi only", null, s.downloadWifiOnly) { v -> update { it.copy(downloadWifiOnly = v) } } }

            item { Header("Appearance") }
            item { Choice("Theme", s.theme.label, ThemeChoice.entries.map { it to it.label }) { v -> update { it.copy(theme = v) } } }
            item { Toggle("Use wallpaper colors", "Material You dynamic color", s.dynamicColor) { v -> update { it.copy(dynamicColor = v) } } }
        }
    }
}

@Composable
private fun Header(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 4.dp),
    )
}

@Composable
fun Toggle(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = if (subtitle != null) { { Text(subtitle) } } else null,
        trailingContent = { Switch(checked, onCheckedChange = onChange) },
        modifier = Modifier.clickable { onChange(!checked) },
    )
}

@Composable
fun <T> Choice(title: String, current: String, options: List<Pair<T, String>>, onPick: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = { Text(current) },
            modifier = Modifier.clickable { open = true },
        )
        DropdownMenu(open, onDismissRequest = { open = false }) {
            options.forEach { (value, label) ->
                DropdownMenuItem(text = { Text(label) }, onClick = { open = false; onPick(value) })
            }
        }
    }
}

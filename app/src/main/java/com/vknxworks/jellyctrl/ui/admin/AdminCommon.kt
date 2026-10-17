package com.vknxworks.jellyctrl.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.vknxworks.jellyctrl.ui.components.ScreenTopBar
import com.vknxworks.jellyctrl.ui.components.friendlyMessage
import com.vknxworks.jellyctrl.ui.nav.LocalNavigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

/** Runs admin actions and reports success/failure in a snackbar. */
class AdminActions(private val scope: CoroutineScope, val snackbar: SnackbarHostState) {
    fun run(success: String? = null, after: () -> Unit = {}, block: suspend () -> Unit) {
        scope.launch {
            try {
                block()
                after()
                if (success != null) snackbar.showSnackbar(success)
            } catch (e: Throwable) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                snackbar.showSnackbar(e.friendlyMessage())
            }
        }
    }
}

@Composable
fun rememberAdminActions(): AdminActions {
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    return remember { AdminActions(scope, snackbar) }
}

@Composable
fun AdminScaffold(
    title: String,
    actions: AdminActions,
    refreshing: Boolean = false,
    onRefresh: (() -> Unit)? = null,
    topActions: @Composable RowScope.() -> Unit = {},
    floating: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val nav = LocalNavigator.current
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            ScreenTopBar(title, actions = topActions)
        },
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(actions.snackbar) },
        floatingActionButton = floating,
    ) { padding ->
        if (onRefresh != null) {
            PullToRefreshBox(refreshing, onRefresh, Modifier.padding(padding).fillMaxSize()) { content() }
        } else {
            androidx.compose.foundation.layout.Box(Modifier.padding(padding).fillMaxSize()) { content() }
        }
    }
}

/** "EnableRemoteAccess" -> "Enable remote access" */
fun humanize(key: String): String {
    val spaced = key.replace(Regex("([a-z0-9])([A-Z])"), "$1 $2").replace(Regex("([A-Z]+)([A-Z][a-z])"), "$1 $2")
    return spaced.split(' ').mapIndexed { i, w ->
        ACRONYMS[w.lowercase()] ?: if (i == 0) w.lowercase().replaceFirstChar { it.uppercase() } else w.lowercase()
    }.joinToString(" ")
}

private val ACRONYMS = mapOf(
    "tv" to "TV", "hls" to "HLS", "url" to "URL", "urls" to "URLs", "api" to "API", "dlna" to "DLNA", "upnp" to "UPnP",
    "https" to "HTTPS", "http" to "HTTP", "ssl" to "SSL", "ffmpeg" to "FFmpeg", "vaapi" to "VA-API", "qsv" to "QSV",
    "nvenc" to "NVENC", "hevc" to "HEVC", "av1" to "AV1", "ip" to "IP", "ipv4" to "IPv4", "ipv6" to "IPv6", "lan" to "LAN",
    "id" to "ID", "ids" to "IDs", "uri" to "URI", "cpu" to "CPU", "gpu" to "GPU", "hdr" to "HDR", "vpp" to "VPP", "h264" to "H.264",
    "h265" to "H.265", "vp9" to "VP9", "mpeg2" to "MPEG-2", "vc1" to "VC-1", "nfo" to "NFO", "ui" to "UI", "css" to "CSS",
)

/**
 * Edits the simple fields (booleans, numbers, text, string lists) of a JSON settings object.
 * Nested objects are kept untouched so saving never drops settings this app doesn't understand.
 */
@Composable
fun ColumnScope.JsonFieldsEditor(obj: JsonObject, onChange: (String, JsonElement) -> Unit, hidden: Set<String> = emptySet()) {
    obj.entries.filter { it.key !in hidden }.sortedBy { (k, v) -> (if (v is JsonPrimitive && v.booleanOrNull != null) "0" else "1") + k }.forEach { (key, value) ->
        when {
            value is JsonPrimitive && value.booleanOrNull != null && !value.isString -> {
                val checked = value.booleanOrNull == true
                ListItem(
                    headlineContent = { Text(humanize(key)) },
                    trailingContent = { Switch(checked, onCheckedChange = { onChange(key, JsonPrimitive(it)) }) },
                    modifier = Modifier.clickable { onChange(key, JsonPrimitive(!checked)) },
                )
            }
            value is JsonPrimitive && !value.isString && (value.longOrNull != null || value.doubleOrNull != null) -> {
                OutlinedTextField(
                    value = value.content,
                    onValueChange = { t ->
                        val num: JsonPrimitive? = t.toLongOrNull()?.let { JsonPrimitive(it) } ?: t.toDoubleOrNull()?.let { JsonPrimitive(it) }
                        if (num != null) onChange(key, num)
                    },
                    label = { Text(humanize(key)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            value is JsonPrimitive && value.isString && isSecretKey(key) -> SecretField(key, value.content) { onChange(key, JsonPrimitive(it)) }
            value is JsonPrimitive && value.isString -> {
                OutlinedTextField(
                    value = value.content,
                    onValueChange = { onChange(key, JsonPrimitive(it)) },
                    label = { Text(humanize(key)) },
                    singleLine = value.content.length < 80,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            value is JsonNull && isSecretKey(key) -> SecretField(key, "") { onChange(key, JsonPrimitive(it)) }
            value is JsonNull -> {
                OutlinedTextField(
                    value = "",
                    onValueChange = { onChange(key, JsonPrimitive(it)) },
                    label = { Text(humanize(key)) },
                    placeholder = { Text("Not set") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            value is JsonArray && value.all { it is JsonPrimitive && it.isString } -> {
                OutlinedTextField(
                    value = value.joinToString(", ") { (it as JsonPrimitive).content },
                    onValueChange = { t ->
                        onChange(key, JsonArray(t.split(',').map { it.trim() }.filter { it.isNotEmpty() }.map { JsonPrimitive(it) }))
                    },
                    label = { Text(humanize(key) + " (comma separated)") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            else -> Unit
        }
    }
    val skipped = obj.entries.count { (k, v) -> k !in hidden && (v is JsonObject || (v is JsonArray && !v.all { it is JsonPrimitive && it.isString })) }
    if (skipped > 0) {
        Text(
            "$skipped advanced settings aren't shown here and are kept as they are. Use the web dashboard for those.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
        )
    }
}

private val SECRET_KEY_RE = Regex("(?i)(password|passwd|secret|token|api_?key|private_?key|passphrase)")

/** Settings whose value is a credential (e.g. CertificatePassword) are masked until the user chooses to show them. */
fun isSecretKey(key: String): Boolean = SECRET_KEY_RE.containsMatchIn(key)

@Composable
private fun SecretField(key: String, value: String, onChange: (String) -> Unit) {
    var shown by rememberSaveable(key) { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(humanize(key)) },
        placeholder = { Text("Not set") },
        singleLine = true,
        visualTransformation = if (shown) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
        trailingIcon = {
            IconButton(onClick = { shown = !shown }) {
                Icon(if (shown) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, if (shown) "Hide" else "Show")
            }
        },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

fun JsonObject.with(key: String, value: JsonElement): JsonObject = JsonObject(toMutableMap().apply { put(key, value) })

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 6.dp),
    )
}

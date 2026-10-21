package com.vknxworks.jellyctrl.ui.admin

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.PersistableBundle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vknxworks.jellyctrl.container
import com.vknxworks.jellyctrl.data.api.JellyfinClient
import com.vknxworks.jellyctrl.data.api.PackageInfo
import com.vknxworks.jellyctrl.data.api.activityLog
import com.vknxworks.jellyctrl.data.api.apiKeys
import com.vknxworks.jellyctrl.data.api.createApiKey
import com.vknxworks.jellyctrl.data.api.deleteDevice
import com.vknxworks.jellyctrl.data.api.devices
import com.vknxworks.jellyctrl.data.api.disablePlugin
import com.vknxworks.jellyctrl.data.api.enablePlugin
import com.vknxworks.jellyctrl.data.api.installPackage
import com.vknxworks.jellyctrl.data.api.itemRaw
import com.vknxworks.jellyctrl.data.api.namedConfig
import com.vknxworks.jellyctrl.data.api.packages
import com.vknxworks.jellyctrl.data.api.plugins
import com.vknxworks.jellyctrl.data.api.revokeApiKey
import com.vknxworks.jellyctrl.data.api.saveNamedConfig
import com.vknxworks.jellyctrl.data.api.saveServerConfig
import com.vknxworks.jellyctrl.data.api.scheduledTasks
import com.vknxworks.jellyctrl.data.api.serverConfig
import com.vknxworks.jellyctrl.data.api.serverLog
import com.vknxworks.jellyctrl.data.api.serverLogs
import com.vknxworks.jellyctrl.data.api.startTask
import com.vknxworks.jellyctrl.data.api.stopTask
import com.vknxworks.jellyctrl.data.api.str
import com.vknxworks.jellyctrl.data.api.uninstallPlugin
import com.vknxworks.jellyctrl.data.api.updateItem
import com.vknxworks.jellyctrl.data.api.withValues
import com.vknxworks.jellyctrl.ui.components.LoadContent
import com.vknxworks.jellyctrl.ui.components.MessageView
import com.vknxworks.jellyctrl.ui.components.formatBytes
import com.vknxworks.jellyctrl.ui.components.formatDateTime
import com.vknxworks.jellyctrl.ui.components.rememberLoader
import com.vknxworks.jellyctrl.ui.components.rememberPagedLoader
import com.vknxworks.jellyctrl.ui.nav.AdminCatalogRoute
import com.vknxworks.jellyctrl.ui.nav.AdminLogRoute
import com.vknxworks.jellyctrl.ui.nav.LocalNavigator
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import androidx.compose.ui.res.stringResource
import com.vknxworks.jellyctrl.R
import com.vknxworks.jellyctrl.str
import kotlin.math.roundToInt

// ---------- Scheduled tasks ----------

@Composable
fun AdminTasksScreen() {
    val c = LocalContext.current.container
    val actions = rememberAdminActions()
    val client = c.api
    val loader = rememberLoader("admin-tasks") { client.scheduledTasks() }
    LaunchedEffect(loader) { while (true) { delay(3000); loader.reload(silent = true) } }

    AdminScaffold(stringResource(R.string.scheduled_tasks), actions, loader.refreshing, { loader.reload(silent = true) }) {
        LoadContent(loader) { tasks ->
            val grouped = tasks.groupBy { it.category ?: stringResource(R.string.other) }.toSortedMap()
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                grouped.forEach { (cat, list) ->
                    item(key = "h-$cat") { SectionTitle(cat) }
                    items(list, key = { it.id }) { t ->
                        val running = t.state == "Running"
                        ListItem(
                            headlineContent = { Text(t.name.orEmpty()) },
                            supportingContent = {
                                Column {
                                    Text(
                                        if (running) t.currentProgressPercentage?.let { stringResource(R.string.running_x_percent, it.roundToInt()) } ?: stringResource(R.string.task_running)
                                        else t.lastExecutionResult?.let { r -> stringResource(R.string.last_run_x_x, formatDateTime(r.endTimeUtc) ?: "?", r.status.orEmpty()) } ?: stringResource(R.string.never_run),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    if (running) {
                                        LinearProgressIndicator(
                                            progress = { ((t.currentProgressPercentage ?: 0.0) / 100).toFloat() },
                                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                            drawStopIndicator = {},
                                        )
                                    }
                                }
                            },
                            trailingContent = {
                                IconButton(onClick = {
                                    actions.run(after = { loader.reload(silent = true) }) { if (running) client.stopTask(t.id) else client.startTask(t.id) }
                                }) { Icon(if (running) Icons.Filled.Stop else Icons.Filled.PlayArrow, if (running) stringResource(R.string.stop) else stringResource(R.string.run_now)) }
                            },
                        )
                    }
                }
            }
        }
    }
}

// ---------- Plugins ----------

@Composable
fun AdminPluginsScreen() {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val actions = rememberAdminActions()
    val client = c.api
    val loader = rememberLoader("admin-plugins") { client.plugins() }
    var confirmRemove by remember { mutableStateOf<com.vknxworks.jellyctrl.data.api.PluginInfo?>(null) }

    AdminScaffold(
        stringResource(R.string.plugins), actions, loader.refreshing, { loader.reload(silent = true) },
        floating = { ExtendedFloatingActionButton(onClick = { nav.navigate(AdminCatalogRoute) }, icon = { Icon(Icons.Filled.Store, null) }, text = { Text(stringResource(R.string.get_plugins)) }) },
    ) {
        LoadContent(loader) { plugins ->
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
                items(plugins, key = { it.id + it.version }) { p ->
                    val active = p.status == "Active"
                    ListItem(
                        headlineContent = { Text("${p.name}  ${p.version}") },
                        supportingContent = {
                            Column {
                                p.description?.takeIf { it.isNotBlank() }?.let { Text(it, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                                if (p.status != "Active" && p.status != "Disabled") {
                                    Text(p.status.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        },
                        trailingContent = {
                            Row {
                                Switch(active, onCheckedChange = { on ->
                                    actions.run(if (on) str(R.string.enabled_restart_the_server_to_apply) else str(R.string.disabled_restart_the_server_to_apply), after = { loader.reload(silent = true) }) {
                                        if (on) client.enablePlugin(p) else client.disablePlugin(p)
                                    }
                                })
                                if (p.canUninstall) IconButton(onClick = { confirmRemove = p }) { Icon(Icons.Filled.Delete, stringResource(R.string.uninstall)) }
                            }
                        },
                    )
                }
            }
        }
    }

    confirmRemove?.let { p ->
        AlertDialog(
            onDismissRequest = { confirmRemove = null },
            title = { Text(stringResource(R.string.uninstall_x, p.name.orEmpty())) },
            text = { Text(stringResource(R.string.the_server_needs_a_restart_afterwards)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmRemove = null
                    actions.run(str(R.string.uninstalled_restart_the_server_to_finish), after = { loader.reload(silent = true) }) { client.uninstallPlugin(p) }
                }) { Text(stringResource(R.string.uninstall), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmRemove = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
fun AdminCatalogScreen() {
    val c = LocalContext.current.container
    val actions = rememberAdminActions()
    val client = c.api
    val loader = rememberLoader("admin-catalog") {
        val installed = client.plugins().map { it.id.replace("-", "").lowercase() }.toSet()
        client.packages().sortedBy { it.name.lowercase() } to installed
    }
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<PackageInfo?>(null) }

    AdminScaffold(stringResource(R.string.plugin_catalog), actions) {
        LoadContent(loader) { (packages, installed) ->
            Column(Modifier.fillMaxSize()) {
                OutlinedTextField(query, { query = it }, label = { Text(stringResource(R.string.search)) }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(16.dp))
                val filtered = packages.filter { query.isBlank() || it.name.contains(query, true) || it.category.orEmpty().contains(query, true) }
                LazyColumn(Modifier.fillMaxSize()) {
                    items(filtered, key = { it.guid + it.name }) { p ->
                        val isInstalled = p.guid.replace("-", "").lowercase() in installed
                        ListItem(
                            headlineContent = { Text(p.name) },
                            supportingContent = { Text(listOfNotNull(p.category, p.overview ?: p.description).joinToString(" · "), maxLines = 2, overflow = TextOverflow.Ellipsis) },
                            trailingContent = { if (isInstalled) Text(stringResource(R.string.installed), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium) },
                            modifier = Modifier.clickable { selected = p },
                        )
                    }
                }
            }
        }
    }

    selected?.let { p ->
        val latest = p.versions.firstOrNull()
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(p.name) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    p.description?.let { Text(it) }
                    Text(stringResource(R.string.plugin_latest_x_for_x, latest?.version ?: "?", latest?.targetAbi ?: "?"), style = MaterialTheme.typography.bodySmall)
                    p.owner?.let { Text(stringResource(R.string.by_x, it), style = MaterialTheme.typography.bodySmall) }
                    latest?.changelog?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    Text(
                        stringResource(R.string.plugins_run_on_the_server_with),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            },
            confirmButton = {
                TextButton(enabled = latest != null, onClick = {
                    selected = null
                    actions.run(str(R.string.installing_x_restart_the_server_when, p.name)) { client.installPackage(p, latest!!.version) }
                }) { Text(stringResource(R.string.install)) }
            },
            dismissButton = { TextButton(onClick = { selected = null }) { Text(stringResource(R.string.close)) } },
        )
    }
}

// ---------- Logs & activity ----------

@Composable
fun AdminLogsScreen() {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val actions = rememberAdminActions()
    val loader = rememberLoader("admin-logs") { c.api.serverLogs().sortedByDescending { it.dateModified } }
    AdminScaffold(stringResource(R.string.logs), actions, loader.refreshing, { loader.reload(silent = true) }) {
        LoadContent(loader) { logs ->
            LazyColumn(Modifier.fillMaxSize()) {
                items(logs, key = { it.name }) { l ->
                    ListItem(
                        headlineContent = { Text(l.name) },
                        supportingContent = { Text("${formatDateTime(l.dateModified).orEmpty()}  ·  ${formatBytes(l.size)}") },
                        modifier = Modifier.clickable { nav.navigate(AdminLogRoute(l.name)) },
                    )
                }
            }
        }
    }
}

@Composable
fun AdminLogScreen(name: String) {
    val context = LocalContext.current
    val c = context.container
    val actions = rememberAdminActions()
    // Server logs can contain access tokens and API keys from request URLs and headers; they're masked here.
    val loader = rememberLoader("admin-log-$name") {
        val text = c.api.serverLog(name)
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { text.lines().takeLast(3000).map { JellyfinClient.redact(it) } }
    }
    var filter by remember { mutableStateOf("") }
    AdminScaffold(
        name, actions, loader.refreshing, { loader.reload(silent = true) },
        topActions = {
            IconButton(onClick = {
                val text = loader.value?.joinToString("\n").orEmpty()
                // Logs hold user names, IP addresses and file paths: keep them out of clipboard previews.
                val clip = ClipData.newPlainText(name, text).apply {
                    description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }
                }
                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
            }) { Icon(Icons.Filled.ContentCopy, stringResource(R.string.copy)) }
        },
    ) {
        LoadContent(loader) { lines ->
            Column(Modifier.fillMaxSize()) {
                OutlinedTextField(filter, { filter = it }, label = { Text(stringResource(R.string.filter_e_g_err_wrn_a)) }, singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp))
                val shown = if (filter.isBlank()) lines else lines.filter { it.contains(filter, true) }
                val state = rememberLazyListState(initialFirstVisibleItemIndex = (shown.size - 1).coerceAtLeast(0))
                SelectionContainer {
                    LazyColumn(state = state, modifier = Modifier.fillMaxSize().horizontalScroll(rememberScrollState())) {
                        items(shown.size) { i ->
                            val line = shown[i]
                            Text(
                                line,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                softWrap = false,
                                color = when {
                                    "[ERR]" in line || "[FTL]" in line -> MaterialTheme.colorScheme.error
                                    "[WRN]" in line -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.padding(horizontal = 8.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminActivityScreen() {
    val c = LocalContext.current.container
    val actions = rememberAdminActions()
    val client = c.api
    val loader = rememberPagedLoader<com.vknxworks.jellyctrl.data.api.ActivityLogEntry>("admin-activity", 100) { start, limit ->
        val r = client.activityLog(start, limit)
        r.items to r.totalRecordCount
    }
    AdminScaffold(stringResource(R.string.activity), actions, false, { loader.refresh() }) {
        if (loader.items.isEmpty() && !loader.loading) {
            MessageView(Icons.Filled.Info, stringResource(R.string.no_activity_yet))
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(loader.items.size) { i ->
                    if (i >= loader.items.size - 10) LaunchedEffect(i) { loader.loadMore() }
                    val a = loader.items[i]
                    ListItem(
                        headlineContent = { Text(a.name.orEmpty()) },
                        supportingContent = {
                            Text(listOfNotNull(a.shortOverview ?: a.overview, formatDateTime(a.date)).joinToString("\n"), style = MaterialTheme.typography.bodySmall)
                        },
                        leadingContent = {
                            when (a.severity) {
                                "Error", "Critical" -> Icon(Icons.Filled.Error, null, tint = MaterialTheme.colorScheme.error)
                                "Warning", "Warn" -> Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.tertiary)
                                else -> Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                    )
                }
            }
        }
    }
}

// ---------- Devices & API keys ----------

@Composable
fun AdminDevicesScreen() {
    val c = LocalContext.current.container
    val actions = rememberAdminActions()
    val client = c.api
    val loader = rememberLoader("admin-devices") { client.devices().sortedByDescending { it.dateLastActivity } }
    var signingOut by remember { mutableStateOf<com.vknxworks.jellyctrl.data.api.DeviceInfo?>(null) }
    AdminScaffold(stringResource(R.string.devices), actions, loader.refreshing, { loader.reload(silent = true) }) {
        LoadContent(loader) { devices ->
            LazyColumn(Modifier.fillMaxSize()) {
                items(devices, key = { it.id ?: it.name.orEmpty() }) { d ->
                    val isThis = d.id == client.deviceId
                    ListItem(
                        headlineContent = { Text((d.customName ?: d.name).orEmpty() + if (isThis) stringResource(R.string.this_app) else "") },
                        supportingContent = {
                            Text(listOfNotNull("${d.appName} ${d.appVersion.orEmpty()}", d.lastUserName, formatDateTime(d.dateLastActivity)).joinToString("  ·  "))
                        },
                        trailingContent = {
                            if (!isThis && d.id != null) {
                                IconButton(onClick = { signingOut = d }) { Icon(Icons.Filled.Delete, stringResource(R.string.sign_out_device)) }
                            }
                        },
                    )
                }
            }
        }
    }
    signingOut?.let { d ->
        val id = d.id
        AlertDialog(
            onDismissRequest = { signingOut = null },
            title = { Text(stringResource(R.string.sign_out_x, (d.customName ?: d.name).orEmpty())) },
            text = { Text(d.lastUserName?.let { stringResource(R.string.device_remove_body_x, it) } ?: stringResource(R.string.device_remove_body)) },
            confirmButton = {
                TextButton(onClick = {
                    signingOut = null
                    if (id != null) actions.run(str(R.string.device_signed_out), after = { loader.reload(silent = true) }) { client.deleteDevice(id) }
                }) { Text(stringResource(R.string.sign_out), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { signingOut = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
fun AdminKeysScreen() {
    val context = LocalContext.current
    val c = context.container
    val actions = rememberAdminActions()
    val client = c.api
    val loader = rememberLoader("admin-keys") { client.apiKeys() }
    var creating by remember { mutableStateOf(false) }
    var revoking by remember { mutableStateOf<com.vknxworks.jellyctrl.data.api.AuthenticationInfo?>(null) }
    AdminScaffold(
        stringResource(R.string.api_keys), actions, loader.refreshing, { loader.reload(silent = true) },
        floating = { ExtendedFloatingActionButton(onClick = { creating = true }, icon = { Icon(Icons.Filled.Add, null) }, text = { Text(stringResource(R.string.new_key)) }) },
    ) {
        LoadContent(loader) { keys ->
            if (keys.isEmpty()) {
                MessageView(Icons.Filled.Info, stringResource(R.string.no_api_keys), stringResource(R.string.keys_let_other_apps_and_scripts))
                return@LoadContent
            }
            LazyColumn(Modifier.fillMaxSize()) {
                items(keys, key = { it.accessToken }) { k ->
                    ListItem(
                        headlineContent = { Text(k.appName ?: stringResource(R.string.key)) },
                        supportingContent = { Text("${k.accessToken.take(8)}…  ·  ${formatDateTime(k.dateCreated).orEmpty()}") },
                        trailingContent = {
                            Row {
                                IconButton(onClick = {
                                    val clip = ClipData.newPlainText(str(R.string.api_key), k.accessToken).apply {
                                        description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }
                                    }
                                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
                                }) { Icon(Icons.Filled.ContentCopy, stringResource(R.string.copy_key)) }
                                IconButton(onClick = { revoking = k }) { Icon(Icons.Filled.Delete, stringResource(R.string.revoke)) }
                            }
                        },
                    )
                }
            }
        }
    }
    revoking?.let { k ->
        AlertDialog(
            onDismissRequest = { revoking = null },
            title = { Text(k.appName?.let { stringResource(R.string.revoke_x, it) } ?: stringResource(R.string.revoke_this_key)) },
            text = { Text(stringResource(R.string.any_app_or_script_using_this)) },
            confirmButton = {
                TextButton(onClick = {
                    revoking = null
                    actions.run(str(R.string.key_revoked), after = { loader.reload(silent = true) }) { client.revokeApiKey(k.accessToken) }
                }) { Text(stringResource(R.string.revoke), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { revoking = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (creating) {
        var app by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { creating = false },
            title = { Text(stringResource(R.string.new_api_key)) },
            text = { OutlinedTextField(app, { app = it }, label = { Text(stringResource(R.string.app_name_2)) }, singleLine = true) },
            confirmButton = {
                TextButton(enabled = app.isNotBlank(), onClick = {
                    creating = false
                    actions.run(str(R.string.key_created), after = { loader.reload(silent = true) }) { client.createApiKey(app.trim()) }
                }) { Text(stringResource(R.string.create)) }
            },
            dismissButton = { TextButton(onClick = { creating = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

// ---------- Server configuration ----------

/** Edits System/Configuration (section "") or a named section like "encoding" or "network". */
@Composable
fun AdminConfigScreen(section: String, title: String) {
    val c = LocalContext.current.container
    val actions = rememberAdminActions()
    val client = c.api
    val loader = rememberLoader("admin-config-$section") { if (section.isEmpty()) client.serverConfig() else client.namedConfig(section) }
    var edited by remember { mutableStateOf<JsonObject?>(null) }

    AdminScaffold(
        title, actions,
        topActions = {
            IconButton(enabled = edited != null, onClick = {
                val cfg = edited ?: return@IconButton
                actions.run(str(R.string.saved), after = { edited = null; loader.reload(silent = true) }) {
                    if (section.isEmpty()) client.saveServerConfig(cfg) else client.saveNamedConfig(section, cfg)
                }
            }) { Icon(Icons.Filled.Save, stringResource(R.string.save)) }
        },
    ) {
        LoadContent(loader) { original ->
            val cfg = edited ?: original
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
                if (edited != null) {
                    Text(
                        stringResource(R.string.unsaved_changes_tap_save_at_the),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                JsonFieldsEditor(cfg, onChange = { k, v -> edited = cfg.with(k, v) })
            }
        }
    }
}

// ---------- Metadata editor ----------

@Composable
fun EditMetadataScreen(id: String) {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val actions = rememberAdminActions()
    val client = c.api
    val loader = rememberLoader("edit-$id") { client.itemRaw(id) }
    var fields by remember { mutableStateOf<Map<String, String>?>(null) }

    AdminScaffold(stringResource(R.string.edit_metadata), actions) {
        LoadContent(loader) { raw ->
            val keys = listOf(
                "Name", "OriginalTitle", "SortName", "Overview", "ProductionYear", "PremiereDate", "OfficialRating",
                "CommunityRating", "CriticRating", "Genres", "Tags", "Studios",
            )
            val current = fields ?: keys.associateWith { k ->
                when (val v = raw[k]) {
                    is JsonArray -> v.joinToString(", ") { e ->
                        (e as? JsonObject)?.str("Name") ?: (e as? JsonPrimitive)?.content.orEmpty()
                    }
                    is JsonPrimitive -> if (v.isString || v.content != "null") v.content else ""
                    else -> ""
                }
            }.also { fields = it }
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                keys.forEach { k ->
                    OutlinedTextField(
                        value = current[k].orEmpty(),
                        onValueChange = { v -> fields = current + (k to v) },
                        label = { Text(humanize(k) + if (k in setOf("Genres", "Tags", "Studios")) stringResource(R.string.comma_separated) else "") },
                        singleLine = k != "Overview",
                        minLines = if (k == "Overview") 4 else 1,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                HorizontalDivider()
                Button(onClick = {
                    val f = fields ?: return@Button
                    fun list(k: String) = f[k].orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }
                    val changes = mutableMapOf<String, Any?>(
                        "Name" to f["Name"], "OriginalTitle" to f["OriginalTitle"]?.ifBlank { null }, "SortName" to f["SortName"]?.ifBlank { null },
                        "Overview" to f["Overview"], "ProductionYear" to f["ProductionYear"]?.toIntOrNull(),
                        "PremiereDate" to f["PremiereDate"]?.ifBlank { null }, "OfficialRating" to f["OfficialRating"]?.ifBlank { null },
                        "CommunityRating" to f["CommunityRating"]?.toFloatOrNull(), "CriticRating" to f["CriticRating"]?.toFloatOrNull(),
                        "Genres" to list("Genres"), "Tags" to list("Tags"),
                        "Studios" to JsonArray(list("Studios").map { s -> JsonObject(mapOf("Name" to JsonPrimitive(s))) }),
                    )
                    val updated = raw.withValues(changes)
                    actions.run(str(R.string.saved), after = { nav.close(com.vknxworks.jellyctrl.ui.nav.EditMetadataRoute::class) }) { client.updateItem(id, updated) }
                }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.save)) }
            }
        }
    }
}

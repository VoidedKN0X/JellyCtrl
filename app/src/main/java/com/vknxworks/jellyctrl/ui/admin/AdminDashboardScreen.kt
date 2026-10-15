package com.vknxworks.jellyctrl.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Subject
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vknxworks.jellyctrl.container
import com.vknxworks.jellyctrl.data.api.activityLog
import com.vknxworks.jellyctrl.data.api.restartServer
import com.vknxworks.jellyctrl.data.api.scanAllLibraries
import com.vknxworks.jellyctrl.data.api.scheduledTasks
import com.vknxworks.jellyctrl.data.api.sessions
import com.vknxworks.jellyctrl.data.api.shutdownServer
import com.vknxworks.jellyctrl.data.api.systemInfo
import com.vknxworks.jellyctrl.ui.components.LoadContent
import com.vknxworks.jellyctrl.ui.components.formatDateTime
import com.vknxworks.jellyctrl.ui.components.rememberLoader
import com.vknxworks.jellyctrl.ui.nav.AdminActivityRoute
import com.vknxworks.jellyctrl.ui.nav.AdminConfigRoute
import com.vknxworks.jellyctrl.ui.nav.AdminDevicesRoute
import com.vknxworks.jellyctrl.ui.nav.AdminKeysRoute
import com.vknxworks.jellyctrl.ui.nav.AdminLibrariesRoute
import com.vknxworks.jellyctrl.ui.nav.AdminLogsRoute
import com.vknxworks.jellyctrl.ui.nav.AdminPluginsRoute
import com.vknxworks.jellyctrl.ui.nav.AdminTasksRoute
import com.vknxworks.jellyctrl.ui.nav.AdminUsersRoute
import com.vknxworks.jellyctrl.ui.nav.LocalNavigator
import com.vknxworks.jellyctrl.ui.remote.SessionCard
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

private data class AdminLink(val icon: ImageVector, val label: String, val route: Any)

private val LINKS = listOf(
    AdminLink(Icons.Filled.People, "Users", AdminUsersRoute),
    AdminLink(Icons.Filled.Storage, "Libraries", AdminLibrariesRoute),
    AdminLink(Icons.Filled.Schedule, "Scheduled tasks", AdminTasksRoute),
    AdminLink(Icons.Filled.Extension, "Plugins", AdminPluginsRoute),
    AdminLink(Icons.Filled.History, "Activity", AdminActivityRoute),
    AdminLink(Icons.AutoMirrored.Filled.Subject, "Logs", AdminLogsRoute),
    AdminLink(Icons.Filled.Devices, "Devices", AdminDevicesRoute),
    AdminLink(Icons.Filled.Key, "API keys", AdminKeysRoute),
    AdminLink(Icons.Filled.Settings, "General", AdminConfigRoute("", "General settings")),
    AdminLink(Icons.Filled.Movie, "Playback & transcoding", AdminConfigRoute("encoding", "Playback & transcoding")),
    AdminLink(Icons.Filled.Lan, "Networking", AdminConfigRoute("network", "Networking")),
    AdminLink(Icons.Filled.Search, "Metadata", AdminConfigRoute("metadata", "Metadata")),
    AdminLink(Icons.Filled.LiveTv, "Live TV", AdminConfigRoute("livetv", "Live TV")),
    AdminLink(Icons.Filled.Palette, "Branding", AdminConfigRoute("branding", "Branding")),
)

@Composable
fun AdminDashboardScreen() {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val actions = rememberAdminActions()
    val client = c.api
    var confirm by remember { mutableStateOf<String?>(null) }

    val loader = rememberLoader("admin-dash") {
        coroutineScope {
            val info = async { client.systemInfo() }
            val sessions = async { client.sessions(controllableOnly = false, activeWithinSeconds = 960) }
            val activity = async { runCatching { client.activityLog(0, 6).items }.getOrDefault(emptyList()) }
            val tasks = async { runCatching { client.scheduledTasks().filter { it.state == "Running" } }.getOrDefault(emptyList()) }
            Dash(info.await(), sessions.await().filter { it.nowPlayingItem != null || it.isActive }, activity.await(), tasks.await())
        }
    }
    LaunchedEffect(loader) {
        while (true) { delay(10_000); loader.reload(silent = true) }
    }

    AdminScaffold("Dashboard", actions, loader.refreshing, { loader.reload(silent = true) }) {
        LoadContent(loader) { d ->
            LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(d.info.serverName ?: "Jellyfin", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Jellyfin ${d.info.version.orEmpty()}  ·  ${d.info.operatingSystemDisplayName ?: d.info.operatingSystem.orEmpty()}  ·  ${d.info.systemArchitecture.orEmpty()}",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (d.info.hasPendingRestart) Text("Restart pending to apply changes", color = MaterialTheme.colorScheme.error)
                            if (d.info.hasUpdateAvailable) Text("An update is available", color = MaterialTheme.colorScheme.primary)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                                FilledTonalButton(onClick = { actions.run("Library scan started") { client.scanAllLibraries() } }) {
                                    Icon(Icons.Filled.Search, null); Text("  Scan libraries")
                                }
                                FilledTonalButton(onClick = { confirm = "restart" }, enabled = d.info.canSelfRestart) {
                                    Icon(Icons.Filled.RestartAlt, null); Text("  Restart")
                                }
                                FilledTonalButton(onClick = { confirm = "shutdown" }) {
                                    Icon(Icons.Filled.PowerSettingsNew, null); Text("  Shut down")
                                }
                            }
                        }
                    }
                }
                if (d.tasks.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle("Running tasks") }
                    items(d.tasks, key = { "task-" + it.id }, span = { GridItemSpan(maxLineSpan) }) { t ->
                        Column(Modifier.fillMaxWidth()) {
                            Text("${t.name}  ${t.currentProgressPercentage?.let { "%.0f%%".format(it) } ?: ""}")
                            LinearProgressIndicator(progress = { ((t.currentProgressPercentage ?: 0.0) / 100).toFloat() }, modifier = Modifier.fillMaxWidth(), drawStopIndicator = {})
                        }
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle("Manage") }
                items(LINKS, key = { it.label }) { link ->
                    OutlinedCard(Modifier.fillMaxWidth().clickable { nav.navigate(link.route) }) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(link.icon, null, tint = MaterialTheme.colorScheme.primary)
                            Text(link.label, modifier = Modifier.padding(start = 10.dp), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle("Active devices (${d.sessions.size})") }
                items(d.sessions, key = { "s-" + it.id }, span = { GridItemSpan(maxLineSpan) }) { s ->
                    SessionCard(s, client, showUser = true) { loader.reload(silent = true) }
                }
                if (d.activity.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SectionTitle("Recent activity")
                            TextButton(onClick = { nav.navigate(AdminActivityRoute) }) { Text("See all") }
                        }
                    }
                    items(d.activity, key = { "a-" + it.id }, span = { GridItemSpan(maxLineSpan) }) { a ->
                        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(a.name.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                            Text(formatDateTime(a.date).orEmpty(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    confirm?.let { what ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(if (what == "restart") "Restart server?" else "Shut down server?") },
            text = {
                Text(
                    if (what == "restart") "Playback on all devices stops while it restarts."
                    else "The server stops completely. You'll need access to the machine to start it again."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirm = null
                    if (what == "restart") actions.run("Restarting…") { client.restartServer() }
                    else actions.run("Shutting down…") { client.shutdownServer() }
                }) { Text(if (what == "restart") "Restart" else "Shut down", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel") } },
        )
    }
}

private data class Dash(
    val info: com.vknxworks.jellyctrl.data.api.SystemInfo,
    val sessions: List<com.vknxworks.jellyctrl.data.api.SessionInfo>,
    val activity: List<com.vknxworks.jellyctrl.data.api.ActivityLogEntry>,
    val tasks: List<com.vknxworks.jellyctrl.data.api.TaskInfo>,
)

package com.vknxworks.jellyctrl.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.vknxworks.jellyctrl.ui.components.Group
import com.vknxworks.jellyctrl.ui.components.GroupRow
import com.vknxworks.jellyctrl.ui.components.RoundAction
import com.vknxworks.jellyctrl.ui.components.SectionHeader
import com.vknxworks.jellyctrl.ui.theme.Gutter
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
    AdminLink(Icons.Filled.History, "Activity log", AdminActivityRoute),
    AdminLink(Icons.AutoMirrored.Filled.Subject, "Logs", AdminLogsRoute),
    AdminLink(Icons.Filled.Devices, "Devices", AdminDevicesRoute),
    AdminLink(Icons.Filled.Key, "API keys", AdminKeysRoute),
    AdminLink(Icons.Filled.Settings, "General", AdminConfigRoute("", "General")),
    AdminLink(Icons.Filled.Movie, "Playback and transcoding", AdminConfigRoute("encoding", "Playback and transcoding")),
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

    AdminScaffold("Server", actions, loader.refreshing, { loader.reload(silent = true) }) {
        LoadContent(loader) { d ->
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
                item("server") {
                    Column(Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(d.info.serverName ?: "Jellyfin", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            listOfNotNull(
                                "Jellyfin ${d.info.version.orEmpty()}",
                                d.info.operatingSystemDisplayName ?: d.info.operatingSystem,
                                d.info.systemArchitecture,
                            ).joinToString(", "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (d.info.hasPendingRestart) Text("Restart the server to finish applying changes", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodyMedium)
                        if (d.info.hasUpdateAvailable) Text("A Jellyfin update is available", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                            RoundAction(Icons.Filled.Search, "Scan libraries", { actions.run("Scanning libraries") { client.scanAllLibraries() } })
                            if (d.info.canSelfRestart) RoundAction(Icons.Filled.RestartAlt, "Restart", { confirm = "restart" })
                            RoundAction(Icons.Filled.PowerSettingsNew, "Shut down", { confirm = "shutdown" })
                        }
                    }
                }
                if (d.tasks.isNotEmpty()) {
                    item("tasks") {
                        Group("Running now") {
                            d.tasks.forEach { t ->
                                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                                    Row {
                                        Text(t.name.orEmpty(), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                        Text(t.currentProgressPercentage?.let { "%.0f%%".format(it) }.orEmpty(), style = MaterialTheme.typography.labelLarge)
                                    }
                                    LinearProgressIndicator(
                                        progress = { ((t.currentProgressPercentage ?: 0.0) / 100).toFloat() },
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                        drawStopIndicator = {},
                                    )
                                }
                            }
                        }
                    }
                }
                item("manage") {
                    Group("Manage") {
                        LINKS.take(4).forEach { l -> GroupRow(l.label, icon = l.icon, showChevron = true, onClick = { nav.navigate(l.route) }) }
                    }
                    Group("Activity and access") {
                        LINKS.drop(4).take(4).forEach { l -> GroupRow(l.label, icon = l.icon, showChevron = true, onClick = { nav.navigate(l.route) }) }
                    }
                    Group("Settings") {
                        LINKS.drop(8).forEach { l -> GroupRow(l.label, icon = l.icon, showChevron = true, onClick = { nav.navigate(l.route) }) }
                    }
                }
                item("sessions-title") {
                    SectionHeader(if (d.sessions.isEmpty()) "Nobody's watching right now" else "Watching now")
                }
                items(d.sessions, key = { "s-" + it.id }) { s ->
                    Box(Modifier.padding(horizontal = Gutter, vertical = 6.dp)) {
                        SessionCard(s, client, showUser = true) { loader.reload(silent = true) }
                    }
                }
                if (d.activity.isNotEmpty()) {
                    item("activity") {
                        SectionHeader("Recent activity", onMore = { nav.navigate(AdminActivityRoute) })
                        Group(modifier = Modifier.padding(top = 0.dp)) {
                            d.activity.forEach { a ->
                                GroupRow(a.name.orEmpty(), formatDateTime(a.date))
                            }
                        }
                    }
                }
            }
        }
    }

    confirm?.let { what ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(if (what == "restart") "Restart the server?" else "Shut down the server?") },
            text = {
                Text(
                    if (what == "restart") "Everyone's playback stops for a minute or two while it restarts."
                    else "The server stops completely. You'll need to start it again on the machine it runs on."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirm = null
                    if (what == "restart") actions.run("Restarting") { client.restartServer() }
                    else actions.run("Shutting down") { client.shutdownServer() }
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

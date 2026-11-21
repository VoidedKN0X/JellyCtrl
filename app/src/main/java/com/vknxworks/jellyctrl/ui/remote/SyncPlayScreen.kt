package com.vknxworks.jellyctrl.ui.remote

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.jellyctrl.R
import com.vknxworks.jellyctrl.container
import com.vknxworks.jellyctrl.data.realtime.SyncGroup
import com.vknxworks.jellyctrl.player.PlayerActivity
import com.vknxworks.jellyctrl.ui.components.CtrlAlertDialog
import com.vknxworks.jellyctrl.ui.components.CtrlTextField
import com.vknxworks.jellyctrl.ui.components.Group
import com.vknxworks.jellyctrl.ui.components.GroupRow
import com.vknxworks.jellyctrl.ui.components.LoadContent
import com.vknxworks.jellyctrl.ui.components.ScreenTopBar
import com.vknxworks.jellyctrl.ui.components.WhileOnScreen
import com.vknxworks.jellyctrl.ui.components.friendlyMessage
import com.vknxworks.jellyctrl.ui.components.rememberLoader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * Watch together (Jellyfin's SyncPlay): start a group or join one on this server. While in a group, what you
 * play is played for everyone in it, and play, pause and seeking move everyone together.
 */
@Composable
fun SyncPlayScreen() {
    val context = LocalContext.current
    val c = context.container
    val scope = rememberCoroutineScope()
    val group by c.syncPlay.group.collectAsStateWithLifecycle()
    val queue by c.syncPlay.queue.collectAsStateWithLifecycle()
    val connected by c.socket.connected.collectAsStateWithLifecycle()
    var busy by remember { mutableStateOf(false) }
    var naming by remember { mutableStateOf(false) }

    // While this screen is open the live connection is kept up, also before a group is joined.
    DisposableEffect(Unit) {
        c.syncPlay.screenOpen.value = true
        onDispose { c.syncPlay.screenOpen.value = false }
    }

    val groups = rememberLoader("syncplay-groups") { c.syncPlay.listGroups().also { c.syncPlay.reconcile(it) } }
    // Other people's groups come and go: looked up again every few seconds while this screen shows.
    WhileOnScreen(groups) { back ->
        if (back) groups.reload(silent = true)
        while (true) {
            delay(5000)
            groups.reload(silent = true)
        }
    }

    fun act(block: suspend () -> Unit) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Toast.makeText(context, e.friendlyMessage(), Toast.LENGTH_LONG).show()
            } finally {
                busy = false
                groups.reload(silent = true)
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ScreenTopBar(stringResource(R.string.syncplay_title)) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            val current = group
            if (current != null) {
                Group(stringResource(R.string.syncplay_your_group), footer = stringResource(R.string.syncplay_in_group_footer)) {
                    GroupRow(
                        current.groupName.ifBlank { stringResource(R.string.syncplay_title) },
                        listOf(
                            stateLabel(current.state),
                            if (current.participants.isEmpty()) null else current.participants.joinToString(", "),
                            if (connected) null else stringResource(R.string.syncplay_reconnecting),
                        ).filterNotNull().joinToString(" · "),
                        icon = Icons.Outlined.Groups,
                        iconTint = MaterialTheme.colorScheme.primary,
                    )
                    if (queue?.current != null) {
                        GroupRow(
                            stringResource(R.string.syncplay_open_playing),
                            stringResource(R.string.syncplay_open_playing_summary),
                            icon = Icons.Filled.PlayArrow,
                            showChevron = true,
                            onClick = { PlayerActivity.startSync(context) },
                        )
                    }
                    GroupRow(
                        stringResource(R.string.syncplay_leave),
                        icon = Icons.AutoMirrored.Outlined.Logout,
                        iconTint = MaterialTheme.colorScheme.error,
                        titleColor = MaterialTheme.colorScheme.error,
                        onClick = { act { c.syncPlay.leave() } },
                    )
                }
            } else {
                Group(stringResource(R.string.syncplay_start), footer = stringResource(R.string.syncplay_intro)) {
                    GroupRow(
                        stringResource(R.string.syncplay_new_group),
                        stringResource(R.string.syncplay_new_group_summary),
                        icon = Icons.Outlined.Add,
                        showChevron = true,
                        onClick = { naming = true },
                    )
                }
            }

            LoadContent(groups) { all ->
                val others = all.filter { g -> current == null || !com.vknxworks.jellyctrl.data.realtime.sameId(g.groupId, current.groupId) }
                Group(stringResource(if (current == null) R.string.syncplay_groups else R.string.syncplay_other_groups)) {
                    if (others.isEmpty()) {
                        GroupRow(
                            stringResource(R.string.syncplay_no_groups),
                            stringResource(R.string.syncplay_no_groups_summary),
                            icon = Icons.Outlined.Refresh,
                            onClick = { groups.reload(silent = true) },
                        )
                    }
                    others.forEach { g ->
                        GroupRow(
                            g.groupName.ifBlank { stringResource(R.string.syncplay_title) },
                            listOf(
                                stateLabel(g.state),
                                pluralStringResource(R.plurals.syncplay_people, g.participants.size, g.participants.size),
                                g.participants.joinToString(", ").ifBlank { null },
                            ).filterNotNull().joinToString(" · "),
                            icon = Icons.Outlined.Groups,
                            showChevron = true,
                            onClick = { act { c.syncPlay.join(g.groupId) } },
                        )
                    }
                }
            }
        }
    }

    if (naming) {
        val userName = c.sessions.active.collectAsStateWithLifecycle().value?.account?.userName.orEmpty()
        val suggestion = stringResource(R.string.syncplay_default_name, userName)
        var name by rememberSaveable { mutableStateOf(suggestion) }
        CtrlAlertDialog(
            onDismissRequest = { naming = false },
            title = { Text(stringResource(R.string.syncplay_new_group)) },
            text = {
                CtrlTextField(
                    value = name,
                    onValueChange = { name = it.take(60) },
                    label = { Text(stringResource(R.string.syncplay_group_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    enabled = name.isNotBlank(),
                    onClick = {
                        naming = false
                        val chosen = name.trim()
                        act { c.syncPlay.create(chosen) }
                    },
                ) { Text(stringResource(R.string.syncplay_create)) }
            },
            dismissButton = { TextButton(onClick = { naming = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun stateLabel(state: String): String = when (state) {
    "Playing" -> stringResource(R.string.syncplay_state_playing)
    "Paused" -> stringResource(R.string.syncplay_state_paused)
    "Waiting" -> stringResource(R.string.syncplay_state_waiting)
    else -> stringResource(R.string.syncplay_state_idle)
}

package com.vknxworks.jellyctrl.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.jellyctrl.ui.components.LocalBottomClearance
import com.vknxworks.jellyctrl.BuildConfig
import com.vknxworks.jellyctrl.R
import com.vknxworks.jellyctrl.container
import com.vknxworks.jellyctrl.data.api.changeOwnPassword
import com.vknxworks.jellyctrl.data.api.currentUser
import com.vknxworks.jellyctrl.data.api.quickConnectAuthorize
import com.vknxworks.jellyctrl.data.api.userViews
import com.vknxworks.jellyctrl.data.download.DownloadStatus
import com.vknxworks.jellyctrl.ui.components.ArtImage
import com.vknxworks.jellyctrl.ui.components.Group
import com.vknxworks.jellyctrl.ui.components.GroupRow
import com.vknxworks.jellyctrl.ui.components.PillButton
import com.vknxworks.jellyctrl.ui.components.ScreenTopBar
import com.vknxworks.jellyctrl.ui.components.friendlyMessage
import com.vknxworks.jellyctrl.ui.components.rememberLoader
import com.vknxworks.jellyctrl.ui.nav.AccountsRoute
import com.vknxworks.jellyctrl.ui.nav.AddAccountRoute
import com.vknxworks.jellyctrl.ui.nav.AdminRoute
import com.vknxworks.jellyctrl.ui.nav.DownloadsRoute
import com.vknxworks.jellyctrl.ui.nav.FavoritesRoute
import com.vknxworks.jellyctrl.ui.nav.LiveTvRoute
import com.vknxworks.jellyctrl.ui.nav.LocalNavigator
import com.vknxworks.jellyctrl.ui.nav.QuickConnectApproveRoute
import com.vknxworks.jellyctrl.ui.nav.RemoteRoute
import com.vknxworks.jellyctrl.ui.nav.SettingsRoute
import com.vknxworks.jellyctrl.ui.theme.Gutter
import kotlinx.coroutines.launch
import com.vknxworks.jellyctrl.str
import androidx.compose.ui.res.pluralStringResource

private const val SPONSOR_URL = "https://github.com/sponsors/VoidedKN0X"
private const val REPO_URL = "https://github.com/VoidedKN0X/JellyCtrl"
private const val ISSUES_URL = "$REPO_URL/issues"
private const val LICENCE_URL = "https://www.gnu.org/licenses/gpl-3.0.html"

/** The You page, opened from your profile picture: who you are, your stuff, and where the settings live. */
@Composable
fun ProfileScreen() {
    val context = LocalContext.current
    val c = context.container
    val nav = LocalNavigator.current
    val session by c.sessions.active.collectAsStateWithLifecycle()
    val downloads by c.downloads.entries.collectAsStateWithLifecycle()
    val acc = session?.account
    val client = session?.client
    val views = rememberLoader("profile-views") { client?.userViews().orEmpty() }
    // Refresh the admin flag in case the user's role changed on the server.
    rememberLoader("profile-me") { client?.currentUser()?.let { c.sessions.updateAdminFlag(it.policy?.isAdministrator == true) } }
    var changePassword by remember { mutableStateOf(false) }
    val ready = downloads.count { it.status == DownloadStatus.DONE }
    val hasLiveTv = views.value?.any { it.collectionType == "livetv" } == true

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar(stringResource(R.string.you))
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp + LocalBottomClearance.current)) {
            Column(
                Modifier.fillMaxWidth().padding(start = Gutter, end = Gutter, top = 8.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.size(96.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                    if (acc?.userImageTag != null && client != null) {
                        ArtImage(client.userImageUrl(acc.userId, acc.userImageTag), Modifier.fillMaxSize())
                    } else {
                        Text(acc?.userName?.take(1)?.uppercase().orEmpty(), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(acc?.userName.orEmpty(), style = MaterialTheme.typography.headlineMedium)
                Text(
                    acc?.serverName.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (acc?.serverUrl?.startsWith("http://", true) == true) {
                    Text(
                        stringResource(R.string.not_encrypted_http),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Group {
                GroupRow(stringResource(R.string.favourites), icon = Icons.Filled.Favorite, showChevron = true, onClick = { nav.navigate(FavoritesRoute) })
                GroupRow(
                    stringResource(R.string.downloads),
                    if (ready == 0) null else pluralStringResource(R.plurals.ready_offline, ready, ready),
                    icon = Icons.Filled.Download,
                    showChevron = true,
                    onClick = { nav.navigate(DownloadsRoute) },
                )
                if (hasLiveTv) GroupRow(stringResource(R.string.live_tv), icon = Icons.Filled.LiveTv, showChevron = true, onClick = { nav.navigate(LiveTvRoute) })
            }
            Group {
                GroupRow(stringResource(R.string.control_another_device), stringResource(R.string.play_and_pause_jellyfin_on_your), icon = Icons.Filled.Cast, showChevron = true, onClick = { nav.navigate(RemoteRoute) })
                GroupRow(stringResource(R.string.sign_in_another_device), stringResource(R.string.approve_a_quick_connect_code), icon = Icons.Filled.Key, showChevron = true, onClick = { nav.navigate(QuickConnectApproveRoute) })
            }
            Group {
                GroupRow(stringResource(R.string.settings), icon = Icons.Filled.Settings, showChevron = true, onClick = { nav.navigate(SettingsRoute) })
                GroupRow(stringResource(R.string.switch_account_or_server), icon = Icons.Filled.SwapHoriz, showChevron = true, onClick = { nav.navigate(AccountsRoute) })
                GroupRow(stringResource(R.string.change_password), icon = Icons.Filled.Password, showChevron = true, onClick = { changePassword = true })
            }
            if (acc?.isAdmin == true) {
                Group {
                    GroupRow(
                        stringResource(R.string.server_dashboard),
                        stringResource(R.string.users_libraries_plugins_tasks_and_logs),
                        icon = Icons.Filled.AdminPanelSettings,
                        showChevron = true,
                        onClick = { nav.navigate(AdminRoute) },
                    )
                }
            }
            Group(stringResource(R.string.group_about)) {
                GroupRow(
                    stringResource(R.string.support_title),
                    stringResource(R.string.support_subtitle),
                    icon = Icons.Filled.Favorite,
                    showChevron = true,
                    onClick = { openLink(context, SPONSOR_URL) },
                )
                GroupRow(
                    stringResource(R.string.bugs_title),
                    stringResource(R.string.bugs_subtitle),
                    icon = Icons.Filled.BugReport,
                    showChevron = true,
                    onClick = { openLink(context, ISSUES_URL) },
                )
                GroupRow(
                    stringResource(R.string.source_title),
                    stringResource(R.string.source_subtitle),
                    icon = Icons.Filled.Code,
                    showChevron = true,
                    onClick = { openLink(context, REPO_URL) },
                )
                GroupRow(
                    stringResource(R.string.licence_title),
                    stringResource(R.string.licence_subtitle),
                    icon = Icons.Filled.Gavel,
                    showChevron = true,
                    onClick = { openLink(context, LICENCE_URL) },
                )
            }
            Text(
                stringResource(R.string.about_footer, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )
        }
    }

    if (changePassword) ChangePasswordDialog { changePassword = false }
}

/** Opens one of the app's own https links (sponsor page, issues, source, licence) in the browser. */
private fun openLink(context: android.content.Context, url: String) {
    runCatching { com.vknxworks.jellyctrl.ui.lock.AppLock.expectReturn(); context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE)) }
}

@Composable
private fun ChangePasswordDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val c = context.container
    val scope = rememberCoroutineScope()
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    // Passwords stay out of screenshots and screen recordings.
    com.vknxworks.jellyctrl.ui.lock.SecureScreen()
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(securePolicy = androidx.compose.ui.window.SecureFlagPolicy.SecureOn),
        title = { Text(stringResource(R.string.change_password)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(current, { current = it }, label = { Text(stringResource(R.string.current_password)) }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                OutlinedTextField(new, { new = it }, label = { Text(stringResource(R.string.new_password)) }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                OutlinedTextField(confirm, { confirm = it }, label = { Text(stringResource(R.string.new_password_again)) }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (new != confirm) { error = str(R.string.the_new_passwords_don_t_match); return@TextButton }
                scope.launch {
                    runCatching { c.api.changeOwnPassword(current, new) }
                        .onSuccess { Toast.makeText(context, str(R.string.password_changed), Toast.LENGTH_SHORT).show(); onDismiss() }
                        .onFailure { error = it.friendlyMessage() }
                }
            }) { Text(stringResource(R.string.change_password)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
fun AccountsScreen() {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val accounts by c.sessions.accounts.collectAsStateWithLifecycle()
    val active by c.sessions.active.collectAsStateWithLifecycle()
    var signOut by remember { mutableStateOf<String?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ScreenTopBar(stringResource(R.string.accounts_and_servers)) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
            accounts.groupBy { it.serverName to it.serverUrl }.forEach { (server, list) ->
                val footer = if (server.second.startsWith("http://", true)) "${server.second}  ·  Not encrypted (HTTP)" else server.second
                Group(server.first, footer = footer) {
                    list.forEach { a ->
                        val isActive = a.key == active?.account?.key
                        GroupRow(
                            a.userName,
                            when {
                                isActive && a.isAdmin -> stringResource(R.string.signed_in_now_administrator)
                                isActive -> stringResource(R.string.signed_in_now)
                                a.isAdmin -> stringResource(R.string.administrator)
                                else -> stringResource(R.string.tap_to_switch)
                            },
                            icon = if (isActive) Icons.Filled.CheckCircle else Icons.Filled.Person,
                            iconTint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = if (isActive) null else ({ scope.launch { c.sessions.switchTo(a.key) }; Unit }),
                        ) {
                            IconButton(onClick = { signOut = a.key }) {
                                Icon(Icons.AutoMirrored.Filled.Logout, stringResource(R.string.sign_out_x_2, a.userName), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            Group {
                GroupRow(stringResource(R.string.add_an_account_or_server), icon = Icons.Filled.Add, onClick = { nav.navigate(AddAccountRoute) })
            }
        }
    }

    signOut?.let { key ->
        val acc = accounts.firstOrNull { it.key == key }
        AlertDialog(
            onDismissRequest = { signOut = null },
            title = { Text(stringResource(R.string.sign_out_x, acc?.userName.orEmpty())) },
            text = { Text(stringResource(R.string.this_signs_the_account_out_on)) },
            confirmButton = { TextButton(onClick = { signOut = null; scope.launch { c.sessions.signOut(key) } }) { Text(stringResource(R.string.sign_out)) } },
            dismissButton = { TextButton(onClick = { signOut = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
fun QuickConnectApproveScreen() {
    val context = LocalContext.current
    val c = context.container
    val nav = LocalNavigator.current
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var confirming by remember { mutableStateOf(false) }
    val active by c.sessions.active.collectAsStateWithLifecycle()
    val account = active?.account

    fun approve() {
        busy = true
        scope.launch {
            runCatching { c.api.quickConnectAuthorize(code) }
                .onSuccess { ok ->
                    Toast.makeText(context, if (ok) str(R.string.device_signed_in) else str(R.string.that_code_wasn_t_accepted_check), Toast.LENGTH_LONG).show()
                    if (ok) nav.close(com.vknxworks.jellyctrl.ui.nav.QuickConnectApproveRoute::class)
                }
                .onFailure { Toast.makeText(context, it.friendlyMessage(), Toast.LENGTH_LONG).show() }
            busy = false
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ScreenTopBar(stringResource(R.string.sign_in_another_device)) },
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(horizontal = Gutter, vertical = 16.dp).widthIn(max = 480.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                stringResource(R.string.on_the_other_device_choose_quick),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                if (account?.isAdmin == true) stringResource(R.string.quick_connect_warning_admin) else stringResource(R.string.quick_connect_warning),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            OutlinedTextField(
                code, { code = it.filter(Char::isDigit).take(6) },
                singleLine = true,
                placeholder = { Text("000000", style = MaterialTheme.typography.displaySmall.copy(letterSpacing = 8.sp)) },
                textStyle = MaterialTheme.typography.displaySmall.copy(letterSpacing = 8.sp, textAlign = TextAlign.Center),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            Row {
                PillButton(
                    if (busy) stringResource(R.string.approving) else stringResource(R.string.approve),
                    enabled = code.length == 6 && !busy,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { confirming = true },
                )
            }
        }
    }

    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text(account?.userName?.let { stringResource(R.string.quick_connect_title_x, it) } ?: stringResource(R.string.quick_connect_title)) },
            text = {
                Text(
                    if (account?.isAdmin == true) stringResource(R.string.quick_connect_body_admin, code)
                    else stringResource(R.string.quick_connect_body, code)
                )
            },
            confirmButton = { TextButton(onClick = { confirming = false; approve() }) { Text(stringResource(R.string.sign_it_in)) } },
            dismissButton = { TextButton(onClick = { confirming = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

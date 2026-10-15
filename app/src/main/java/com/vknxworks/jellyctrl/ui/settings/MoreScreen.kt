package com.vknxworks.jellyctrl.ui.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.jellyctrl.BuildConfig
import com.vknxworks.jellyctrl.container
import com.vknxworks.jellyctrl.data.api.changeOwnPassword
import com.vknxworks.jellyctrl.data.api.currentUser
import com.vknxworks.jellyctrl.data.api.quickConnectAuthorize
import com.vknxworks.jellyctrl.data.api.userViews
import com.vknxworks.jellyctrl.ui.components.ArtImage
import com.vknxworks.jellyctrl.ui.components.friendlyMessage
import com.vknxworks.jellyctrl.ui.components.rememberLoader
import com.vknxworks.jellyctrl.ui.nav.AccountsRoute
import com.vknxworks.jellyctrl.ui.nav.AddAccountRoute
import com.vknxworks.jellyctrl.ui.nav.AdminRoute
import com.vknxworks.jellyctrl.ui.nav.LibraryRoute
import com.vknxworks.jellyctrl.ui.nav.LiveTvRoute
import com.vknxworks.jellyctrl.ui.nav.LocalNavigator
import com.vknxworks.jellyctrl.ui.nav.QuickConnectApproveRoute
import com.vknxworks.jellyctrl.ui.nav.RemoteRoute
import com.vknxworks.jellyctrl.ui.nav.SettingsRoute
import kotlinx.coroutines.launch

@Composable
fun MoreScreen() {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val session by c.sessions.active.collectAsStateWithLifecycle()
    val acc = session?.account
    val client = session?.client
    val views = rememberLoader("more-views") { client?.userViews().orEmpty() }
    // Refresh the admin flag in case the user's role changed on the server.
    rememberLoader("more-me") { client?.currentUser()?.let { c.sessions.updateAdminFlag(it.policy?.isAdministrator == true) } }
    var changePassword by remember { mutableStateOf(false) }

    Scaffold(contentWindowInsets = WindowInsets(0), topBar = { TopAppBar(title = { Text("More") }) }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                ListItem(
                    headlineContent = { Text(acc?.userName ?: "", fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text("${acc?.serverName}  ·  ${acc?.serverUrl}") },
                    leadingContent = {
                        Box(Modifier.size(44.dp).clip(CircleShape)) {
                            if (acc?.userImageTag != null && client != null) ArtImage(client.userImageUrl(acc.userId, acc.userImageTag), Modifier.fillMaxSize())
                            else Icon(Icons.Filled.Person, null, Modifier.align(Alignment.Center))
                        }
                    },
                    modifier = Modifier.clickable { nav.navigate(AccountsRoute) },
                )
                HorizontalDivider()
            }
            val libs = views.value.orEmpty()
            if (libs.isNotEmpty()) {
                item { Section("Libraries") }
                items(libs, key = { it.id }) { v ->
                    Entry(if (v.collectionType == "livetv") Icons.Filled.LiveTv else Icons.Filled.VideoLibrary, v.name.orEmpty()) {
                        if (v.collectionType == "livetv") nav.navigate(LiveTvRoute) else nav.navigate(LibraryRoute(v.id, v.name.orEmpty(), v.collectionType))
                    }
                }
            }
            item { Section("App") }
            item { Entry(Icons.Filled.Cast, "Control other devices", "Remote control Jellyfin on your TV or browser") { nav.navigate(RemoteRoute) } }
            item { Entry(Icons.Filled.QrCode2, "Quick Connect", "Sign in another device with a code") { nav.navigate(QuickConnectApproveRoute) } }
            item { Entry(Icons.Filled.Settings, "Settings", "Playback, player engine, downloads, appearance") { nav.navigate(SettingsRoute) } }
            item { Entry(Icons.Filled.ManageAccounts, "Accounts & servers", "Switch user or server") { nav.navigate(AccountsRoute) } }
            item { Entry(Icons.Filled.Password, "Change password") { changePassword = true } }
            if (acc?.isAdmin == true) {
                item { Section("Server") }
                item { Entry(Icons.Filled.AdminPanelSettings, "Admin dashboard", "Users, libraries, tasks, plugins, logs and settings") { nav.navigate(AdminRoute) } }
            }
            item { Section("About") }
            item { Entry(Icons.Filled.Info, "JellyCtrl ${BuildConfig.VERSION_NAME}", "by VKNXWorks") {} }
        }
    }

    if (changePassword) ChangePasswordDialog { changePassword = false }
}

@Composable
private fun Section(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun Entry(icon: ImageVector, title: String, subtitle: String? = null, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = if (subtitle != null) { { Text(subtitle) } } else null,
        leadingContent = { Icon(icon, null) },
        modifier = Modifier.clickable(onClick = onClick),
    )
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(current, { current = it }, label = { Text("Current password") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                OutlinedTextField(new, { new = it }, label = { Text("New password") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                OutlinedTextField(confirm, { confirm = it }, label = { Text("Confirm new password") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (new != confirm) { error = "Passwords don't match"; return@TextButton }
                scope.launch {
                    runCatching { c.api.changeOwnPassword(current, new) }
                        .onSuccess { Toast.makeText(context, "Password changed", Toast.LENGTH_SHORT).show(); onDismiss() }
                        .onFailure { error = it.friendlyMessage() }
                }
            }) { Text("Change") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
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
        topBar = {
            TopAppBar(
                title = { Text("Accounts & servers") },
                navigationIcon = { IconButton(onClick = nav::back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize()) {
            items(accounts, key = { it.key }) { a ->
                val isActive = a.key == active?.account?.key
                ListItem(
                    headlineContent = { Text(a.userName, fontWeight = if (isActive) FontWeight.Bold else null) },
                    supportingContent = { Text("${a.serverName}  ·  ${a.serverUrl}" + if (a.isAdmin) "  ·  Admin" else "") },
                    leadingContent = { Icon(if (isActive) Icons.Filled.CheckCircle else Icons.Filled.Person, null, tint = if (isActive) MaterialTheme.colorScheme.primary else androidx.compose.material3.LocalContentColor.current) },
                    trailingContent = { IconButton(onClick = { signOut = a.key }) { Icon(Icons.AutoMirrored.Filled.Logout, "Sign out") } },
                    modifier = Modifier.clickable(enabled = !isActive) { scope.launch { c.sessions.switchTo(a.key) } },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Add account or server") },
                    leadingContent = { Icon(Icons.Filled.Add, null) },
                    modifier = Modifier.clickable { nav.navigate(AddAccountRoute) },
                )
            }
        }
    }

    signOut?.let { key ->
        val acc = accounts.firstOrNull { it.key == key }
        AlertDialog(
            onDismissRequest = { signOut = null },
            title = { Text("Sign out of ${acc?.userName}?") },
            text = { Text("This signs the account out on the server and removes it from this phone. Downloads from it stay until you delete them.") },
            confirmButton = { TextButton(onClick = { signOut = null; scope.launch { c.sessions.signOut(key) } }) { Text("Sign out") } },
            dismissButton = { TextButton(onClick = { signOut = null }) { Text("Cancel") } },
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
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("Quick Connect") },
                navigationIcon = { IconButton(onClick = nav::back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(24.dp).widthIn(max = 480.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Enter the code shown on the device you want to sign in. It will be signed in as you.")
            OutlinedTextField(
                code, { code = it.filter(Char::isDigit).take(6) },
                label = { Text("Code") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Row {
                Button(
                    enabled = code.length == 6 && !busy,
                    onClick = {
                        busy = true
                        scope.launch {
                            runCatching { c.api.quickConnectAuthorize(code) }
                                .onSuccess { ok ->
                                    Toast.makeText(context, if (ok) "Device signed in" else "Code not accepted", Toast.LENGTH_SHORT).show()
                                    if (ok) nav.back()
                                }
                                .onFailure { Toast.makeText(context, it.friendlyMessage(), Toast.LENGTH_LONG).show() }
                            busy = false
                        }
                    },
                ) { Text("Authorize") }
            }
        }
    }
}

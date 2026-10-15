package com.vknxworks.jellyctrl.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vknxworks.jellyctrl.container
import com.vknxworks.jellyctrl.ui.components.ArtImage
import com.vknxworks.jellyctrl.ui.theme.JellyBlue
import com.vknxworks.jellyctrl.ui.theme.JellyPurple

@Composable
fun ServerSetupFlow(onDone: () -> Unit, onCancel: (() -> Unit)?) {
    val c = LocalContext.current.container
    val vm: ServerSetupViewModel = viewModel { ServerSetupViewModel(c) }
    val server = vm.server

    BackHandler(enabled = server != null || onCancel != null) {
        if (server != null) vm.backToServer() else onCancel?.invoke()
    }

    Box(
        Modifier.fillMaxSize().systemBarsPadding().imePadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (server != null || onCancel != null) {
                    IconButton(onClick = { if (server != null) vm.backToServer() else onCancel?.invoke() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
                Column {
                    Text("JellyCtrl", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = JellyBlue)
                    Text(
                        if (server == null) "Connect to your Jellyfin server" else "Sign in to ${server.info.serverName ?: "Jellyfin"}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (server == null) ServerStep(vm) else LoginStep(vm, onDone)
        }
    }
}

@Composable
private fun ServerStep(vm: ServerSetupViewModel) {
    OutlinedTextField(
        value = vm.address,
        onValueChange = { vm.address = it },
        label = { Text("Server address") },
        placeholder = { Text("jellyfin.example.com or 192.168.1.10:8096") },
        singleLine = true,
        leadingIcon = { Icon(Icons.Filled.Dns, null) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(onGo = { vm.connect() }),
        modifier = Modifier.fillMaxWidth(),
        enabled = !vm.checking,
    )
    vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    Button(onClick = { vm.connect() }, enabled = !vm.checking && vm.address.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
        if (vm.checking) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Connect")
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Servers on this network", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        if (vm.discovering) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        else IconButton(onClick = { vm.discover() }) { Icon(Icons.Filled.Refresh, "Search again") }
    }
    if (vm.discovered.isEmpty() && !vm.discovering) {
        Text(
            "None found. Enter the address manually - it's the same one you use in a browser.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    vm.discovered.forEach { s ->
        Card(Modifier.fillMaxWidth().clickable { vm.address = s.address; vm.connect(s.address) }) {
            ListItem(
                headlineContent = { Text(s.name.ifBlank { "Jellyfin" }) },
                supportingContent = { Text(s.address) },
                leadingContent = { Icon(Icons.Filled.Dns, null, tint = JellyPurple) },
            )
        }
    }
}

@Composable
private fun LoginStep(vm: ServerSetupViewModel, onDone: () -> Unit) {
    val c = LocalContext.current.container
    val server = vm.server ?: return
    var showPassword by remember { mutableStateOf(false) }

    if (server.url.startsWith("http://")) {
        Card {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Warning, null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(10.dp))
                Text(
                    "This connection isn't encrypted. Your password and media travel in plain text - fine at home, risky elsewhere.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
    Text(
        "${server.url}  ·  Jellyfin ${server.info.version ?: ""}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    if (vm.publicUsers.isNotEmpty()) {
        Text("Who's watching?", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            vm.publicUsers.forEach { u ->
                Column(
                    Modifier.width(76.dp).clip(MaterialTheme.shapes.medium).clickable { vm.username = u.name.orEmpty(); vm.password = "" },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val client = remember(server.url) { c.sessions.anonymousClient(server.url) }
                    Box(Modifier.size(64.dp).clip(CircleShape)) {
                        if (u.primaryImageTag != null) {
                            ArtImage(client.userImageUrl(u.id, u.primaryImageTag), Modifier.fillMaxSize())
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Person, null, Modifier.size(36.dp))
                            }
                        }
                    }
                    Text(u.name.orEmpty(), style = MaterialTheme.typography.labelMedium, maxLines = 1, textAlign = TextAlign.Center)
                }
            }
        }
        HorizontalDivider()
    }

    OutlinedTextField(
        value = vm.username,
        onValueChange = { vm.username = it },
        label = { Text("Username") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = vm.password,
        onValueChange = { vm.password = it },
        label = { Text("Password") },
        singleLine = true,
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { showPassword = !showPassword }) {
                Icon(if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, "Show password")
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { vm.signIn(onDone) }),
        modifier = Modifier.fillMaxWidth(),
    )
    vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    Button(onClick = { vm.signIn(onDone) }, enabled = !vm.signingIn, modifier = Modifier.fillMaxWidth()) {
        if (vm.signingIn) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Sign in")
    }

    if (vm.quickConnectAvailable) {
        val code = vm.quickConnectCode
        if (code == null) {
            OutlinedButton(onClick = { vm.startQuickConnect(onDone) }, modifier = Modifier.fillMaxWidth()) {
                Text("Use Quick Connect")
            }
        } else {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Enter this code in Jellyfin on a signed-in device", textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text(code, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = JellyBlue)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Profile → Quick Connect. Waiting for approval…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = { vm.cancelQuickConnect() }) { Text("Cancel") }
                }
            }
        }
    }
}

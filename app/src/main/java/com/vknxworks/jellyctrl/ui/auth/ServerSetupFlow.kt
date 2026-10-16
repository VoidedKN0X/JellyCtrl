package com.vknxworks.jellyctrl.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vknxworks.jellyctrl.container
import com.vknxworks.jellyctrl.ui.components.ArtImage
import com.vknxworks.jellyctrl.ui.components.Group
import com.vknxworks.jellyctrl.ui.components.GroupRow
import com.vknxworks.jellyctrl.ui.components.PillButton

@Composable
fun ServerSetupFlow(onDone: () -> Unit, onCancel: (() -> Unit)?) {
    val c = LocalContext.current.container
    val vm: ServerSetupViewModel = viewModel { ServerSetupViewModel(c) }
    val server = vm.server

    BackHandler(enabled = server != null || onCancel != null) {
        if (server != null) vm.backToServer() else onCancel?.invoke()
    }

    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding().imePadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.height(48.dp)) {
                if (server != null || onCancel != null) {
                    IconButton(onClick = { if (server != null) vm.backToServer() else onCancel?.invoke() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            }
            Text(
                if (server == null) "JellyCtrl" else "Sign in",
                style = MaterialTheme.typography.displaySmall,
            )
            Text(
                if (server == null) "Enter the address of your Jellyfin server, the same one you'd type in a browser."
                else "to ${server.info.serverName ?: "your Jellyfin server"}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            if (server == null) ServerStep(vm) else LoginStep(vm, onDone)
        }
    }
}

@Composable
private fun Field(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboard: KeyboardOptions = KeyboardOptions.Default,
    actions: KeyboardActions = KeyboardActions.Default,
    enabled: Boolean = true,
    visual: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
) {
    TextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = keyboard,
        keyboardActions = actions,
        visualTransformation = visual,
        trailingIcon = trailing,
        shape = MaterialTheme.shapes.medium,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun ServerStep(vm: ServerSetupViewModel) {
    Field(
        vm.address, { vm.address = it },
        label = "Server address",
        keyboard = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
        actions = KeyboardActions(onGo = { vm.connect() }),
        enabled = !vm.checking,
    )
    vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
    if (vm.checking) {
        Box(Modifier.fillMaxWidth().height(52.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(28.dp)) }
    } else {
        PillButton("Continue", onClick = { vm.connect() }, enabled = vm.address.isNotBlank(), modifier = Modifier.fillMaxWidth())
    }

    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "On this network",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (vm.discovering) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        else IconButton(onClick = { vm.discover() }) { Icon(Icons.Filled.Refresh, "Look again", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
    if (vm.discovered.isEmpty() && !vm.discovering) {
        Text(
            "No servers found nearby. That's normal if your server is on another network.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else if (vm.discovered.isNotEmpty()) {
        Group(padded = false) {
            vm.discovered.forEach { s ->
                GroupRow(
                    s.name.ifBlank { "Jellyfin" },
                    s.address,
                    icon = Icons.Filled.Dns,
                    showChevron = true,
                    onClick = { vm.address = s.address; vm.connect(s.address) },
                )
            }
        }
    }
}

@Composable
private fun LoginStep(vm: ServerSetupViewModel, onDone: () -> Unit) {
    val c = LocalContext.current.container
    val server = vm.server ?: return
    var showPassword by remember { mutableStateOf(false) }
    val secure = server.url.startsWith("https://")

    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceContainer).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (secure) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
            null,
            tint = if (secure) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(server.url, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (secure) "Jellyfin ${server.info.version.orEmpty()}, encrypted connection"
                else "Not encrypted. Fine on your home network, risky anywhere else.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (vm.publicUsers.isNotEmpty()) {
        val client = remember(server.url) { c.sessions.anonymousClient(server.url) }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(vm.publicUsers, key = { it.id }) { u ->
                val selected = vm.username.equals(u.name, ignoreCase = true)
                Column(
                    Modifier.width(72.dp).clip(MaterialTheme.shapes.medium).clickable { vm.username = u.name.orEmpty(); vm.password = "" }.padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(if (selected) 3.dp else 0.dp)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (u.primaryImageTag != null) {
                            ArtImage(client.userImageUrl(u.id, u.primaryImageTag), Modifier.fillMaxSize())
                        } else {
                            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
                                Text(u.name?.take(1)?.uppercase().orEmpty(), style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(u.name.orEmpty(), style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                }
            }
        }
    }

    Field(
        vm.username, { vm.username = it },
        label = "Username",
        keyboard = KeyboardOptions(imeAction = ImeAction.Next),
    )
    Field(
        vm.password, { vm.password = it },
        label = "Password",
        visual = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        keyboard = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        actions = KeyboardActions(onDone = { vm.signIn(onDone) }),
        trailing = {
            IconButton(onClick = { showPassword = !showPassword }) {
                Icon(if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, if (showPassword) "Hide password" else "Show password")
            }
        },
    )
    vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
    if (vm.signingIn) {
        Box(Modifier.fillMaxWidth().height(52.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(28.dp)) }
    } else {
        PillButton("Sign in", onClick = { vm.signIn(onDone) }, modifier = Modifier.fillMaxWidth())
    }

    if (vm.quickConnectAvailable) {
        val code = vm.quickConnectCode
        if (code == null) {
            TextButton(onClick = { vm.startQuickConnect(onDone) }, modifier = Modifier.fillMaxWidth()) {
                Text("Sign in with a code instead")
            }
        } else {
            Column(
                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceContainer).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Your code", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    code.chunked(3).joinToString(" "),
                    style = MaterialTheme.typography.displayMedium.copy(letterSpacing = 6.sp),
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "On a device that's already signed in, open your profile, choose Quick Connect and enter this code. This screen continues by itself.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                TextButton(onClick = { vm.cancelQuickConnect() }) { Text("Cancel") }
            }
        }
    }
}

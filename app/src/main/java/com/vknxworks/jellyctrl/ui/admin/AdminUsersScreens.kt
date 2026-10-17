package com.vknxworks.jellyctrl.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.vknxworks.jellyctrl.container
import com.vknxworks.jellyctrl.data.api.adminSetPassword
import com.vknxworks.jellyctrl.data.api.allUsers
import com.vknxworks.jellyctrl.data.api.createUser
import com.vknxworks.jellyctrl.data.api.deleteUser
import com.vknxworks.jellyctrl.data.api.str
import com.vknxworks.jellyctrl.data.api.updateUser
import com.vknxworks.jellyctrl.data.api.updateUserPolicy
import com.vknxworks.jellyctrl.data.api.userRaw
import com.vknxworks.jellyctrl.data.api.virtualFolders
import com.vknxworks.jellyctrl.ui.components.ArtImage
import com.vknxworks.jellyctrl.ui.components.LoadContent
import com.vknxworks.jellyctrl.ui.components.formatDateTime
import com.vknxworks.jellyctrl.ui.components.rememberLoader
import com.vknxworks.jellyctrl.ui.nav.AdminUserRoute
import com.vknxworks.jellyctrl.ui.nav.LocalNavigator
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive

/** Password keyboards don't learn or suggest what's typed. */
private val PasswordKeyboard = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false)

@Composable
fun AdminUsersScreen() {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val actions = rememberAdminActions()
    val client = c.api
    val loader = rememberLoader("admin-users") { client.allUsers() }
    var creating by remember { mutableStateOf(false) }

    AdminScaffold(
        "Users", actions, loader.refreshing, { loader.reload(silent = true) },
        floating = { ExtendedFloatingActionButton(onClick = { creating = true }, icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Add user") }) },
    ) {
        LoadContent(loader) { users ->
            LazyColumn(Modifier.fillMaxSize()) {
                items(users, key = { it.id }) { u ->
                    ListItem(
                        headlineContent = { Text(u.name.orEmpty()) },
                        supportingContent = {
                            Text(
                                listOfNotNull(
                                    if (u.policy?.isAdministrator == true) "Administrator" else null,
                                    if (u.policy?.isDisabled == true) "Disabled" else null,
                                    if (u.policy?.isHidden == true) "Hidden" else null,
                                    u.lastActivityDate?.let { "Active ${formatDateTime(it)}" },
                                ).joinToString("  ·  ").ifBlank { "Never signed in" }
                            )
                        },
                        leadingContent = {
                            Box(Modifier.size(40.dp).clip(CircleShape)) {
                                if (u.primaryImageTag != null) ArtImage(client.userImageUrl(u.id, u.primaryImageTag), Modifier.fillMaxSize())
                                else Icon(
                                    when {
                                        u.policy?.isDisabled == true -> Icons.Filled.Block
                                        u.policy?.isAdministrator == true -> Icons.Filled.AdminPanelSettings
                                        else -> Icons.Filled.Person
                                    },
                                    null,
                                    Modifier.align(Alignment.Center),
                                )
                            }
                        },
                        modifier = Modifier.clickable { nav.navigate(AdminUserRoute(u.id)) },
                    )
                }
            }
        }
    }

    if (creating) {
        var name by remember { mutableStateOf("") }
        var pw by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { creating = false },
            title = { Text("New user") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                    OutlinedTextField(pw, { pw = it }, label = { Text("Password (optional)") }, singleLine = true, visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = PasswordKeyboard)
                }
            },
            confirmButton = {
                TextButton(enabled = name.isNotBlank(), onClick = {
                    creating = false
                    actions.run("User created", after = { loader.reload(silent = true) }) { client.createUser(name.trim(), pw) }
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { creating = false }) { Text("Cancel") } },
        )
    }
}

@Composable
fun AdminUserScreen(id: String) {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val actions = rememberAdminActions()
    val client = c.api
    val loader = rememberLoader("admin-user-$id") {
        coroutineScope {
            val user = async { client.userRaw(id) }
            val libs = async { runCatching { client.virtualFolders() }.getOrDefault(emptyList()) }
            user.await() to libs.await()
        }
    }
    var policy by remember { mutableStateOf<JsonObject?>(null) }
    var name by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var newPassword by remember { mutableStateOf<String?>(null) }

    AdminScaffold(name ?: "User", actions) {
        LoadContent(loader) { (user, libs) ->
            val p = policy ?: (user["Policy"] as? JsonObject)?.also { policy = it } ?: JsonObject(emptyMap())
            val currentName = name ?: user.str("Name").orEmpty().also { name = it }
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
                SectionTitle("Profile")
                OutlinedTextField(
                    currentName, { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val updatedUser = JsonObject(user.toMutableMap().apply { put("Name", JsonPrimitive(currentName)) })
                        actions.run("Saved", after = { loader.reload(silent = true) }) {
                            if (currentName != user.str("Name")) client.updateUser(id, updatedUser)
                            client.updateUserPolicy(id, p)
                        }
                    }) { Text("Save changes") }
                    OutlinedButton(onClick = { newPassword = "" }) { Text("Set password") }
                }

                SectionTitle("Library access")
                val allFolders = p["EnableAllFolders"]?.jsonPrimitive?.booleanOrNull ?: true
                ListItem(
                    headlineContent = { Text("Access to all libraries") },
                    trailingContent = { Checkbox(allFolders, onCheckedChange = { policy = p.with("EnableAllFolders", JsonPrimitive(it)) }) },
                )
                if (!allFolders) {
                    val enabled = (p["EnabledFolders"] as? JsonArray)?.map { it.jsonPrimitive.content }.orEmpty().toSet()
                    libs.forEach { lib ->
                        val libId = lib.itemId ?: return@forEach
                        val on = libId in enabled
                        ListItem(
                            headlineContent = { Text(lib.name.orEmpty()) },
                            leadingContent = {
                                Checkbox(on, onCheckedChange = { checked ->
                                    val next = if (checked) enabled + libId else enabled - libId
                                    policy = p.with("EnabledFolders", JsonArray(next.map { JsonPrimitive(it) }))
                                })
                            },
                        )
                    }
                }

                SectionTitle("Permissions")
                JsonFieldsEditor(
                    p,
                    onChange = { k, v -> policy = p.with(k, v) },
                    hidden = setOf(
                        "EnableAllFolders", "EnabledFolders", "AuthenticationProviderId", "PasswordResetProviderId",
                        "BlockedTags", "AllowedTags", "BlockUnratedItems", "AccessSchedules", "EnabledChannels", "EnabledDevices",
                        "BlockedMediaFolders", "BlockedChannels", "EnableContentDeletionFromFolders", "SyncPlayAccess",
                    ),
                )

                HorizontalDivider(Modifier.padding(vertical = 16.dp))
                TextButton(onClick = { confirmDelete = true }, modifier = Modifier.padding(horizontal = 8.dp)) {
                    Text("Delete user", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    newPassword?.let { pw ->
        AlertDialog(
            onDismissRequest = { newPassword = null },
            title = { Text("Set password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(pw, { newPassword = it }, label = { Text("New password (blank removes it)") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(), keyboardOptions = PasswordKeyboard)
                    if (pw.isEmpty()) {
                        Text(
                            "With no password, anyone who can reach the server can sign in as ${name ?: "this user"}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    newPassword = null
                    actions.run(if (pw.isEmpty()) "Password removed" else "Password updated") { client.adminSetPassword(id, pw) }
                }) {
                    if (pw.isEmpty()) Text("Remove password", color = MaterialTheme.colorScheme.error) else Text("Save")
                }
            },
            dismissButton = { TextButton(onClick = { newPassword = null }) { Text("Cancel") } },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${name ?: "user"}?") },
            text = { Text("Their watch history, favorites and settings are removed. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    actions.run(after = { nav.close(com.vknxworks.jellyctrl.ui.nav.AdminUserRoute::class) }) { client.deleteUser(id) }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

package com.vknxworks.jellyctrl.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vknxworks.jellyctrl.container
import com.vknxworks.jellyctrl.data.api.FileSystemEntry
import com.vknxworks.jellyctrl.data.api.JellyfinClient
import com.vknxworks.jellyctrl.data.api.VirtualFolderInfo
import com.vknxworks.jellyctrl.data.api.addLibraryPath
import com.vknxworks.jellyctrl.data.api.addVirtualFolder
import com.vknxworks.jellyctrl.data.api.directoryContents
import com.vknxworks.jellyctrl.data.api.drives
import com.vknxworks.jellyctrl.data.api.parentPath
import com.vknxworks.jellyctrl.data.api.refreshItem
import com.vknxworks.jellyctrl.data.api.removeLibraryPath
import com.vknxworks.jellyctrl.data.api.removeVirtualFolder
import com.vknxworks.jellyctrl.data.api.renameVirtualFolder
import com.vknxworks.jellyctrl.data.api.scanAllLibraries
import com.vknxworks.jellyctrl.data.api.virtualFolders
import com.vknxworks.jellyctrl.ui.components.LoadContent
import com.vknxworks.jellyctrl.ui.components.friendlyMessage
import com.vknxworks.jellyctrl.ui.components.rememberLoader
import kotlinx.coroutines.delay

private val COLLECTION_TYPES = listOf(
    "movies" to "Movies", "tvshows" to "Shows", "music" to "Music", "musicvideos" to "Music videos",
    "homevideos" to "Home videos & photos", "boxsets" to "Collections", "books" to "Books", "mixed" to "Mixed movies & shows",
)

@Composable
fun AdminLibrariesScreen() {
    val c = LocalContext.current.container
    val actions = rememberAdminActions()
    val client = c.api
    val loader = rememberLoader("admin-libs") { client.virtualFolders() }
    var adding by remember { mutableStateOf(false) }
    var addPathTo by remember { mutableStateOf<String?>(null) }
    var renaming by remember { mutableStateOf<VirtualFolderInfo?>(null) }
    var removing by remember { mutableStateOf<VirtualFolderInfo?>(null) }

    // Poll while any library is scanning so progress updates.
    LaunchedEffect(loader.value) {
        if (loader.value?.any { it.refreshStatus == "Active" } == true) {
            delay(3000)
            loader.reload(silent = true)
        }
    }

    AdminScaffold(
        "Libraries", actions, loader.refreshing, { loader.reload(silent = true) },
        topActions = {
            IconButton(onClick = { actions.run("Scan started", after = { loader.reload(silent = true) }) { client.scanAllLibraries() } }) {
                Icon(Icons.Filled.Refresh, "Scan all")
            }
        },
        floating = { ExtendedFloatingActionButton(onClick = { adding = true }, icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Add library") }) },
    ) {
        LoadContent(loader) { libs ->
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(libs, key = { it.itemId ?: it.name.orEmpty() }) { lib ->
                    var menu by remember { mutableStateOf(false) }
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 12.dp, end = 4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Storage, null, tint = MaterialTheme.colorScheme.primary)
                                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                    Text(lib.name.orEmpty(), fontWeight = FontWeight.SemiBold)
                                    Text(
                                        COLLECTION_TYPES.firstOrNull { it.first == lib.collectionType }?.second ?: (lib.collectionType ?: "Mixed"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Box {
                                    IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "More") }
                                    DropdownMenu(menu, onDismissRequest = { menu = false }) {
                                        DropdownMenuItem(text = { Text("Scan this library") }, leadingIcon = { Icon(Icons.Filled.Refresh, null) }, onClick = {
                                            menu = false
                                            lib.itemId?.let { id -> actions.run("Scan started", after = { loader.reload(silent = true) }) { client.refreshItem(id, replaceAll = false) } }
                                        })
                                        DropdownMenuItem(text = { Text("Add folder") }, leadingIcon = { Icon(Icons.Filled.Add, null) }, onClick = {
                                            menu = false; addPathTo = lib.name
                                        })
                                        DropdownMenuItem(text = { Text("Rename") }, leadingIcon = { Icon(Icons.Filled.Edit, null) }, onClick = {
                                            menu = false; renaming = lib
                                        })
                                        DropdownMenuItem(text = { Text("Remove library", color = MaterialTheme.colorScheme.error) }, leadingIcon = { Icon(Icons.Filled.Delete, null) }, onClick = {
                                            menu = false; removing = lib
                                        })
                                    }
                                }
                            }
                            if (lib.refreshStatus == "Active") {
                                LinearProgressIndicator(
                                    progress = { ((lib.refreshProgress ?: 0.0) / 100).toFloat() },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, end = 12.dp),
                                    drawStopIndicator = {},
                                )
                            }
                            lib.locations.forEach { path ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Folder, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(path, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f).padding(start = 8.dp))
                                    if (lib.locations.size > 1) {
                                        IconButton(onClick = {
                                            actions.run("Folder removed", after = { loader.reload(silent = true) }) { client.removeLibraryPath(lib.name.orEmpty(), path) }
                                        }) { Icon(Icons.Filled.Delete, "Remove folder") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (adding) {
        AddLibraryDialog(client, onDismiss = { adding = false }) { name, type, path ->
            adding = false
            actions.run("Library added - scanning", after = { loader.reload(silent = true) }) { client.addVirtualFolder(name, type, listOf(path)) }
        }
    }
    addPathTo?.let { libName ->
        FolderPickerDialog(client, onDismiss = { addPathTo = null }) { path ->
            addPathTo = null
            actions.run("Folder added", after = { loader.reload(silent = true) }) { client.addLibraryPath(libName, path) }
        }
    }
    renaming?.let { lib ->
        var newName by remember(lib.name) { mutableStateOf(lib.name.orEmpty()) }
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text("Rename library") },
            text = { OutlinedTextField(newName, { newName = it }, singleLine = true, label = { Text("Name") }) },
            confirmButton = {
                TextButton(enabled = newName.isNotBlank(), onClick = {
                    renaming = null
                    actions.run("Renamed", after = { loader.reload(silent = true) }) { client.renameVirtualFolder(lib.name.orEmpty(), newName.trim()) }
                }) { Text("Rename") }
            },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text("Cancel") } },
        )
    }
    removing?.let { lib ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text("Remove ${lib.name}?") },
            text = { Text("The library is removed from Jellyfin along with its watch history. Your media files on disk are not touched.") },
            confirmButton = {
                TextButton(onClick = {
                    removing = null
                    actions.run("Library removed", after = { loader.reload(silent = true) }) { client.removeVirtualFolder(lib.name.orEmpty()) }
                }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { removing = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun AddLibraryDialog(client: JellyfinClient, onDismiss: () -> Unit, onCreate: (String, String?, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("movies") }
    var path by remember { mutableStateOf<String?>(null) }
    var picking by remember { mutableStateOf(false) }
    var typeMenu by remember { mutableStateOf(false) }

    if (picking) {
        FolderPickerDialog(client, onDismiss = { picking = false }) { picked ->
            path = picked
            if (name.isBlank()) name = picked.trimEnd('/', '\\').substringAfterLast('/').substringAfterLast('\\')
            picking = false
        }
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add library") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box {
                    OutlinedTextField(
                        value = COLLECTION_TYPES.first { it.first == type }.second,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Content type") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Box(Modifier.matchParentSize().clickable { typeMenu = true })
                    DropdownMenu(typeMenu, onDismissRequest = { typeMenu = false }) {
                        COLLECTION_TYPES.forEach { (k, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { type = k; typeMenu = false })
                        }
                    }
                }
                OutlinedTextField(name, { name = it }, label = { Text("Display name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                ListItem(
                    headlineContent = { Text(path ?: "Choose a folder on the server") },
                    leadingContent = { Icon(Icons.Filled.Folder, null) },
                    modifier = Modifier.clickable { picking = true },
                )
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank() && path != null, onClick = {
                onCreate(name.trim(), if (type == "mixed") null else type, path!!)
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Browses the server's file system (not the phone's) to pick a media folder. */
@Composable
fun FolderPickerDialog(client: JellyfinClient, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    var path by remember { mutableStateOf<String?>(null) }
    var entries by remember { mutableStateOf<List<FileSystemEntry>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var typed by remember { mutableStateOf("") }

    LaunchedEffect(path) {
        if (path == PARENT_MARKER) {
            // Resolve ".." through the server, since paths may be Windows or Unix style.
            path = runCatching { client.parentPath(typed) }.getOrNull()
            return@LaunchedEffect
        }
        loading = true
        error = null
        try {
            entries = path?.let { client.directoryContents(it) } ?: client.drives()
            typed = path.orEmpty()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            error = e.friendlyMessage()
        } finally {
            loading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Server folder") },
        text = {
            Column {
                OutlinedTextField(
                    typed, { typed = it },
                    label = { Text("Path") },
                    singleLine = true,
                    trailingIcon = { TextButton(onClick = { path = typed.ifBlank { null } }) { Text("Go") } },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (loading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 8.dp))
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    if (path != null) {
                        item {
                            var parentLoading by remember { mutableStateOf(false) }
                            ListItem(
                                headlineContent = { Text("..") },
                                leadingContent = { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) },
                                modifier = Modifier.clickable(enabled = !parentLoading) { parentLoading = true; path = PARENT_MARKER },
                            )
                        }
                    }
                    items(entries, key = { it.path }) { e ->
                        ListItem(
                            headlineContent = { Text(e.name ?: e.path) },
                            leadingContent = { Icon(Icons.Filled.Folder, null) },
                            modifier = Modifier.clickable { path = e.path },
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(enabled = path != null && path != PARENT_MARKER, onClick = { path?.let(onPick) }) { Text("Use this folder") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private const val PARENT_MARKER = "\u0000parent"

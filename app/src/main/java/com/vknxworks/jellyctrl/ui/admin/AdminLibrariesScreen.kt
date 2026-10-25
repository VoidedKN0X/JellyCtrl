package com.vknxworks.jellyctrl.ui.admin

import androidx.compose.material3.CardDefaults
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Storage
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
import androidx.compose.ui.res.stringResource
import com.vknxworks.jellyctrl.R
import com.vknxworks.jellyctrl.str
import com.vknxworks.jellyctrl.ui.components.CtrlAlertDialog
import com.vknxworks.jellyctrl.ui.components.CtrlTextField
import com.vknxworks.jellyctrl.ui.components.CtrlListItem
import com.vknxworks.jellyctrl.ui.components.CtrlProgressBar


private val COLLECTION_TYPES: List<Pair<String, String>>
    get() = listOf(
        "movies" to str(R.string.kind_movies), "tvshows" to str(R.string.kind_shows), "music" to str(R.string.kind_music),
        "musicvideos" to str(R.string.kind_music_videos), "homevideos" to str(R.string.kind_home_videos), "boxsets" to str(R.string.kind_collections),
        "books" to str(R.string.kind_books), "mixed" to str(R.string.kind_mixed),
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
    var removingPath by remember { mutableStateOf<Pair<String, String>?>(null) }

    // Poll while any library is scanning so progress updates.
    LaunchedEffect(loader.value) {
        if (loader.value?.any { it.refreshStatus == "Active" } == true) {
            delay(3000)
            loader.reload(silent = true)
        }
    }

    AdminScaffold(
        stringResource(R.string.libraries), actions, loader.refreshing, { loader.reload(silent = true) },
        topActions = {
            IconButton(onClick = { actions.run(str(R.string.scan_started), after = { loader.reload(silent = true) }) { client.scanAllLibraries() } }) {
                Icon(Icons.Outlined.Refresh, stringResource(R.string.scan_all))
            }
        },
        floating = { ExtendedFloatingActionButton(shape = com.vknxworks.jellyctrl.ui.theme.CtrlShapes.MainButton, containerColor = com.vknxworks.jellyctrl.ui.theme.CtrlColors.Accent, contentColor = com.vknxworks.jellyctrl.ui.theme.CtrlColors.OnAccent, elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp), onClick = { adding = true }, icon = { Icon(Icons.Outlined.Add, null) }, text = { Text(stringResource(R.string.add_library)) }) },
    ) {
        LoadContent(loader) { libs ->
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(libs, key = { it.itemId ?: it.name.orEmpty() }) { lib ->
                    var menu by remember { mutableStateOf(false) }
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = com.vknxworks.jellyctrl.ui.theme.CtrlShapes.Card,
                        colors = CardDefaults.cardColors(containerColor = com.vknxworks.jellyctrl.ui.theme.CtrlColors.Surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, com.vknxworks.jellyctrl.ui.theme.CtrlColors.Outline),
                    ) {
                        Column(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 12.dp, end = 4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Storage, null, tint = MaterialTheme.colorScheme.primary)
                                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                    Text(lib.name.orEmpty(), fontWeight = FontWeight.SemiBold)
                                    Text(
                                        COLLECTION_TYPES.firstOrNull { it.first == lib.collectionType }?.second ?: (lib.collectionType ?: stringResource(R.string.mixed)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Box {
                                    IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, stringResource(R.string.more)) }
                                    DropdownMenu(menu, onDismissRequest = { menu = false }) {
                                        DropdownMenuItem(text = { Text(stringResource(R.string.scan_this_library)) }, leadingIcon = { Icon(Icons.Outlined.Refresh, null) }, onClick = {
                                            menu = false
                                            lib.itemId?.let { id -> actions.run(str(R.string.scan_started), after = { loader.reload(silent = true) }) { client.refreshItem(id, replaceAll = false) } }
                                        })
                                        DropdownMenuItem(text = { Text(stringResource(R.string.add_folder)) }, leadingIcon = { Icon(Icons.Outlined.Add, null) }, onClick = {
                                            menu = false; addPathTo = lib.name
                                        })
                                        DropdownMenuItem(text = { Text(stringResource(R.string.rename)) }, leadingIcon = { Icon(Icons.Outlined.Edit, null) }, onClick = {
                                            menu = false; renaming = lib
                                        })
                                        DropdownMenuItem(text = { Text(stringResource(R.string.remove_library), color = MaterialTheme.colorScheme.error) }, leadingIcon = { Icon(Icons.Outlined.Delete, null) }, onClick = {
                                            menu = false; removing = lib
                                        })
                                    }
                                }
                            }
                            if (lib.refreshStatus == "Active") {
                                CtrlProgressBar(
                                    progress = { ((lib.refreshProgress ?: 0.0) / 100).toFloat() },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, end = 12.dp),
                                    drawStopIndicator = {},
                                )
                            }
                            lib.locations.forEach { path ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Folder, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(path, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f).padding(start = 8.dp))
                                    if (lib.locations.size > 1) {
                                        IconButton(onClick = { removingPath = lib.name.orEmpty() to path }) { Icon(Icons.Outlined.Delete, stringResource(R.string.remove_folder)) }
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
            actions.run(str(R.string.library_added_scanning), after = { loader.reload(silent = true) }) { client.addVirtualFolder(name, type, listOf(path)) }
        }
    }
    addPathTo?.let { libName ->
        FolderPickerDialog(client, onDismiss = { addPathTo = null }) { path ->
            addPathTo = null
            actions.run(str(R.string.folder_added), after = { loader.reload(silent = true) }) { client.addLibraryPath(libName, path) }
        }
    }
    renaming?.let { lib ->
        var newName by remember(lib.name) { mutableStateOf(lib.name.orEmpty()) }
        CtrlAlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text(stringResource(R.string.rename_library)) },
            text = { CtrlTextField(newName, { newName = it }, singleLine = true, label = { Text(stringResource(R.string.name)) }) },
            confirmButton = {
                TextButton(enabled = newName.isNotBlank(), onClick = {
                    renaming = null
                    actions.run(str(R.string.renamed), after = { loader.reload(silent = true) }) { client.renameVirtualFolder(lib.name.orEmpty(), newName.trim()) }
                }) { Text(stringResource(R.string.rename)) }
            },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    removingPath?.let { (libName, path) ->
        CtrlAlertDialog(
            onDismissRequest = { removingPath = null },
            title = { Text(stringResource(R.string.remove_folder_2)) },
            text = { Text(stringResource(R.string.x_will_no_longer_be_part, path, libName)) },
            confirmButton = {
                TextButton(onClick = {
                    removingPath = null
                    actions.run(str(R.string.folder_removed), after = { loader.reload(silent = true) }) { client.removeLibraryPath(libName, path) }
                }) { Text(stringResource(R.string.remove), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { removingPath = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    removing?.let { lib ->
        CtrlAlertDialog(
            onDismissRequest = { removing = null },
            title = { Text(stringResource(R.string.remove_x, lib.name.orEmpty())) },
            text = { Text(stringResource(R.string.the_library_is_removed_from_jellyfin)) },
            confirmButton = {
                TextButton(onClick = {
                    removing = null
                    actions.run(str(R.string.library_removed), after = { loader.reload(silent = true) }) { client.removeVirtualFolder(lib.name.orEmpty()) }
                }) { Text(stringResource(R.string.remove), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { removing = null }) { Text(stringResource(R.string.cancel)) } },
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

    CtrlAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_library)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box {
                    CtrlTextField(
                        value = COLLECTION_TYPES.first { it.first == type }.second,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.content_type)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Box(Modifier.matchParentSize().clickable { typeMenu = true })
                    DropdownMenu(typeMenu, onDismissRequest = { typeMenu = false }) {
                        COLLECTION_TYPES.forEach { (k, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { type = k; typeMenu = false })
                        }
                    }
                }
                CtrlTextField(name, { name = it }, label = { Text(stringResource(R.string.display_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                CtrlListItem(
                    headlineContent = { Text(path ?: stringResource(R.string.choose_a_folder_on_the_server)) },
                    leadingContent = { Icon(Icons.Outlined.Folder, null) },
                    modifier = Modifier.clickable { picking = true },
                )
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank() && path != null, onClick = {
                onCreate(name.trim(), if (type == "mixed") null else type, path!!)
            }) { Text(stringResource(R.string.add)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
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

    CtrlAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.server_folder)) },
        text = {
            Column {
                CtrlTextField(
                    typed, { typed = it },
                    label = { Text(stringResource(R.string.path)) },
                    singleLine = true,
                    trailingIcon = { TextButton(onClick = { path = typed.ifBlank { null } }) { Text(stringResource(R.string.go)) } },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (loading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 8.dp), color = com.vknxworks.jellyctrl.ui.theme.CtrlColors.Accent, trackColor = com.vknxworks.jellyctrl.ui.theme.CtrlColors.ProgressTrack)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    if (path != null) {
                        item {
                            var parentLoading by remember { mutableStateOf(false) }
                            CtrlListItem(
                                headlineContent = { Text("..") },
                                leadingContent = { Icon(Icons.AutoMirrored.Outlined.ArrowBack, null) },
                                modifier = Modifier.clickable(enabled = !parentLoading) { parentLoading = true; path = PARENT_MARKER },
                            )
                        }
                    }
                    items(entries, key = { it.path }) { e ->
                        CtrlListItem(
                            headlineContent = { Text(e.name ?: e.path) },
                            leadingContent = { Icon(Icons.Outlined.Folder, null) },
                            modifier = Modifier.clickable { path = e.path },
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(enabled = path != null && path != PARENT_MARKER, onClick = { path?.let(onPick) }) { Text(stringResource(R.string.use_this_folder)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

private const val PARENT_MARKER = "\u0000parent"

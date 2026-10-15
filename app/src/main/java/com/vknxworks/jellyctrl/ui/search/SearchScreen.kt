package com.vknxworks.jellyctrl.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.vknxworks.jellyctrl.container
import com.vknxworks.jellyctrl.data.api.BaseItemDto
import com.vknxworks.jellyctrl.data.api.ItemQuery
import com.vknxworks.jellyctrl.data.api.items
import com.vknxworks.jellyctrl.ui.components.ItemActionsSheet
import com.vknxworks.jellyctrl.ui.components.ItemRow
import com.vknxworks.jellyctrl.ui.components.LoadingView
import com.vknxworks.jellyctrl.ui.components.MessageView
import com.vknxworks.jellyctrl.ui.components.friendlyMessage
import com.vknxworks.jellyctrl.ui.nav.LocalNavigator
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

private val GROUPS = listOf(
    "Movies" to listOf("Movie"),
    "Shows" to listOf("Series"),
    "Episodes" to listOf("Episode"),
    "Collections" to listOf("BoxSet"),
    "Albums" to listOf("MusicAlbum"),
    "Artists" to listOf("MusicArtist"),
    "Songs" to listOf("Audio"),
    "Music videos" to listOf("MusicVideo"),
    "Videos" to listOf("Video"),
    "Playlists" to listOf("Playlist"),
    "Channels" to listOf("TvChannel"),
    "Programs" to listOf("Program"),
    "Books" to listOf("Book", "AudioBook"),
    "People" to listOf("Person"),
)

@Composable
fun SearchScreen() {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val client = c.api
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Pair<String, List<BaseItemDto>>>?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var menuItem by remember { mutableStateOf<BaseItemDto?>(null) }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) { if (query.isEmpty()) focus.requestFocus() }

    LaunchedEffect(query) {
        val q = query.trim()
        if (q.length < 2) {
            results = null
            return@LaunchedEffect
        }
        delay(350) // debounce typing
        loading = true
        error = null
        try {
            results = coroutineScope {
                GROUPS.map { (label, types) ->
                    async {
                        label to runCatching {
                            client.items(ItemQuery(searchTerm = q, includeItemTypes = types, recursive = true), limit = 24).items
                        }.getOrDefault(emptyList())
                    }
                }.map { it.await() }.filter { it.second.isNotEmpty() }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            error = e.friendlyMessage()
        } finally {
            loading = false
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search movies, shows, music, people…") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Clear, "Clear") }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
            modifier = Modifier.fillMaxWidth().padding(16.dp).focusRequester(focus),
        )
        val r = results
        when {
            loading && r == null -> LoadingView()
            error != null -> MessageView(Icons.Outlined.SearchOff, "Search failed", error)
            r == null -> MessageView(Icons.Filled.Search, "Search your library", "Type at least two letters")
            r.isEmpty() -> MessageView(Icons.Outlined.SearchOff, "No results", "Nothing matches \"${query.trim()}\"")
            else -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                items(r, key = { it.first }) { (label, items) ->
                    ItemRow(label, items, client, nav::openItem, landscape = label == "Episodes" || label == "Videos", onLongItem = { menuItem = it })
                }
            }
        }
    }
    menuItem?.let { ItemActionsSheet(it, onDismiss = { menuItem = null }) }
}

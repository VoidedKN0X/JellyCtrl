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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import com.vknxworks.jellyctrl.ui.components.LocalBottomClearance
import com.vknxworks.jellyctrl.ui.components.TabTitle
import com.vknxworks.jellyctrl.ui.theme.Gutter
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
import androidx.compose.ui.res.stringResource
import com.vknxworks.jellyctrl.R
import com.vknxworks.jellyctrl.str

private val GROUPS: List<Pair<String, List<String>>>
    get() = listOf(
        str(R.string.kind_movies) to listOf("Movie"),
        str(R.string.kind_shows) to listOf("Series"),
        str(R.string.kind_episodes) to listOf("Episode"),
        str(R.string.kind_collections) to listOf("BoxSet"),
        str(R.string.kind_albums) to listOf("MusicAlbum"),
        str(R.string.kind_artists) to listOf("MusicArtist"),
        str(R.string.kind_songs) to listOf("Audio"),
        str(R.string.kind_music_videos) to listOf("MusicVideo"),
        str(R.string.kind_videos) to listOf("Video"),
        str(R.string.kind_playlists) to listOf("Playlist"),
        str(R.string.kind_channels) to listOf("TvChannel"),
        str(R.string.kind_programs) to listOf("Program"),
        str(R.string.kind_books) to listOf("Book", "AudioBook"),
        str(R.string.kind_people) to listOf("Person"),
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
        TabTitle(stringResource(R.string.search))
        TextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(stringResource(R.string.films_shows_music_people)) },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Clear, stringResource(R.string.clear_search)) }
            },
            singleLine = true,
            shape = CircleShape,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
            modifier = Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 4.dp).focusRequester(focus),
        )
        val r = results
        when {
            loading && r == null -> LoadingView()
            error != null -> MessageView(Icons.Outlined.SearchOff, stringResource(R.string.search_didn_t_work), error)
            r == null -> MessageView(Icons.Filled.Search, stringResource(R.string.find_anything_on_your_server), stringResource(R.string.start_typing_a_title_actor_artist))
            r.isEmpty() -> MessageView(Icons.Outlined.SearchOff, stringResource(R.string.no_matches), stringResource(R.string.nothing_on_your_server_matches_x, query.trim()))
            else -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp + LocalBottomClearance.current)) {
                items(r, key = { it.first }) { (label, items) ->
                    ItemRow(label, items, client, nav::openItem, landscape = items.firstOrNull()?.type in setOf("Episode", "Video"), onLongItem = { menuItem = it })
                }
            }
        }
    }
    menuItem?.let { ItemActionsSheet(it, onDismiss = { menuItem = null }) }
}

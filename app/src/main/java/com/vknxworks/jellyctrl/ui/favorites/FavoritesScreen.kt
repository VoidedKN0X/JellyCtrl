package com.vknxworks.jellyctrl.ui.favorites

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.vknxworks.jellyctrl.container
import com.vknxworks.jellyctrl.data.api.BaseItemDto
import com.vknxworks.jellyctrl.data.api.ItemQuery
import com.vknxworks.jellyctrl.data.api.items
import com.vknxworks.jellyctrl.ui.components.ItemActionsSheet
import com.vknxworks.jellyctrl.ui.components.ItemRow
import com.vknxworks.jellyctrl.ui.components.LoadContent
import com.vknxworks.jellyctrl.ui.components.MessageView
import com.vknxworks.jellyctrl.ui.components.rememberLoader
import com.vknxworks.jellyctrl.ui.nav.LocalNavigator

private val FAV_GROUPS = listOf(
    "Movies" to listOf("Movie"),
    "Shows" to listOf("Series"),
    "Episodes" to listOf("Episode"),
    "Collections" to listOf("BoxSet"),
    "Albums" to listOf("MusicAlbum"),
    "Artists" to listOf("MusicArtist"),
    "Songs" to listOf("Audio"),
    "Videos" to listOf("Video", "MusicVideo"),
    "Playlists" to listOf("Playlist"),
    "Channels" to listOf("TvChannel"),
    "People" to listOf("Person"),
    "Books" to listOf("Book", "AudioBook"),
    "Photos" to listOf("Photo", "PhotoAlbum"),
)

@Composable
fun FavoritesScreen() {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val client = c.api
    var menuItem by remember { mutableStateOf<BaseItemDto?>(null) }
    val loader = rememberLoader("favorites") {
        FAV_GROUPS.map { (label, types) ->
            label to runCatching {
                client.items(ItemQuery(isFavorite = true, includeItemTypes = types, recursive = true), limit = 60).items
            }.getOrDefault(emptyList())
        }.filter { it.second.isNotEmpty() }
    }
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { TopAppBar(title = { Text("Favorites") }) },
    ) { padding ->
        PullToRefreshBox(loader.refreshing, onRefresh = { loader.reload(silent = true) }, modifier = Modifier.padding(padding).fillMaxSize()) {
            LoadContent(loader) { groups ->
                if (groups.isEmpty()) {
                    MessageView(Icons.Outlined.FavoriteBorder, "No favorites yet", "Tap the heart on anything to keep it here.")
                } else {
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                        items(groups, key = { it.first }) { (label, items) ->
                            ItemRow(label, items, client, nav::openItem, landscape = label == "Episodes", onLongItem = { menuItem = it })
                        }
                    }
                }
            }
        }
    }
    menuItem?.let { ItemActionsSheet(it, onDismiss = { menuItem = null }, onChanged = { loader.reload(silent = true) }) }
}

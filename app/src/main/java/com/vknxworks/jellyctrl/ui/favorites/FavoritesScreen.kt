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
import com.vknxworks.jellyctrl.ui.components.ScreenTopBar
import com.vknxworks.jellyctrl.ui.components.rememberLoader
import com.vknxworks.jellyctrl.ui.nav.LocalNavigator
import androidx.compose.ui.res.stringResource
import com.vknxworks.jellyctrl.R
import com.vknxworks.jellyctrl.str

private val FAV_GROUPS: List<Pair<String, List<String>>>
    get() = listOf(
        str(R.string.kind_movies) to listOf("Movie"),
        str(R.string.kind_shows) to listOf("Series"),
        str(R.string.kind_episodes) to listOf("Episode"),
        str(R.string.kind_collections) to listOf("BoxSet"),
        str(R.string.kind_albums) to listOf("MusicAlbum"),
        str(R.string.kind_artists) to listOf("MusicArtist"),
        str(R.string.kind_songs) to listOf("Audio"),
        str(R.string.kind_videos) to listOf("Video", "MusicVideo"),
        str(R.string.kind_playlists) to listOf("Playlist"),
        str(R.string.kind_channels) to listOf("TvChannel"),
        str(R.string.kind_people) to listOf("Person"),
        str(R.string.kind_books) to listOf("Book", "AudioBook"),
        str(R.string.kind_photos) to listOf("Photo", "PhotoAlbum"),
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
        topBar = { ScreenTopBar(stringResource(R.string.favourites)) },
    ) { padding ->
        PullToRefreshBox(loader.refreshing, onRefresh = { loader.reload(silent = true) }, modifier = Modifier.padding(padding).fillMaxSize()) {
            LoadContent(loader) { groups ->
                if (groups.isEmpty()) {
                    MessageView(Icons.Outlined.FavoriteBorder, stringResource(R.string.no_favourites_yet), stringResource(R.string.tap_the_heart_on_any_film))
                } else {
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                        items(groups, key = { it.first }) { (label, items) ->
                            ItemRow(label, items, client, nav::openItem, landscape = items.firstOrNull()?.type == "Episode", onLongItem = { menuItem = it })
                        }
                    }
                }
            }
        }
    }
    menuItem?.let { ItemActionsSheet(it, onDismiss = { menuItem = null }, onChanged = { loader.reload(silent = true) }) }
}

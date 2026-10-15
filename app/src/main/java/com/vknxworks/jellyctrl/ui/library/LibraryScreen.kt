package com.vknxworks.jellyctrl.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.vknxworks.jellyctrl.container
import com.vknxworks.jellyctrl.data.api.BaseItemDto
import com.vknxworks.jellyctrl.data.api.ItemQuery
import com.vknxworks.jellyctrl.data.api.albumArtists
import com.vknxworks.jellyctrl.data.api.genres
import com.vknxworks.jellyctrl.data.api.items
import com.vknxworks.jellyctrl.ui.components.ItemActionsSheet
import com.vknxworks.jellyctrl.ui.components.LoadContent
import com.vknxworks.jellyctrl.ui.components.PagedItemGrid
import com.vknxworks.jellyctrl.ui.components.rememberLoader
import com.vknxworks.jellyctrl.ui.components.rememberPagedLoader
import com.vknxworks.jellyctrl.ui.livetv.LiveTvScreen
import com.vknxworks.jellyctrl.ui.nav.LibraryRoute
import com.vknxworks.jellyctrl.ui.nav.ListRoute
import com.vknxworks.jellyctrl.ui.nav.LocalNavigator

enum class SortOption(val label: String, val fields: List<String>, val defaultDescending: Boolean = false) {
    NAME("Name", listOf("SortName")),
    ADDED("Date added", listOf("DateCreated", "SortName"), true),
    RELEASED("Release date", listOf("PremiereDate", "ProductionYear", "SortName"), true),
    RATING("Community rating", listOf("CommunityRating", "SortName"), true),
    CRITIC("Critic rating", listOf("CriticRating", "SortName"), true),
    PLAYED("Date played", listOf("DatePlayed", "SortName"), true),
    RUNTIME("Runtime", listOf("Runtime", "SortName")),
    PLAYCOUNT("Play count", listOf("PlayCount", "SortName"), true),
    RANDOM("Random", listOf("Random")),
}

enum class FilterOption(val label: String, val value: String) {
    UNPLAYED("Unplayed", "IsUnplayed"),
    PLAYED("Played", "IsPlayed"),
    FAVORITES("Favorites", "IsFavorite"),
    RESUMABLE("In progress", "IsResumable"),
}

private data class LibTab(val label: String, val types: List<String>, val kind: String = "items", val recursive: Boolean = true)

private fun tabsFor(collectionType: String?): List<LibTab> = when (collectionType) {
    "movies" -> listOf(LibTab("Movies", listOf("Movie")), LibTab("Genres", listOf("Movie"), "genres"))
    "tvshows" -> listOf(LibTab("Shows", listOf("Series")), LibTab("Episodes", listOf("Episode")), LibTab("Genres", listOf("Series"), "genres"))
    "music" -> listOf(
        LibTab("Albums", listOf("MusicAlbum")),
        LibTab("Album artists", emptyList(), "artists"),
        LibTab("Songs", listOf("Audio")),
        LibTab("Playlists", listOf("Playlist"), "playlists"),
        LibTab("Genres", listOf("MusicAlbum"), "genres"),
    )
    "musicvideos" -> listOf(LibTab("Videos", listOf("MusicVideo")))
    "boxsets" -> listOf(LibTab("Collections", listOf("BoxSet")))
    "playlists" -> listOf(LibTab("Playlists", listOf("Playlist"), "playlists"))
    "books" -> listOf(LibTab("Books", listOf("Book", "AudioBook")))
    "photos", "homevideos", "folders" -> listOf(LibTab("Folders", emptyList(), recursive = false))
    else -> listOf(LibTab("All", emptyList(), recursive = false))
}

@Composable
fun LibraryScreen(route: LibraryRoute) {
    if (route.collectionType == "livetv") {
        LiveTvScreen()
        return
    }
    val nav = LocalNavigator.current
    val tabs = remember(route.collectionType) { tabsFor(route.collectionType) }
    var tabIndex by rememberSaveable { mutableStateOf(0) }
    val tab = tabs[tabIndex.coerceIn(0, tabs.lastIndex)]
    var sort by rememberSaveable { mutableStateOf(SortOption.NAME) }
    var descending by rememberSaveable { mutableStateOf(false) }
    var filters by rememberSaveable { mutableStateOf(setOf<FilterOption>()) }
    var showSort by remember { mutableStateOf(false) }
    var showFilter by remember { mutableStateOf(false) }
    val canSortFilter = tab.kind == "items" || tab.kind == "playlists"

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(route.name) },
                    navigationIcon = { IconButton(onClick = nav::back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                    actions = {
                        if (canSortFilter) {
                            IconButton(onClick = {
                                nav.shuffle(BaseItemDto(id = route.id, type = "CollectionFolder", collectionType = route.collectionType, isFolder = true))
                            }) { Icon(Icons.Filled.Shuffle, "Shuffle") }
                            Box {
                                IconButton(onClick = { showSort = true }) { Icon(Icons.AutoMirrored.Filled.Sort, "Sort") }
                                DropdownMenu(showSort, onDismissRequest = { showSort = false }) {
                                    SortOption.entries.forEach { opt ->
                                        DropdownMenuItem(
                                            text = { Text(opt.label) },
                                            leadingIcon = { if (opt == sort) Icon(Icons.Filled.Check, null) },
                                            onClick = {
                                                if (sort == opt) descending = !descending else {
                                                    sort = opt
                                                    descending = opt.defaultDescending
                                                }
                                                showSort = false
                                            },
                                        )
                                    }
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text(if (descending) "Descending" else "Ascending") },
                                        onClick = { descending = !descending; showSort = false },
                                    )
                                }
                            }
                            IconButton(onClick = { showFilter = !showFilter }) { Icon(Icons.Filled.FilterList, "Filter") }
                        }
                    },
                )
                if (tabs.size > 1) {
                    PrimaryScrollableTabRow(selectedTabIndex = tabIndex, edgePadding = 8.dp) {
                        tabs.forEachIndexed { i, t ->
                            Tab(selected = i == tabIndex, onClick = { tabIndex = i }, text = { Text(t.label) })
                        }
                    }
                }
                if (showFilter && canSortFilter) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(FilterOption.entries) { f ->
                            FilterChip(
                                selected = f in filters,
                                onClick = { filters = if (f in filters) filters - f else filters + f },
                                label = { Text(f.label) },
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            when (tab.kind) {
                "genres" -> GenreList(route.id, tab.types)
                "artists" -> ArtistsGrid(route.id)
                else -> LibraryGrid(route.id, tab, sort, descending, filters)
            }
        }
    }
}

@Composable
private fun LibraryGrid(parentId: String, tab: LibTab, sort: SortOption, descending: Boolean, filters: Set<FilterOption>) {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val client = c.api
    var menuItem by remember { mutableStateOf<BaseItemDto?>(null) }
    val key = "lib-$parentId-${tab.label}-${sort.name}-$descending-${filters.joinToString { it.name }}"
    val loader = rememberPagedLoader<BaseItemDto>(key) { start, limit ->
        val res = client.items(
            ItemQuery(
                parentId = parentId,
                includeItemTypes = tab.types,
                recursive = tab.recursive,
                sortBy = if (!tab.recursive && sort == SortOption.NAME) listOf("IsFolder", "SortName") else sort.fields,
                sortOrder = if (descending) "Descending" else "Ascending",
                filters = filters.map { it.value },
            ),
            start, limit,
        )
        res.items to res.totalRecordCount
    }
    PagedItemGrid(
        loader, client,
        onItem = { nav.openItem(it) },
        onLongItem = { menuItem = it },
        emptyText = if (filters.isEmpty()) "This library is empty" else "Nothing matches these filters",
    )
    menuItem?.let { ItemActionsSheet(it, onDismiss = { menuItem = null }) }
}

@Composable
private fun ArtistsGrid(parentId: String) {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val client = c.api
    val loader = rememberPagedLoader<BaseItemDto>("artists-$parentId") { start, limit ->
        val res = client.albumArtists(parentId, start, limit)
        res.items to res.totalRecordCount
    }
    PagedItemGrid(loader, client, onItem = { nav.openItem(it) })
}

@Composable
private fun GenreList(parentId: String, types: List<String>) {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val loader = rememberLoader("genres-$parentId-$types") { c.api.genres(parentId, types) }
    LoadContent(loader) { genres ->
        LazyColumn(Modifier.fillMaxSize()) {
            items(genres, key = { it.id }) { g ->
                ListItem(
                    headlineContent = { Text(g.name.orEmpty()) },
                    modifier = Modifier.fillMaxWidth().clickable {
                        nav.navigate(ListRoute(g.name.orEmpty(), "genre", g.name, parentId, types.joinToString(",")))
                    },
                )
            }
        }
    }
}

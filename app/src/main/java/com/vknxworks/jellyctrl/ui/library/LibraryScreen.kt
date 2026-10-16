package com.vknxworks.jellyctrl.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.clip
import com.vknxworks.jellyctrl.ui.components.ChoiceSheet
import com.vknxworks.jellyctrl.ui.components.ScreenTopBar
import com.vknxworks.jellyctrl.ui.theme.Gutter
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

enum class SortOption(val label: String, val fields: List<String>, val ascLabel: String, val descLabel: String) {
    NAME("Name", listOf("SortName"), "A to Z", "Z to A"),
    ADDED("Date added", listOf("DateCreated", "SortName"), "oldest first", "newest first"),
    RELEASED("Release date", listOf("PremiereDate", "ProductionYear", "SortName"), "oldest first", "newest first"),
    RATING("Rating", listOf("CommunityRating", "SortName"), "lowest first", "highest first"),
    CRITIC("Critic rating", listOf("CriticRating", "SortName"), "lowest first", "highest first"),
    PLAYED("Last watched", listOf("DatePlayed", "SortName"), "longest ago", "most recent"),
    RUNTIME("Length", listOf("Runtime", "SortName"), "shortest first", "longest first"),
    PLAYCOUNT("Times played", listOf("PlayCount", "SortName"), "fewest first", "most first"),
    RANDOM("Random", listOf("Random"), "", ""),
}

enum class FilterOption(val label: String, val value: String) {
    UNPLAYED("Unwatched", "IsUnplayed"),
    PLAYED("Watched", "IsPlayed"),
    FAVORITES("Favourites", "IsFavorite"),
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
    val canSortFilter = tab.kind == "items" || tab.kind == "playlists"

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(Modifier.background(MaterialTheme.colorScheme.background)) {
                ScreenTopBar(route.name) {
                    if (canSortFilter) {
                        IconButton(onClick = {
                            nav.shuffle(BaseItemDto(id = route.id, type = "CollectionFolder", collectionType = route.collectionType, isFolder = true))
                        }) { Icon(Icons.Filled.Shuffle, "Shuffle") }
                    }
                }
                if (tabs.size > 1) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = Gutter),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(tabs.size) { i ->
                            val selected = i == tabIndex
                            Text(
                                tabs[i].label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .clickable { tabIndex = i }
                                    .padding(horizontal = 16.dp, vertical = 9.dp),
                            )
                        }
                    }
                }
                if (canSortFilter) {
                    LazyRow(
                        contentPadding = PaddingValues(start = Gutter, end = Gutter, top = 10.dp, bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item {
                            FilterChip(
                                selected = sort != SortOption.NAME || descending,
                                onClick = { showSort = true },
                                label = { Text(sort.label) },
                                leadingIcon = {
                                    Icon(if (descending) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward, null, Modifier.size(16.dp))
                                },
                                shape = CircleShape,
                            )
                        }
                        items(FilterOption.entries) { f ->
                            FilterChip(
                                selected = f in filters,
                                onClick = { filters = if (f in filters) filters - f else filters + f },
                                label = { Text(f.label) },
                                shape = CircleShape,
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

    if (showSort) {
        val options = SortOption.entries.flatMap { o ->
            if (o == SortOption.RANDOM) listOf((o to false) to o.label)
            else listOf((o to false) to "${o.label}, ${o.ascLabel}", (o to true) to "${o.label}, ${o.descLabel}")
        }
        ChoiceSheet("Sort by", options, sort to descending, onDismiss = { showSort = false }) { (o, d) ->
            sort = o
            descending = d
            showSort = false
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

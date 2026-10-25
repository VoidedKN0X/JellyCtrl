package com.vknxworks.jellyctrl.ui.library

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.vknxworks.jellyctrl.container
import com.vknxworks.jellyctrl.data.api.BaseItemDto
import com.vknxworks.jellyctrl.data.api.ItemQuery
import com.vknxworks.jellyctrl.data.api.items
import com.vknxworks.jellyctrl.data.api.nextUp
import com.vknxworks.jellyctrl.data.api.resumeItems
import com.vknxworks.jellyctrl.ui.components.ItemActionsSheet
import com.vknxworks.jellyctrl.ui.components.PagedItemGrid
import com.vknxworks.jellyctrl.ui.components.ScreenTopBar
import com.vknxworks.jellyctrl.ui.components.rememberPagedLoader
import com.vknxworks.jellyctrl.ui.nav.ListRoute
import com.vknxworks.jellyctrl.ui.nav.LocalNavigator

@Composable
fun ListScreen(route: ListRoute) {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val client = c.api
    var menuItem by remember { mutableStateOf<BaseItemDto?>(null) }
    val types = route.itemTypes?.split(',')?.filter { it.isNotBlank() }.orEmpty()

    val loader = rememberPagedLoader<BaseItemDto>("list-${route.kind}-${route.param}-${route.parentId}-${route.itemTypes}") { start, limit ->
        when (route.kind) {
            "resume" -> if (start == 0) client.resumeItems("Video", 200).let { it to it.size } else emptyList<BaseItemDto>() to start
            "nextup" -> if (start == 0) client.nextUp(200).let { it to it.size } else emptyList<BaseItemDto>() to start
            else -> {
                val base = ItemQuery(
                    parentId = route.parentId,
                    includeItemTypes = types.ifEmpty { listOf("Movie", "Series", "MusicAlbum", "MusicVideo", "Video", "BoxSet") },
                    recursive = true,
                )
                val q = when (route.kind) {
                    "genre" -> base.copy(genres = listOfNotNull(route.param))
                    "studio" -> base.copy(studioIds = listOfNotNull(route.param))
                    "person" -> base.copy(personIds = listOfNotNull(route.param), includeItemTypes = listOf("Movie", "Series", "Episode", "MusicVideo"))
                    "year" -> base.copy(years = listOfNotNull(route.param?.toIntOrNull()))
                    "artist" -> base.copy(artistIds = listOfNotNull(route.param), includeItemTypes = listOf("MusicAlbum"))
                    else -> base
                }
                val res = client.items(q, start, limit)
                res.items to res.totalRecordCount
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { ScreenTopBar(route.title) },
    ) { padding ->
        PagedItemGrid(
            loader, client,
            onItem = { nav.openItem(it) },
            onLongItem = { menuItem = it },
            modifier = Modifier.padding(padding).fillMaxSize(),
        )
    }
    menuItem?.let { ItemActionsSheet(it, onDismiss = { menuItem = null }, onChanged = { loader.refresh() }) }
}

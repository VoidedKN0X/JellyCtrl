package com.vknxworks.jellyctrl.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.jellyctrl.ui.components.LocalBottomClearance
import com.vknxworks.jellyctrl.container
import com.vknxworks.jellyctrl.data.api.userViews
import com.vknxworks.jellyctrl.data.download.DownloadStatus
import com.vknxworks.jellyctrl.ui.components.ArtImage
import com.vknxworks.jellyctrl.ui.components.Group
import com.vknxworks.jellyctrl.ui.components.GroupRow
import com.vknxworks.jellyctrl.ui.components.LoadContent
import com.vknxworks.jellyctrl.ui.components.MessageView
import com.vknxworks.jellyctrl.ui.components.TabTitle
import com.vknxworks.jellyctrl.ui.components.rememberLoader
import com.vknxworks.jellyctrl.ui.nav.DownloadsRoute
import com.vknxworks.jellyctrl.ui.nav.FavoritesRoute
import com.vknxworks.jellyctrl.ui.nav.LocalNavigator
import com.vknxworks.jellyctrl.ui.theme.Gutter

/** The Library tab: every library as a tile, plus favourites and downloads. */
@Composable
fun LibrariesScreen() {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val client = c.api
    val loader = rememberLoader("libraries") { client.userViews() }
    val downloads by c.downloads.entries.collectAsStateWithLifecycle()
    val ready = downloads.count { it.status == DownloadStatus.DONE }

    PullToRefreshBox(loader.refreshing, onRefresh = { loader.reload(silent = true) }, modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        LoadContent(loader) { views ->
            LazyVerticalGrid(
                columns = GridCells.Adaptive(300.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = Gutter, end = Gutter, bottom = 32.dp + LocalBottomClearance.current),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) { TabTitle("Library", padded = false) }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Group(padded = false) {
                            GroupRow("Favourites", icon = Icons.Filled.Favorite, showChevron = true, onClick = { nav.navigate(FavoritesRoute) })
                            GroupRow(
                                "Downloads",
                                if (ready == 0) "Nothing saved for offline yet" else if (ready == 1) "1 ready to watch offline" else "$ready ready to watch offline",
                                icon = Icons.Filled.Download,
                                showChevron = true,
                                onClick = { nav.navigate(DownloadsRoute) },
                            )
                    }
                }
                if (views.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        MessageView(Icons.Outlined.VideoLibrary, "No libraries yet", "Libraries you have access to will show up here.")
                    }
                }
                items(views, key = { it.id }) { v ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { nav.openItem(v) },
                    ) {
                        ArtImage(
                            v.imageTags["Primary"]?.let { client.imageUrl(v.id, "Primary", it, 800) },
                            Modifier.fillMaxSize(),
                        )
                        Box(
                            Modifier.fillMaxSize().background(
                                Brush.verticalGradient(0.4f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.75f))
                            )
                        )
                        Text(
                            v.name.orEmpty(),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                        )
                    }
                }
            }
        }
    }
}

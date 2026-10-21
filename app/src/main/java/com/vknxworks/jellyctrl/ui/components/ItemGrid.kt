package com.vknxworks.jellyctrl.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vknxworks.jellyctrl.data.api.BaseItemDto
import com.vknxworks.jellyctrl.data.api.JellyfinClient
import com.vknxworks.jellyctrl.ui.theme.Gutter
import androidx.compose.ui.res.stringResource
import com.vknxworks.jellyctrl.R
import androidx.compose.ui.res.pluralStringResource

/** Adaptive poster grid that pages in more items as you scroll. More columns appear on tablets and unfolded foldables. */
@Composable
fun PagedItemGrid(
    loader: PagedLoader<BaseItemDto>,
    client: JellyfinClient,
    onItem: (BaseItemDto) -> Unit,
    modifier: Modifier = Modifier,
    onLongItem: ((BaseItemDto) -> Unit)? = null,
    header: (@Composable () -> Unit)? = null,
    emptyText: String = stringResource(R.string.nothing_here),
) {
    val state = rememberLazyGridState()
    val first = loader.items.firstOrNull()
    val minCell = when (first?.posterAspect()) {
        1f -> 140.dp
        16f / 9f -> 220.dp
        else -> 112.dp
    }
    val nearEnd by remember {
        derivedStateOf {
            val last = state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= state.layoutInfo.totalItemsCount - 20
        }
    }
    LaunchedEffect(nearEnd, loader.items.size) {
        if (nearEnd) loader.loadMore()
    }

    if (loader.items.isEmpty()) {
        when {
            loader.error != null -> ErrorView(loader.error!!, modifier) { loader.refresh() }
            loader.loading || loader.total < 0 -> LoadingView(modifier)
            else -> MessageView(Icons.Outlined.Inbox, emptyText, modifier = modifier)
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minCell),
        state = state,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Gutter, end = Gutter, top = 8.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        if (header != null) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "header") { header() }
        }
        items(loader.items, key = { it.id + (it.playlistItemId ?: "") }) { item ->
            PosterCard(
                item, client, null,
                onClick = { onItem(item) },
                onLongClick = onLongItem?.let { { it(item) } },
            )
        }
        if (loader.loading || loader.error != null) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "footer") {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    if (loader.error != null) {
                        TextButton(onClick = { loader.loadMore() }) { Text(stringResource(R.string.retry_x, loader.error.orEmpty())) }
                    } else {
                        CircularProgressIndicator()
                    }
                }
            }
        }
        if (loader.total > 0) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "count") {
                Text(
                    pluralStringResource(R.plurals.count_items, loader.total, loader.total),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            }
        }
    }
}

/** Non-paged grid for short lists. */
@Composable
fun ItemGrid(
    items: List<BaseItemDto>,
    client: JellyfinClient,
    onItem: (BaseItemDto) -> Unit,
    modifier: Modifier = Modifier,
    onLongItem: ((BaseItemDto) -> Unit)? = null,
    emptyText: String = stringResource(R.string.nothing_here),
) {
    if (items.isEmpty()) {
        MessageView(Icons.Outlined.Inbox, emptyText, modifier = modifier)
        return
    }
    val minCell = when (items.first().posterAspect()) {
        1f -> 140.dp
        16f / 9f -> 220.dp
        else -> 112.dp
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minCell),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Gutter, end = Gutter, top = 8.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        items(items, key = { it.id + (it.playlistItemId ?: "") }) { item ->
            PosterCard(item, client, null, onClick = { onItem(item) }, onLongClick = onLongItem?.let { { it(item) } })
        }
    }
}

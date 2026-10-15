package com.vknxworks.jellyctrl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.vknxworks.jellyctrl.data.api.BaseItemDto
import com.vknxworks.jellyctrl.data.api.JellyfinClient

@Composable
fun ArtImage(
    url: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholderText: String? = null,
) {
    val fallback: @Composable () -> Unit = {
        Box(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            if (placeholderText != null) {
                Text(
                    placeholderText,
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Icon(Icons.Outlined.Movie, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    if (url == null) {
        Box(modifier) { fallback() }
    } else {
        SubcomposeAsyncImage(
            model = url,
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale,
            error = { fallback() },
            loading = { Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh)) },
        )
    }
}

@Composable
fun PosterCard(
    item: BaseItemDto,
    client: JellyfinClient,
    width: Dp?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    landscape: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    showSubtitle: Boolean = true,
) {
    val aspect = if (landscape) 16f / 9f else item.posterAspect()
    val url = if (landscape) client.landscapeImage(item) else client.primaryImage(item)
    val base = if (width != null) modifier.width(width) else modifier.fillMaxWidth()
    Column(
        base
            .clip(RoundedCornerShape(10.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(aspect).clip(RoundedCornerShape(10.dp))) {
            ArtImage(url, Modifier.fillMaxSize(), placeholderText = item.name)
            CardBadges(item, Modifier.align(Alignment.TopEnd).padding(6.dp))
            item.resumeProgress()?.let { p ->
                LinearProgressIndicator(
                    progress = { p },
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(4.dp),
                    trackColor = Color.Black.copy(alpha = 0.5f),
                    drawStopIndicator = {},
                )
            }
        }
        Text(
            if (landscape && item.type == "Episode") item.seriesName ?: item.name.orEmpty() else item.cardTitle(),
            modifier = Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (showSubtitle) {
            item.subtitleLine()?.let {
                Text(
                    it,
                    modifier = Modifier.padding(horizontal = 2.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun CardBadges(item: BaseItemDto, modifier: Modifier = Modifier) {
    val ud = item.userData ?: return
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (ud.isFavorite) Badge { Icon(Icons.Filled.Favorite, null, Modifier.size(12.dp), tint = Color(0xFFFF6B81)) }
        val unplayed = ud.unplayedItemCount
        when {
            unplayed != null && unplayed > 0 && item.isFolder -> Badge { Text("$unplayed", style = MaterialTheme.typography.labelSmall, color = Color.White) }
            ud.played -> Badge { Icon(Icons.Filled.Check, null, Modifier.size(12.dp), tint = Color.White) }
        }
    }
}

@Composable
private fun Badge(content: @Composable () -> Unit) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)) {
        Box(Modifier.padding(horizontal = 6.dp, vertical = 3.dp), contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, onMore: (() -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        if (onMore != null) {
            IconButton(onClick = onMore) { Icon(Icons.AutoMirrored.Filled.ArrowForward, "See all") }
        }
    }
}

@Composable
fun ItemRow(
    title: String,
    items: List<BaseItemDto>,
    client: JellyfinClient,
    onItem: (BaseItemDto) -> Unit,
    landscape: Boolean = false,
    onMore: (() -> Unit)? = null,
    onLongItem: ((BaseItemDto) -> Unit)? = null,
) {
    if (items.isEmpty()) return
    SectionHeader(title, onMore = onMore)
    val cardWidth = if (landscape) 240.dp else when (items.firstOrNull()?.posterAspect()) {
        1f -> 140.dp
        16f / 9f -> 220.dp
        else -> 120.dp
    }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items.distinctBy { it.id }, key = { it.id }) { item ->
            PosterCard(
                item, client, cardWidth,
                onClick = { onItem(item) },
                landscape = landscape,
                onLongClick = onLongItem?.let { { it(item) } },
            )
        }
    }
}

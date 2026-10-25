package com.vknxworks.jellyctrl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.vknxworks.jellyctrl.ui.theme.CtrlColors
import com.vknxworks.jellyctrl.ui.theme.CtrlShapes
import com.vknxworks.jellyctrl.ui.theme.CtrlType
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.vknxworks.jellyctrl.data.api.BaseItemDto
import com.vknxworks.jellyctrl.data.api.JellyfinClient
import com.vknxworks.jellyctrl.data.api.ticksToMs
import com.vknxworks.jellyctrl.ui.theme.Gutter
import androidx.compose.ui.res.stringResource
import com.vknxworks.jellyctrl.R
import com.vknxworks.jellyctrl.str

@Composable
fun ArtImage(
    url: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholderText: String? = null,
) {
    val fallback: @Composable () -> Unit = {
        Box(
            Modifier.fillMaxSize().background(CtrlColors.Raised),
            contentAlignment = Alignment.Center,
        ) {
            if (placeholderText != null) {
                Text(
                    placeholderText,
                    modifier = Modifier.padding(10.dp),
                    style = CtrlType.MonoMedium,
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
            loading = { Box(Modifier.fillMaxSize().background(CtrlColors.Surface)) },
        )
    }
}

/** Thin progress line drawn along the bottom edge of artwork. */
@Composable
fun ArtProgress(progress: Float, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(4.dp).background(Color.Black.copy(alpha = 0.55f))) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(4.dp).background(CtrlColors.Accent))
    }
}

@Composable
private fun PlayedMark(modifier: Modifier) {
    Box(
        modifier.size(24.dp).clip(CtrlShapes.Chip).background(CtrlColors.Accent),
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Outlined.Check, stringResource(R.string.watched), Modifier.size(15.dp), tint = CtrlColors.OnAccent) }
}

@Composable
private fun UnplayedCount(count: Int, modifier: Modifier) {
    Box(
        modifier.heightIn(min = 24.dp).clip(CtrlShapes.Chip).background(CtrlColors.Accent).padding(horizontal = 7.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) { Text("$count", style = CtrlType.LabelStrong.copy(letterSpacing = 0.sp), color = CtrlColors.OnAccent) }
}

/** Minutes left on a partly watched item, e.g. "23m left". */
fun BaseItemDto.timeLeft(): String? {
    val pos = userData?.playbackPositionTicks ?: return null
    val total = runTimeTicks ?: return null
    if (pos <= 0 || total <= 0) return null
    val min = ((total - pos).ticksToMs() / 60_000).coerceAtLeast(1)
    return if (min >= 60) str(R.string.x_h_x_m_left, min / 60, min % 60) else str(R.string.x_m_left, min)
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
    val shape = CtrlShapes.Poster
    Column(base.clip(shape).combinedClickable(onClick = onClick, onLongClick = onLongClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(aspect).clip(shape).border(1.dp, CtrlColors.Outline, shape)) {
            ArtImage(url, Modifier.fillMaxSize(), placeholderText = item.name)
            val ud = item.userData
            val unplayed = ud?.unplayedItemCount
            when {
                item.isFolder && unplayed != null && unplayed > 0 -> UnplayedCount(unplayed, Modifier.align(Alignment.TopEnd).padding(8.dp))
                ud?.played == true -> PlayedMark(Modifier.align(Alignment.TopEnd).padding(8.dp))
            }
            item.resumeProgress()?.let { ArtProgress(it, Modifier.align(Alignment.BottomCenter)) }
        }
        Text(
            if (landscape && item.type == "Episode") item.seriesName ?: item.name.orEmpty() else item.cardTitle(),
            modifier = Modifier.padding(top = 10.dp, start = 2.dp, end = 2.dp),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (showSubtitle) {
            val sub = if (landscape) listOfNotNull(item.episodeCode(), item.timeLeft()).joinToString("  ").ifBlank { item.subtitleLine() }
            else item.subtitleLine()
            if (!sub.isNullOrBlank()) {
                Text(
                    sub,
                    modifier = Modifier.padding(top = 2.dp, start = 2.dp, end = 2.dp),
                    style = CtrlType.Mono.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** "S2 E4" for episodes. */
fun BaseItemDto.episodeCode(): String? {
    if (type != "Episode") return null
    val s = parentIndexNumber
    val e = indexNumber
    return when {
        s != null && e != null -> "S$s E$e"
        e != null -> "E$e"
        else -> null
    }
}

/** Row heading. When [onMore] is set the whole heading opens the full list. */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, onMore: (() -> Unit)? = null) {
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onMore != null) Modifier.clickable(onClick = onMore) else Modifier)
            .padding(start = Gutter, end = Gutter, top = 24.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (onMore != null) {
            Spacer(Modifier.width(12.dp))
            CtrlSectionLabel(stringResource(R.string.see_all))
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
    val cardWidth = if (landscape) 260.dp else when (items.firstOrNull()?.posterAspect()) {
        1f -> 148.dp
        16f / 9f -> 240.dp
        else -> 124.dp
    }
    LazyRow(
        contentPadding = PaddingValues(horizontal = Gutter),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
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

package com.vknxworks.jellyctrl.ui.detail

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vknxworks.jellyctrl.data.api.BaseItemDto
import com.vknxworks.jellyctrl.data.api.BaseItemPerson
import com.vknxworks.jellyctrl.data.api.ChapterInfo
import com.vknxworks.jellyctrl.data.api.JellyfinClient
import com.vknxworks.jellyctrl.data.api.MediaStream
import com.vknxworks.jellyctrl.data.api.ticksToMs
import com.vknxworks.jellyctrl.ui.components.ArtImage
import com.vknxworks.jellyctrl.ui.components.SectionHeader
import com.vknxworks.jellyctrl.ui.components.backdropImage
import com.vknxworks.jellyctrl.ui.components.formatClock
import com.vknxworks.jellyctrl.ui.components.formatRuntime
import com.vknxworks.jellyctrl.ui.components.isoToEpochMs
import com.vknxworks.jellyctrl.ui.components.logoImage

@Composable
fun Backdrop(item: BaseItemDto, client: JellyfinClient, modifier: Modifier = Modifier) {
    val bg = MaterialTheme.colorScheme.background
    Box(modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
        ArtImage(client.backdropImage(item), Modifier.fillMaxSize())
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0f to Color.Transparent, 0.55f to bg.copy(alpha = 0.25f), 1f to bg)
            )
        )
        client.logoImage(item)?.let { logo ->
            ArtImage(
                logo,
                Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp).fillMaxWidth(0.55f).height(70.dp),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
fun MetaLine(item: BaseItemDto) {
    val parts = buildList {
        item.productionYear?.let { add(it.toString()) }
        item.officialRating?.let { add(it) }
        formatRuntime(item.runTimeTicks)?.let { add(it) }
        if (item.type == "Series") item.childCount?.let { add("$it seasons") }
        if (item.type == "MusicAlbum") item.childCount?.let { add("$it tracks") }
        val runtime = item.runTimeTicks
        if (runtime != null && item.mediaType == "Video" && item.type != "TvChannel") {
            val remaining = runtime - (item.userData?.playbackPositionTicks ?: 0)
            val end = System.currentTimeMillis() + remaining.ticksToMs()
            val fmt = java.time.format.DateTimeFormatter.ofPattern("HH:mm").withZone(java.time.ZoneId.systemDefault())
            add("Ends at ${fmt.format(java.time.Instant.ofEpochMilli(end))}")
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        item.communityRating?.let {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Star, null, Modifier.size(16.dp), tint = Color(0xFFFFC107))
                Text(" %.1f".format(it), style = MaterialTheme.typography.bodyMedium)
            }
        }
        item.criticRating?.let {
            Text("🍅 ${it.toInt()}%", style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            parts.joinToString("  ·  "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
        )
    }
}

@Composable
fun ExpandableText(text: String, modifier: Modifier = Modifier, collapsedLines: Int = 4) {
    var expanded by remember { mutableStateOf(false) }
    Text(
        text,
        modifier = modifier.animateContentSize().clickable { expanded = !expanded },
        style = MaterialTheme.typography.bodyMedium,
        maxLines = if (expanded) Int.MAX_VALUE else collapsedLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun ChipRow(labels: List<String>, onClick: (String) -> Unit) {
    if (labels.isEmpty()) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEach { l -> AssistChip(onClick = { onClick(l) }, label = { Text(l) }) }
    }
}

@Composable
fun PeopleRow(title: String, people: List<BaseItemPerson>, client: JellyfinClient, onPerson: (BaseItemPerson) -> Unit) {
    if (people.isEmpty()) return
    SectionHeader(title)
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(people, key = { it.id + (it.role ?: "") + (it.type ?: "") }) { p ->
            Column(
                Modifier.width(96.dp).clip(RoundedCornerShape(8.dp)).clickable { onPerson(p) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.size(84.dp).clip(CircleShape)) {
                    ArtImage(
                        p.primaryImageTag?.let { client.imageUrl(p.id, "Primary", it, 200) },
                        Modifier.fillMaxSize(),
                        placeholderText = p.name?.split(" ")?.mapNotNull { it.firstOrNull()?.toString() }?.take(2)?.joinToString(""),
                    )
                }
                Text(p.name.orEmpty(), style = MaterialTheme.typography.labelMedium, maxLines = 2, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis)
                val role = p.role?.takeIf { it.isNotBlank() } ?: p.type
                if (role != null) {
                    Text(
                        role,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
fun ChaptersRow(itemId: String, chapters: List<ChapterInfo>, client: JellyfinClient, onChapter: (ChapterInfo) -> Unit) {
    if (chapters.size < 2) return
    SectionHeader("Chapters")
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(chapters.withIndex().toList(), key = { it.index }) { (i, ch) ->
            Column(Modifier.width(180.dp).clip(RoundedCornerShape(8.dp)).clickable { onChapter(ch) }) {
                Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp))) {
                    ArtImage(ch.imageTag?.let { client.imageUrl(itemId, "Chapter", it, 400, index = i) }, Modifier.fillMaxSize(), placeholderText = ch.name)
                }
                Text(ch.name ?: "Chapter ${i + 1}", style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    formatClock(ch.startPositionTicks.ticksToMs()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** A labelled dropdown used for version, audio and subtitle choices. */
@Composable
fun <T> Picker(label: String, options: List<T>, selected: T?, display: (T?) -> String, onSelect: (T?) -> Unit, allowNone: Boolean = false) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedCard(Modifier.clickable(enabled = options.size > 1 || allowNone) { open = true }) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f, fill = false)) {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(display(selected), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (options.size > 1 || allowNone) Icon(Icons.Filled.ArrowDropDown, null)
            }
        }
        DropdownMenu(open, onDismissRequest = { open = false }) {
            if (allowNone) {
                DropdownMenuItem(text = { Text(display(null)) }, onClick = { onSelect(null); open = false })
            }
            options.forEach { o ->
                DropdownMenuItem(text = { Text(display(o)) }, onClick = { onSelect(o); open = false })
            }
        }
    }
}

fun MediaStream.label(): String = displayTitle ?: buildString {
    append(language ?: "Unknown")
    codec?.let { append(" · ").append(it.uppercase()) }
    if (isForced) append(" · Forced")
    if (isExternal) append(" · External")
}

@Composable
fun MediaInfoCard(streams: List<MediaStream>, container: String?, sizeBytes: Long?) {
    val video = streams.firstOrNull { it.type == "Video" }
    Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Media info", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            container?.let { InfoLine("Container", it.uppercase() + (sizeBytes?.let { s -> "  ·  " + com.vknxworks.jellyctrl.ui.components.formatBytes(s) } ?: "")) }
            video?.let { v ->
                InfoLine(
                    "Video",
                    listOfNotNull(
                        v.codec?.uppercase(), v.profile, v.width?.let { "${it}x${v.height}" },
                        v.videoRangeType ?: v.videoRange, v.bitDepth?.let { "$it-bit" },
                        v.realFrameRate?.let { "%.3f fps".format(it) }, v.bitRate?.let { "${it / 1000} kbps" },
                    ).joinToString("  ·  "),
                )
            }
            streams.filter { it.type == "Audio" }.forEach { InfoLine("Audio", it.label() + (it.channelLayout?.let { l -> "  ·  $l" } ?: "")) }
            streams.filter { it.type == "Subtitle" }.take(8).forEach { InfoLine("Subtitle", it.label()) }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(72.dp))
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

fun airDateText(item: BaseItemDto): String? = isoToEpochMs(item.premiereDate)?.let {
    java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy").withZone(java.time.ZoneId.systemDefault()).format(java.time.Instant.ofEpochMilli(it))
}

@Composable
fun Gap(h: Int = 12) = Spacer(Modifier.height(h.dp))

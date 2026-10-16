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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.vknxworks.jellyctrl.ui.components.formatBytes
import com.vknxworks.jellyctrl.ui.components.formatClock
import com.vknxworks.jellyctrl.ui.components.formatRuntime
import com.vknxworks.jellyctrl.ui.components.isoToEpochMs
import com.vknxworks.jellyctrl.ui.theme.Gutter

/** Year, rating, length and score as plain facts. */
fun metaParts(item: BaseItemDto): List<String> = buildList {
    item.productionYear?.let { add(it.toString()) }
    item.officialRating?.let { add(it) }
    when (item.type) {
        "Series" -> item.childCount?.let { add(if (it == 1) "1 season" else "$it seasons") }
        "MusicAlbum", "Playlist" -> item.childCount?.let { add(if (it == 1) "1 track" else "$it tracks") }
        else -> formatRuntime(item.runTimeTicks)?.let { add(it) }
    }
    item.communityRating?.let { add("★ %.1f".format(it)) }
    val runtime = item.runTimeTicks
    if (runtime != null && item.mediaType == "Video" && item.type != "TvChannel") {
        val remaining = runtime - (item.userData?.playbackPositionTicks ?: 0)
        val end = System.currentTimeMillis() + remaining.ticksToMs()
        val fmt = java.time.format.DateTimeFormatter.ofPattern("HH:mm").withZone(java.time.ZoneId.systemDefault())
        add("Ends ${fmt.format(java.time.Instant.ofEpochMilli(end))}")
    }
}

@Composable
fun ExpandableText(text: String, modifier: Modifier = Modifier, collapsedLines: Int = 4) {
    var expanded by remember { mutableStateOf(false) }
    Text(
        text,
        modifier = modifier.animateContentSize().clickable { expanded = !expanded },
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.86f),
        maxLines = if (expanded) Int.MAX_VALUE else collapsedLines,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Small rounded tags, e.g. genres. */
@Composable
fun TagRow(labels: List<String>, onClick: (String) -> Unit) {
    if (labels.isEmpty()) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEach { l ->
            Text(
                l,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable { onClick(l) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            )
        }
    }
}

@Composable
fun PeopleRow(title: String, people: List<BaseItemPerson>, client: JellyfinClient, onPerson: (BaseItemPerson) -> Unit) {
    if (people.isEmpty()) return
    SectionHeader(title)
    LazyRow(contentPadding = PaddingValues(horizontal = Gutter), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        items(people.distinctBy { it.id + (it.role ?: "") + (it.type ?: "") }, key = { it.id + (it.role ?: "") + (it.type ?: "") }) { p ->
            Column(
                Modifier.width(88.dp).clip(MaterialTheme.shapes.small).clickable { onPerson(p) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.size(80.dp).clip(CircleShape)) {
                    ArtImage(
                        p.primaryImageTag?.let { client.imageUrl(p.id, "Primary", it, 200) },
                        Modifier.fillMaxSize(),
                        placeholderText = p.name?.split(" ")?.mapNotNull { it.firstOrNull()?.toString() }?.take(2)?.joinToString(""),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(p.name.orEmpty(), style = MaterialTheme.typography.labelLarge, maxLines = 2, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis)
                val role = p.role?.takeIf { it.isNotBlank() } ?: p.type
                if (role != null) {
                    Text(
                        role,
                        style = MaterialTheme.typography.bodySmall,
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
    LazyRow(contentPadding = PaddingValues(horizontal = Gutter), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(chapters.withIndex().toList(), key = { it.index }) { (i, ch) ->
            Column(Modifier.width(200.dp).clip(MaterialTheme.shapes.small).clickable { onChapter(ch) }) {
                Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(MaterialTheme.shapes.small)) {
                    ArtImage(ch.imageTag?.let { client.imageUrl(itemId, "Chapter", it, 400, index = i) }, Modifier.fillMaxSize(), placeholderText = ch.name)
                }
                Text(
                    ch.name ?: "Chapter ${i + 1}",
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    formatClock(ch.startPositionTicks.ticksToMs()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

fun MediaStream.label(): String = displayTitle ?: buildString {
    append(language ?: "Unknown")
    codec?.let { append(" ").append(it.uppercase()) }
    if (isForced) append(", forced")
    if (isExternal) append(", external file")
}

/** Codec and file details, folded away until asked for. */
@Composable
fun MediaInfoBlock(streams: List<MediaStream>, container: String?, sizeBytes: Long?) {
    var open by remember { mutableStateOf(false) }
    val video = streams.firstOrNull { it.type == "Video" }
    Column(Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 8.dp)) {
        Text(
            if (open) "Hide file details" else "Show file details",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clip(CircleShape).clickable { open = !open }.padding(vertical = 8.dp),
        )
        if (open) {
            Column(
                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                container?.let { InfoLine("File", it.uppercase() + (sizeBytes?.let { s -> ", " + formatBytes(s) } ?: "")) }
                video?.let { v ->
                    InfoLine(
                        "Video",
                        listOfNotNull(
                            v.codec?.uppercase(), v.profile, v.width?.let { "${it}×${v.height}" },
                            v.videoRangeType ?: v.videoRange, v.bitDepth?.let { "$it-bit" },
                            v.realFrameRate?.let { "%.3f fps".format(it) }, v.bitRate?.let { "${it / 1000} kbps" },
                        ).joinToString(", "),
                    )
                }
                streams.filter { it.type == "Audio" }.forEach { InfoLine("Audio", it.label() + (it.channelLayout?.let { l -> ", $l" } ?: "")) }
                streams.filter { it.type == "Subtitle" }.take(10).forEach { InfoLine("Subtitles", it.label()) }
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(76.dp))
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

fun airDateText(item: BaseItemDto): String? = isoToEpochMs(item.premiereDate)?.let {
    java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy").withZone(java.time.ZoneId.systemDefault()).format(java.time.Instant.ofEpochMilli(it))
}

@Composable
fun Gap(h: Int = 12) = Spacer(Modifier.height(h.dp))

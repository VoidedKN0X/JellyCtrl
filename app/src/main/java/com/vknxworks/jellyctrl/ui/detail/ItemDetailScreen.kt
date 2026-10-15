package com.vknxworks.jellyctrl.ui.detail

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.jellyctrl.MainActivity
import com.vknxworks.jellyctrl.container
import com.vknxworks.jellyctrl.data.api.BaseItemDto
import com.vknxworks.jellyctrl.data.api.ItemQuery
import com.vknxworks.jellyctrl.data.api.JellyfinClient
import com.vknxworks.jellyctrl.data.api.MediaSourceInfo
import com.vknxworks.jellyctrl.data.api.MediaStream
import com.vknxworks.jellyctrl.data.api.deleteItem
import com.vknxworks.jellyctrl.data.api.episodes
import com.vknxworks.jellyctrl.data.api.item
import com.vknxworks.jellyctrl.data.api.items
import com.vknxworks.jellyctrl.data.api.localTrailers
import com.vknxworks.jellyctrl.data.api.nextUp
import com.vknxworks.jellyctrl.data.api.playlistItems
import com.vknxworks.jellyctrl.data.api.refreshItem
import com.vknxworks.jellyctrl.data.api.removeFromPlaylist
import com.vknxworks.jellyctrl.data.api.seasons
import com.vknxworks.jellyctrl.data.api.setFavorite
import com.vknxworks.jellyctrl.data.api.setPlayed
import com.vknxworks.jellyctrl.data.api.similar
import com.vknxworks.jellyctrl.data.api.specialFeatures
import com.vknxworks.jellyctrl.data.api.ticksToMs
import com.vknxworks.jellyctrl.data.download.DownloadStatus
import com.vknxworks.jellyctrl.player.PlayerActivity
import com.vknxworks.jellyctrl.ui.components.ArtImage
import com.vknxworks.jellyctrl.ui.components.ItemActionsSheet
import com.vknxworks.jellyctrl.ui.components.ItemRow
import com.vknxworks.jellyctrl.ui.components.LoadContent
import com.vknxworks.jellyctrl.ui.components.SectionHeader
import com.vknxworks.jellyctrl.ui.components.downloadItemWith
import com.vknxworks.jellyctrl.ui.components.formatClock
import com.vknxworks.jellyctrl.ui.components.formatRuntime
import com.vknxworks.jellyctrl.ui.components.friendlyMessage
import com.vknxworks.jellyctrl.ui.components.landscapeImage
import com.vknxworks.jellyctrl.ui.components.primaryImage
import com.vknxworks.jellyctrl.ui.components.rememberLoader
import com.vknxworks.jellyctrl.ui.components.resumeProgress
import com.vknxworks.jellyctrl.ui.nav.EditMetadataRoute
import com.vknxworks.jellyctrl.ui.nav.ItemRoute
import com.vknxworks.jellyctrl.ui.nav.ListRoute
import com.vknxworks.jellyctrl.ui.nav.LocalNavigator
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** Everything a detail page shows, loaded in parallel. */
private data class Detail(
    val item: BaseItemDto,
    val seasons: List<BaseItemDto> = emptyList(),
    val episodes: List<BaseItemDto> = emptyList(),
    val nextUp: BaseItemDto? = null,
    val similar: List<BaseItemDto> = emptyList(),
    val extras: List<BaseItemDto> = emptyList(),
    val children: List<BaseItemDto> = emptyList(),
    val filmography: List<BaseItemDto> = emptyList(),
)

private suspend fun loadDetail(client: JellyfinClient, id: String): Detail = coroutineScope {
    val item = client.item(id)
    when (item.type) {
        "Series" -> {
            val seasons = async { client.seasons(id) }
            val next = async { runCatching { client.nextUp(1, seriesId = id).firstOrNull() }.getOrNull() }
            val similar = async { runCatching { client.similar(id) }.getOrDefault(emptyList()) }
            val extras = async { client.specialFeatures(id) }
            Detail(item, seasons = seasons.await(), nextUp = next.await(), similar = similar.await(), extras = extras.await())
        }
        "Season" -> Detail(item, episodes = client.episodes(item.seriesId ?: item.parentId ?: id, id))
        "Episode" -> {
            val eps = async {
                val sid = item.seriesId
                if (sid != null) runCatching { client.episodes(sid, item.seasonId) }.getOrDefault(emptyList()) else emptyList()
            }
            Detail(item, episodes = eps.await())
        }
        "MusicAlbum" -> {
            val tracks = client.items(
                ItemQuery(parentId = id, recursive = true, includeItemTypes = listOf("Audio"), sortBy = listOf("ParentIndexNumber", "IndexNumber", "SortName")),
                limit = 500,
            ).items
            val similar = async { runCatching { client.similar(id) }.getOrDefault(emptyList()) }
            Detail(item, children = tracks, similar = similar.await())
        }
        "MusicArtist" -> {
            val albums = async {
                client.items(
                    ItemQuery(albumArtistIds = listOf(id), includeItemTypes = listOf("MusicAlbum"), sortBy = listOf("ProductionYear", "SortName"), sortOrder = "Descending"),
                    limit = 200,
                ).items
            }
            val appears = async {
                client.items(
                    ItemQuery(artistIds = listOf(id), includeItemTypes = listOf("MusicAlbum"), sortBy = listOf("ProductionYear", "SortName"), sortOrder = "Descending"),
                    limit = 200,
                ).items
            }
            val similar = async { runCatching { client.similar(id) }.getOrDefault(emptyList()) }
            val own = albums.await()
            Detail(item, children = own, filmography = appears.await().filter { a -> own.none { it.id == a.id } }, similar = similar.await())
        }
        "Playlist" -> Detail(item, children = client.playlistItems(id))
        "BoxSet" -> Detail(
            item,
            children = client.items(ItemQuery(parentId = id, recursive = false, sortBy = listOf("ProductionYear", "SortName")), limit = 500).items,
        )
        "Person" -> Detail(
            item,
            filmography = client.items(
                ItemQuery(personIds = listOf(id), includeItemTypes = listOf("Movie", "Series", "MusicVideo"), sortBy = listOf("ProductionYear", "SortName"), sortOrder = "Descending"),
                limit = 300,
            ).items,
        )
        else -> {
            val similar = async { runCatching { client.similar(id) }.getOrDefault(emptyList()) }
            val extras = async { client.specialFeatures(id) + client.localTrailers(id) }
            Detail(item, similar = similar.await(), extras = extras.await())
        }
    }
}

@Composable
fun ItemDetailScreen(id: String) {
    val c = LocalContext.current.container
    val client = c.api
    val loader = rememberLoader("detail-$id") { loadDetail(client, id) }
    val nav = LocalNavigator.current

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = nav::back,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Color.Black.copy(alpha = 0.35f), contentColor = Color.White),
                    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = { loader.value?.item?.let { DetailOverflow(it, onChanged = { loader.reload(silent = true) }) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { _ ->
        LoadContent(loader) { d ->
            DetailBody(d, client) { loader.reload(silent = true) }
        }
    }
}

@Composable
private fun DetailBody(d: Detail, client: JellyfinClient, reload: () -> Unit) {
    val item = d.item
    val nav = LocalNavigator.current
    val ctx = LocalContext.current
    var menuItem by remember { mutableStateOf<BaseItemDto?>(null) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 720.dp
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
            item("hero") {
                if (item.type == "Person") PersonHeader(item, client) else Hero(item, client, wide)
            }
            item("actions") {
                Column(Modifier.padding(horizontal = 16.dp).widthIn(max = 900.dp)) {
                    ActionsBar(d, reload)
                }
            }
            when (item.type) {
                "Series" -> seriesSections(d, client, nav::openItem) { menuItem = it }
                "Season" -> episodeList(d.episodes, client, nav::openItem, nav::play) { menuItem = it }
                "MusicAlbum", "Playlist" -> trackList(d, client, reload) { menuItem = it }
                "MusicArtist" -> {
                    item("albums") { ItemRow("Albums", d.children, client, nav::openItem, onLongItem = { menuItem = it }) }
                    item("appears") { ItemRow("Appears on", d.filmography, client, nav::openItem) }
                }
                "BoxSet" -> item("children") { ItemRow("In this collection", d.children, client, nav::openItem, onLongItem = { menuItem = it }) }
                "Person" -> item("films") { ItemRow("Filmography", d.filmography, client, nav::openItem, onLongItem = { menuItem = it }) }
                else -> {
                    if (item.type == "Episode" && d.episodes.size > 1) {
                        item("season") {
                            ItemRow(
                                item.seasonName ?: "More from this season",
                                d.episodes.filter { it.id != item.id }, client, nav::openItem, landscape = true,
                                onMore = { item.seasonId?.let { nav.navigate(ItemRoute(it)) } },
                            )
                        }
                    }
                    item("chapters") {
                        ChaptersRow(item.id, item.chapters, client) { ch ->
                            PlayerActivity.start(ctx, item.id, startMs = ch.startPositionTicks.ticksToMs())
                        }
                    }
                }
            }
            if (item.type != "Person") {
                item("cast") {
                    PeopleRow("Cast & crew", item.people, client) { p -> nav.navigate(ItemRoute(p.id)) }
                }
            }
            item("extras") { ItemRow("Extras", d.extras, client, nav::openItem, landscape = true) }
            item("similar") { ItemRow("More like this", d.similar, client, nav::openItem, onLongItem = { menuItem = it }) }
            item("info") {
                val source = item.mediaSources.firstOrNull()
                if (source != null && item.mediaType == "Video") {
                    Column(Modifier.padding(16.dp)) {
                        MediaInfoCard(source.mediaStreams, source.container, source.size)
                    }
                }
            }
            if (item.externalUrls.isNotEmpty()) {
                item("links") {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        SectionHeader("Links", Modifier.padding(start = 0.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item.externalUrls.forEach { link ->
                                TextButton(onClick = {
                                    link.url?.let { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }
                                }) { Text(link.name ?: "Link") }
                            }
                        }
                    }
                }
            }
        }
    }

    menuItem?.let { ItemActionsSheet(it, onDismiss = { menuItem = null }, onChanged = reload) }
}

@Composable
private fun Hero(item: BaseItemDto, client: JellyfinClient, wide: Boolean) {
    val nav = LocalNavigator.current
    val square = item.type == "MusicAlbum" || item.type == "MusicArtist" || item.type == "Playlist" || item.type == "Audio"
    Column {
        Backdrop(item, client)
        Row(
            Modifier.padding(horizontal = 16.dp).offset(y = if (wide) (-60).dp else (-40).dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            val posterW = if (wide) 160.dp else 110.dp
            Box(
                Modifier
                    .width(posterW)
                    .aspectRatio(if (square) 1f else if (item.type == "Episode") 16f / 9f else 2f / 3f)
                    .clip(RoundedCornerShape(10.dp)),
            ) {
                ArtImage(client.primaryImage(item, 500), Modifier.fillMaxSize(), placeholderText = item.name)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                if (item.type == "Episode" || item.type == "Season") {
                    Text(
                        item.seriesName.orEmpty(),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { item.seriesId?.let { nav.navigate(ItemRoute(it)) } },
                    )
                }
                if (item.type == "MusicAlbum") {
                    val artist = item.albumArtists.firstOrNull()
                    Text(
                        artist?.name ?: item.albumArtist.orEmpty(),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { artist?.id?.let { nav.navigate(ItemRoute(it)) } },
                    )
                }
                Text(
                    buildString {
                        if (item.type == "Episode") {
                            item.parentIndexNumber?.let { append("S$it ") }
                            item.indexNumber?.let { append("E$it · ") }
                        }
                        append(item.name.orEmpty())
                    },
                    style = if (wide) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.originalTitle != null && item.originalTitle != item.name) {
                    Text(item.originalTitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(4.dp))
                MetaLine(item)
            }
        }
    }
}

@Composable
private fun PersonHeader(item: BaseItemDto, client: JellyfinClient) {
    Column(
        Modifier.fillMaxWidth().statusBarsPadding().padding(top = 56.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(160.dp).clip(CircleShape)) {
            ArtImage(client.primaryImage(item, 400), Modifier.fillMaxSize(), placeholderText = item.name)
        }
        Spacer(Modifier.height(12.dp))
        Text(item.name.orEmpty(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        item.premiereDate?.let { airDateText(item)?.let { d -> Text("Born $d", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
    }
}

/** Play buttons, track pickers, played/favorite/download and the overview. */
@Composable
private fun ActionsBar(d: Detail, reload: () -> Unit) {
    val context = LocalContext.current
    val c = context.container
    val nav = LocalNavigator.current
    val item = d.item
    val scope = rememberCoroutineScope()
    val downloads by c.downloads.entries.collectAsStateWithLifecycle()
    val dl = downloads.firstOrNull { it.itemId == item.id }

    // Track selection for directly playable video
    val sources = item.mediaSources
    var source by remember(item.id) { mutableStateOf(sources.firstOrNull()) }
    val streams = source?.mediaStreams ?: item.mediaStreams
    val audioStreams = streams.filter { it.type == "Audio" }
    val subStreams = streams.filter { it.type == "Subtitle" }
    var audio by remember(item.id, source?.id) {
        mutableStateOf(audioStreams.firstOrNull { it.index == source?.defaultAudioStreamIndex } ?: audioStreams.firstOrNull())
    }
    var sub by remember(item.id, source?.id) {
        mutableStateOf(subStreams.firstOrNull { it.index == source?.defaultSubtitleStreamIndex })
    }

    val played = item.userData?.played == true
    val fav = item.userData?.isFavorite == true
    val resumeMs = item.userData?.playbackPositionTicks?.ticksToMs() ?: 0
    val isVideo = item.mediaType == "Video" || item.type in setOf("Movie", "Episode", "Video", "MusicVideo", "Recording", "Trailer")

    fun toggle(block: suspend () -> Unit) {
        scope.launch {
            runCatching { block() }.onSuccess { reload() }
                .onFailure { Toast.makeText(context, it.friendlyMessage(), Toast.LENGTH_SHORT).show() }
        }
    }

    fun playVideo(fromStart: Boolean) {
        PlayerActivity.start(
            context, item.id,
            startMs = if (fromStart) 0 else null,
            mediaSourceId = source?.id,
            audioIndex = audio?.index,
            subtitleIndex = sub?.index ?: -1,
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item.resumeProgress()?.let { p ->
            Column {
                LinearProgressIndicator(progress = { p }, modifier = Modifier.fillMaxWidth(), drawStopIndicator = {})
                val left = (item.runTimeTicks ?: 0).ticksToMs() - resumeMs
                Text("${formatClock(left)} left", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            val playLabel = when {
                item.type == "Series" && d.nextUp != null -> "Play ${d.nextUp.subtitleShort()}"
                resumeMs > 0 -> "Resume"
                else -> "Play"
            }
            val canPlay = item.isPlayable || item.isFolder || item.type == "Person"
            if (canPlay && item.type != "Person") {
                Button(onClick = {
                    when {
                        isVideo -> playVideo(false)
                        item.type == "Series" && d.nextUp != null -> nav.play(d.nextUp)
                        item.type in setOf("MusicAlbum", "Playlist") && d.children.isNotEmpty() && d.children.first().mediaType == "Audio" ->
                            c.music.playItems(d.children, 0)
                        else -> nav.play(item)
                    }
                }) {
                    Icon(Icons.Filled.PlayArrow, null)
                    Spacer(Modifier.width(6.dp))
                    Text(playLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (resumeMs > 0 && isVideo) {
                FilledTonalIconButton(onClick = { playVideo(true) }) { Icon(Icons.Filled.Replay, "Play from beginning") }
            }
            if (item.isFolder && item.type != "Person") {
                FilledTonalIconButton(onClick = { nav.shuffle(item) }) { Icon(Icons.Filled.Shuffle, "Shuffle") }
            }
            if (item.type == "MusicArtist" || item.type == "MusicAlbum" || item.mediaType == "Audio") {
                FilledTonalIconButton(onClick = { c.music.instantMix(item) }) { Icon(Icons.Outlined.AutoAwesome, "Instant mix") }
            }
            if (item.type != "Person") {
                FilledTonalIconButton(onClick = { toggle { c.api.setPlayed(item.id, !played) } }) {
                    Icon(if (played) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle, if (played) "Mark unplayed" else "Mark played",
                        tint = if (played) MaterialTheme.colorScheme.primary else androidx.compose.material3.LocalContentColor.current)
                }
            }
            FilledTonalIconButton(onClick = { toggle { c.api.setFavorite(item.id, !fav) } }) {
                Icon(if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, "Favorite",
                    tint = if (fav) Color(0xFFFF6B81) else androidx.compose.material3.LocalContentColor.current)
            }
            if ((isVideo && item.canDownload) || item.type in setOf("Season", "Series", "MusicAlbum")) {
                FilledTonalIconButton(onClick = {
                    if (dl?.status == DownloadStatus.DONE) {
                        Toast.makeText(context, "Already downloaded", Toast.LENGTH_SHORT).show()
                    } else {
                        (context as? MainActivity)?.ensureNotificationPermission()
                        scope.launch {
                            val n = runCatching { downloadItemWith(c, item) }.getOrElse { 0 }
                            Toast.makeText(context, if (n > 0) "$n queued for download" else "Couldn't queue download", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) {
                    Icon(if (dl?.status == DownloadStatus.DONE) Icons.Filled.DownloadDone else Icons.Filled.Download, "Download")
                }
            }
        }

        if (isVideo && source != null) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (sources.size > 1) {
                    Picker("Version", sources, source, { it?.name ?: "Default" }, { s -> source = s })
                }
                if (audioStreams.isNotEmpty()) {
                    Picker("Audio", audioStreams, audio, { it?.label() ?: "Default" }, { a -> audio = a })
                }
                if (subStreams.isNotEmpty()) {
                    Picker("Subtitles", subStreams, sub, { it?.label() ?: "Off" }, { s -> sub = s }, allowNone = true)
                }
            }
        }

        item.taglines.firstOrNull()?.let {
            Text(it, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
        item.overview?.takeIf { it.isNotBlank() }?.let {
            ExpandableText(it.replace(Regex("<br\\s*/?>"), "\n").replace(Regex("<[^>]+>"), ""))
        }
        if (item.genres.isNotEmpty()) {
            ChipRow(item.genres) { g -> nav.navigate(ListRoute(g, "genre", g)) }
        }
        val directors = item.people.filter { it.type == "Director" }.mapNotNull { it.name }
        val writers = item.people.filter { it.type == "Writer" }.mapNotNull { it.name }
        if (directors.isNotEmpty()) LabelValue("Director", directors.joinToString())
        if (writers.isNotEmpty()) LabelValue("Writers", writers.joinToString())
        if (item.studios.isNotEmpty()) LabelValue("Studio", item.studios.mapNotNull { it.name }.joinToString())
        if (item.type == "Episode") airDateText(item)?.let { LabelValue("Aired", it) }
    }
}

private fun BaseItemDto.subtitleShort(): String =
    if (parentIndexNumber != null && indexNumber != null) "S${parentIndexNumber}:E$indexNumber" else name.orEmpty()

@Composable
private fun LabelValue(label: String, value: String) {
    Row {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(80.dp))
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

private fun LazyListScope.seriesSections(
    d: Detail,
    client: JellyfinClient,
    onItem: (BaseItemDto) -> Unit,
    onLong: (BaseItemDto) -> Unit,
) {
    d.nextUp?.let { n -> item("nextup") { ItemRow("Next up", listOf(n), client, onItem, landscape = true, onLongItem = onLong) } }
    item("seasons") { ItemRow("Seasons", d.seasons, client, onItem, onLongItem = onLong) }
}

private fun LazyListScope.episodeList(
    episodes: List<BaseItemDto>,
    client: JellyfinClient,
    onItem: (BaseItemDto) -> Unit,
    onPlay: (BaseItemDto, Boolean) -> Unit,
    onLong: (BaseItemDto) -> Unit,
) {
    item("eps-header") { SectionHeader("${episodes.size} episodes") }
    items(episodes, key = { "ep-" + it.id }) { ep ->
        EpisodeRow(ep, client, onClick = { onItem(ep) }, onPlay = { onPlay(ep, false) }, onLong = { onLong(ep) })
    }
}

@Composable
fun EpisodeRow(ep: BaseItemDto, client: JellyfinClient, onClick: () -> Unit, onPlay: () -> Unit, onLong: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLong)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(Modifier.width(150.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp))) {
            ArtImage(client.landscapeImage(ep, 400), Modifier.fillMaxSize(), placeholderText = ep.name)
            ep.resumeProgress()?.let { p ->
                LinearProgressIndicator(
                    progress = { p },
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp),
                    drawStopIndicator = {},
                )
            }
            IconButton(onClick = onPlay, modifier = Modifier.align(Alignment.Center)) {
                Icon(Icons.Filled.PlayArrow, "Play", tint = Color.White)
            }
            if (ep.userData?.played == true) {
                Icon(
                    Icons.Filled.CheckCircle, "Played",
                    Modifier.align(Alignment.TopEnd).padding(4.dp).size(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "${ep.indexNumber ?: ""}. ${ep.name.orEmpty()}",
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOfNotNull(formatRuntime(ep.runTimeTicks), airDateText(ep)).joinToString("  ·  "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ep.overview?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun LazyListScope.trackList(d: Detail, client: JellyfinClient, reload: () -> Unit, onLong: (BaseItemDto) -> Unit) {
    val isPlaylist = d.item.type == "Playlist"
    item("tracks-header") { SectionHeader(if (isPlaylist) "${d.children.size} items" else "Tracks") }
    itemsIndexed(d.children, key = { i, t -> "t-$i-" + t.id }) { i, track ->
        TrackRow(d, i, track, client, isPlaylist, reload, onLong)
    }
}

@Composable
private fun TrackRow(d: Detail, index: Int, track: BaseItemDto, client: JellyfinClient, isPlaylist: Boolean, reload: () -> Unit, onLong: (BaseItemDto) -> Unit) {
    val context = LocalContext.current
    val c = context.container
    val nav = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val multiDisc = d.children.mapNotNull { it.parentIndexNumber }.distinct().size > 1
    if (multiDisc && !isPlaylist && (index == 0 || d.children[index - 1].parentIndexNumber != track.parentIndexNumber)) {
        Text(
            "Disc ${track.parentIndexNumber ?: 1}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
        )
    }
    ListItem(
        headlineContent = { Text(track.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            val sub = if (isPlaylist) track.artists.joinToString().ifBlank { track.seriesName ?: track.productionYear?.toString() ?: "" }
            else track.artists.joinToString().takeIf { it != d.item.albumArtist } ?: ""
            if (sub.isNotBlank()) Text(sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        leadingContent = {
            if (isPlaylist) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(6.dp))) { ArtImage(client.primaryImage(track, 120), Modifier.fillMaxSize()) }
            } else {
                Text("${track.indexNumber ?: index + 1}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(28.dp))
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatClock((track.runTimeTicks ?: 0).ticksToMs()), style = MaterialTheme.typography.labelSmall)
                if (isPlaylist && track.playlistItemId != null) {
                    IconButton(onClick = {
                        scope.launch {
                            runCatching { c.api.removeFromPlaylist(d.item.id, listOf(track.playlistItemId)) }
                                .onSuccess { reload() }
                                .onFailure { Toast.makeText(context, it.friendlyMessage(), Toast.LENGTH_SHORT).show() }
                        }
                    }) { Icon(Icons.Filled.Delete, "Remove from playlist") }
                }
            }
        },
        modifier = Modifier.combinedClickable(
            onClick = {
                if (track.mediaType == "Audio") c.music.playItems(d.children.filter { it.mediaType == "Audio" }, d.children.filter { it.mediaType == "Audio" }.indexOf(track).coerceAtLeast(0))
                else nav.play(track)
            },
            onLongClick = { onLong(track) },
        ),
    )
}

/** Overflow: edit/refresh/delete for admins and owners. */
@Composable
private fun DetailOverflow(item: BaseItemDto, onChanged: () -> Unit) {
    val context = LocalContext.current
    val c = context.container
    val nav = LocalNavigator.current
    val session by c.sessions.active.collectAsStateWithLifecycle()
    val isAdmin = session?.account?.isAdmin == true
    val scope = rememberCoroutineScope()
    var open by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    if (!isAdmin && !item.canDelete && item.remoteTrailers.isEmpty()) return

    Box {
        FilledTonalIconButton(
            onClick = { open = true },
            colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Color.Black.copy(alpha = 0.35f), contentColor = Color.White),
        ) { Icon(Icons.Filled.MoreVert, "More") }
        DropdownMenu(open, onDismissRequest = { open = false }) {
            item.remoteTrailers.firstOrNull()?.url?.let { url ->
                DropdownMenuItem(
                    text = { Text("Watch trailer") },
                    leadingIcon = { Icon(Icons.Filled.Movie, null) },
                    onClick = {
                        open = false
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                    },
                )
            }
            if (isAdmin) {
                DropdownMenuItem(
                    text = { Text("Edit metadata") },
                    leadingIcon = { Icon(Icons.Filled.Edit, null) },
                    onClick = { open = false; nav.navigate(EditMetadataRoute(item.id)) },
                )
                DropdownMenuItem(
                    text = { Text("Refresh metadata") },
                    leadingIcon = { Icon(Icons.Filled.Refresh, null) },
                    onClick = {
                        open = false
                        scope.launch {
                            runCatching { c.api.refreshItem(item.id, replaceAll = false) }
                                .onSuccess { Toast.makeText(context, "Refresh queued", Toast.LENGTH_SHORT).show() }
                                .onFailure { Toast.makeText(context, it.friendlyMessage(), Toast.LENGTH_SHORT).show() }
                        }
                    },
                )
            }
            if (item.canDelete) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Delete from server", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) },
                    onClick = { open = false; confirmDelete = true },
                )
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${item.name}?") },
            text = { Text("This deletes the files from the server's disk. It can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch {
                        runCatching { c.api.deleteItem(item.id) }
                            .onSuccess { Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show(); nav.back() }
                            .onFailure { Toast.makeText(context, it.friendlyMessage(), Toast.LENGTH_SHORT).show() }
                    }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

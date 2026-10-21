package com.vknxworks.jellyctrl.ui.detail

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
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
import com.vknxworks.jellyctrl.ui.components.AddToPlaylistDialog
import com.vknxworks.jellyctrl.ui.components.ArtImage
import com.vknxworks.jellyctrl.ui.components.ArtProgress
import com.vknxworks.jellyctrl.ui.components.GroupRow
import com.vknxworks.jellyctrl.ui.components.ItemActionsSheet
import com.vknxworks.jellyctrl.ui.components.ItemRow
import com.vknxworks.jellyctrl.ui.components.LoadContent
import com.vknxworks.jellyctrl.ui.components.LoadingView
import com.vknxworks.jellyctrl.ui.components.MetaRow
import com.vknxworks.jellyctrl.ui.components.PillButton
import com.vknxworks.jellyctrl.ui.components.RoundAction
import com.vknxworks.jellyctrl.ui.components.SectionHeader
import com.vknxworks.jellyctrl.ui.components.backdropImage
import com.vknxworks.jellyctrl.ui.components.downloadItemWith
import com.vknxworks.jellyctrl.ui.components.formatClock
import com.vknxworks.jellyctrl.ui.components.formatRuntime
import com.vknxworks.jellyctrl.ui.components.friendlyMessage
import com.vknxworks.jellyctrl.ui.components.landscapeImage
import com.vknxworks.jellyctrl.ui.components.logoImage
import com.vknxworks.jellyctrl.ui.components.primaryImage
import com.vknxworks.jellyctrl.ui.components.rememberLoader
import com.vknxworks.jellyctrl.ui.components.resumeProgress
import com.vknxworks.jellyctrl.ui.components.timeLeft
import com.vknxworks.jellyctrl.ui.nav.EditMetadataRoute
import com.vknxworks.jellyctrl.ui.nav.ItemRoute
import com.vknxworks.jellyctrl.ui.nav.ListRoute
import com.vknxworks.jellyctrl.ui.nav.LocalNavigator
import com.vknxworks.jellyctrl.ui.theme.ArtworkTint
import com.vknxworks.jellyctrl.ui.theme.Gutter
import com.vknxworks.jellyctrl.ui.theme.rememberArtworkColor
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.vknxworks.jellyctrl.R
import com.vknxworks.jellyctrl.str
import androidx.compose.ui.res.pluralStringResource
import com.vknxworks.jellyctrl.plural

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

private val SQUARE_TYPES = setOf("MusicAlbum", "MusicArtist", "Playlist", "Audio")
private val VIDEO_TYPES = setOf("Movie", "Episode", "Video", "MusicVideo", "Recording", "Trailer")

@Composable
fun ItemDetailScreen(id: String) {
    val c = LocalContext.current.container
    val client = c.api
    val nav = LocalNavigator.current
    val loader = rememberLoader("detail-$id") { loadDetail(client, id) }
    val item = loader.value?.item
    val seed = rememberArtworkColor(item?.let { client.primaryImage(it, 300) })

    ArtworkTint(seed) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            LoadContent(loader) { d -> DetailBody(d, client) { loader.reload(silent = true) } }
            IconButton(
                onClick = nav::back,
                modifier = Modifier.statusBarsPadding().padding(start = 8.dp, top = 4.dp)
                    .clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)),
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = Color.White) }
        }
    }
}

@Composable
private fun DetailBody(d: Detail, client: JellyfinClient, reload: () -> Unit) {
    val cur = d.item
    val nav = LocalNavigator.current
    val ctx = LocalContext.current
    var menuItem by remember { mutableStateOf<BaseItemDto?>(null) }

    // Series pages show one season's episodes at a time, starting with the season you're up to.
    var seasonId by rememberSaveable(cur.id) { mutableStateOf(d.nextUp?.seasonId ?: d.seasons.firstOrNull()?.id) }
    val seasonEpisodes = if (cur.type == "Series" && seasonId != null) {
        rememberLoader("eps-${cur.id}-$seasonId") { client.episodes(cur.id, seasonId) }
    } else null

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 720.dp
        val widthDp = maxWidth.value
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 40.dp)) {
            item("hero") {
                when {
                    cur.type == "Person" -> PersonHeader(cur, client)
                    cur.type in SQUARE_TYPES -> SquareHeader(cur, client)
                    else -> Hero(cur, client, wide, widthDp)
                }
            }
            item("actions") {
                Column(Modifier.padding(horizontal = Gutter).widthIn(max = 760.dp)) {
                    ActionsBlock(d, reload)
                }
            }
            when (cur.type) {
                "Series" -> {
                    if (d.seasons.isNotEmpty()) {
                        item("season-chips") { SeasonChips(d.seasons, seasonId) { seasonId = it } }
                        val eps = seasonEpisodes?.value
                        if (eps == null) {
                            item("eps-loading") { Box(Modifier.fillMaxWidth().height(160.dp)) { LoadingView() } }
                        } else {
                            items(eps, key = { "sep-" + it.id }) { ep ->
                                EpisodeRow(ep, client, onClick = { nav.openItem(ep) }, onPlay = { nav.play(ep) }, onLong = { menuItem = ep })
                            }
                        }
                    }
                }
                "Season" -> {
                    item("eps-header") { SectionHeader(pluralStringResource(R.plurals.count_episodes, d.episodes.size, d.episodes.size)) }
                    items(d.episodes, key = { "ep-" + it.id }) { ep ->
                        EpisodeRow(ep, client, onClick = { nav.openItem(ep) }, onPlay = { nav.play(ep) }, onLong = { menuItem = ep })
                    }
                }
                "MusicAlbum", "Playlist" -> trackList(d, client, reload) { menuItem = it }
                "MusicArtist" -> {
                    item("albums") { ItemRow(stringResource(R.string.albums), d.children, client, nav::openItem, onLongItem = { menuItem = it }) }
                    item("appears") { ItemRow(stringResource(R.string.appears_on), d.filmography, client, nav::openItem) }
                }
                "BoxSet" -> item("children") { ItemRow(stringResource(R.string.in_this_collection), d.children, client, nav::openItem, onLongItem = { menuItem = it }) }
                "Person" -> item("films") { ItemRow(stringResource(R.string.known_for), d.filmography, client, nav::openItem, onLongItem = { menuItem = it }) }
                else -> {
                    if (cur.type == "Episode" && d.episodes.size > 1) {
                        item("season") {
                            ItemRow(
                                cur.seasonName ?: stringResource(R.string.more_from_this_season),
                                d.episodes.filter { it.id != cur.id }, client, nav::openItem, landscape = true,
                                onMore = { cur.seasonId?.let { nav.navigate(ItemRoute(it)) } },
                            )
                        }
                    }
                    item("chapters") {
                        ChaptersRow(cur.id, cur.chapters, client) { ch ->
                            PlayerActivity.start(ctx, cur.id, startMs = ch.startPositionTicks.ticksToMs())
                        }
                    }
                }
            }
            if (cur.type != "Person") {
                item("cast") { PeopleRow(stringResource(R.string.cast_and_crew), cur.people, client) { p -> nav.navigate(ItemRoute(p.id)) } }
            }
            item("extras") { ItemRow(stringResource(R.string.extras), d.extras, client, nav::openItem, landscape = true) }
            item("similar") { ItemRow(stringResource(R.string.more_like_this), d.similar, client, nav::openItem, onLongItem = { menuItem = it }) }
            item("info") {
                val source = cur.mediaSources.firstOrNull()
                if (source != null && cur.mediaType == "Video") MediaInfoBlock(source.mediaStreams, source.container, source.size)
            }
            if (cur.externalUrls.isNotEmpty()) {
                item("links") {
                    Column {
                        SectionHeader(stringResource(R.string.links))
                        LazyRow(contentPadding = PaddingValues(horizontal = Gutter), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(cur.externalUrls.filter { it.url != null }.distinctBy { it.url }, key = { it.url.orEmpty() }) { link ->
                                Text(
                                    link.name ?: stringResource(R.string.link),
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                        .clickable { link.url?.let { u -> openWebLink(ctx, u) } }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    menuItem?.let { ItemActionsSheet(it, onDismiss = { menuItem = null }, onChanged = reload) }
}

// ---------- headers ----------

@Composable
private fun Hero(item: BaseItemDto, client: JellyfinClient, wide: Boolean, widthDp: Float) {
    val nav = LocalNavigator.current
    val bg = MaterialTheme.colorScheme.background
    val image = if (item.type == "Episode") client.landscapeImage(item, 1280) else client.backdropImage(item, if (wide) 1920 else 1280)
    val heightDp = if (wide) (widthDp * 0.45f).coerceIn(360f, 560f) else (widthDp * 1.05f).coerceIn(380f, 520f)
    Box(Modifier.fillMaxWidth().height(heightDp.dp)) {
        ArtImage(image ?: client.primaryImage(item, 900), Modifier.fillMaxSize())
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.4f),
                    0.22f to Color.Transparent,
                    0.6f to bg.copy(alpha = 0.55f),
                    1f to bg,
                )
            )
        )
        Column(
            Modifier.align(Alignment.BottomStart).padding(horizontal = Gutter).padding(bottom = 12.dp).widthIn(max = 640.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (item.type == "Episode" || item.type == "Season") {
                Text(
                    item.seriesName.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { item.seriesId?.let { nav.navigate(ItemRoute(it)) } },
                )
            }
            val logo = if (item.type == "Episode") null else client.logoImage(item, 800)
            if (logo != null) {
                ArtImage(logo, Modifier.fillMaxWidth(if (wide) 0.55f else 0.7f).height(88.dp), contentScale = ContentScale.Fit)
            } else {
                Text(
                    item.name.orEmpty(),
                    style = if (wide) MaterialTheme.typography.displayMedium else MaterialTheme.typography.headlineLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val episodeLine = if (item.type == "Episode") {
                listOfNotNull(
                    item.parentIndexNumber?.let { stringResource(R.string.season_x, it) },
                    item.indexNumber?.let { stringResource(R.string.episode_x, it) },
                ).joinToString(", ")
            } else null
            MetaRow(listOfNotNull(episodeLine) + metaParts(item))
        }
    }
}

@Composable
private fun SquareHeader(item: BaseItemDto, client: JellyfinClient) {
    val nav = LocalNavigator.current
    Column(
        Modifier.fillMaxWidth().statusBarsPadding().padding(top = 56.dp, start = Gutter, end = Gutter, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .widthIn(max = 280.dp)
                .fillMaxWidth(0.68f)
                .aspectRatio(1f)
                .clip(if (item.type == "MusicArtist") CircleShape else MaterialTheme.shapes.large),
        ) {
            ArtImage(client.primaryImage(item, 700), Modifier.fillMaxSize(), placeholderText = item.name)
        }
        Spacer(Modifier.height(8.dp))
        Text(item.name.orEmpty(), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis)
        if (item.type == "MusicAlbum") {
            val artist = item.albumArtists.firstOrNull()
            Text(
                artist?.name ?: item.albumArtist.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { artist?.id?.let { nav.navigate(ItemRoute(it)) } },
            )
        }
        MetaRow(metaParts(item, withEnd = false))
    }
}

@Composable
private fun PersonHeader(item: BaseItemDto, client: JellyfinClient) {
    Column(
        Modifier.fillMaxWidth().statusBarsPadding().padding(top = 56.dp, start = Gutter, end = Gutter),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(168.dp).clip(CircleShape)) {
            ArtImage(client.primaryImage(item, 400), Modifier.fillMaxSize(), placeholderText = item.name)
        }
        Spacer(Modifier.height(16.dp))
        Text(item.name.orEmpty(), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        airDateText(item)?.let {
            Text(stringResource(R.string.born_x, it), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ---------- actions ----------

@Composable
private fun ActionsBlock(d: Detail, reload: () -> Unit) {
    val context = LocalContext.current
    val c = context.container
    val nav = LocalNavigator.current
    val item = d.item
    val scope = rememberCoroutineScope()
    val downloads by c.downloads.entries.collectAsStateWithLifecycle()
    val dl = downloads.firstOrNull { it.itemId == item.id }
    var more by remember { mutableStateOf(false) }
    var tracksOpen by remember { mutableStateOf(false) }

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
    val resumeMs = item.userData?.playbackPositionTicks?.ticksToMs() ?: 0L
    val isVideo = item.mediaType == "Video" || item.type in VIDEO_TYPES
    val isAudioSet = item.type in setOf("MusicAlbum", "Playlist") && d.children.firstOrNull()?.mediaType == "Audio"

    fun toggle(block: suspend () -> Unit) {
        scope.launch {
            runCatching { block() }.onSuccess { reload() }
                .onFailure { Toast.makeText(context, it.friendlyMessage(), Toast.LENGTH_SHORT).show() }
        }
    }

    fun playVideo(fromStart: Boolean) = PlayerActivity.start(
        context, item.id,
        startMs = if (fromStart) 0 else null,
        mediaSourceId = source?.id,
        audioIndex = audio?.index,
        subtitleIndex = sub?.index ?: -1,
    )

    fun download() {
        if (dl?.status == DownloadStatus.DONE) {
            Toast.makeText(context, str(R.string.already_downloaded), Toast.LENGTH_SHORT).show()
            return
        }
        (context as? MainActivity)?.ensureNotificationPermission()
        scope.launch {
            val n = runCatching { downloadItemWith(c, item) }.getOrElse { 0 }
            Toast.makeText(context, if (n == 0) str(R.string.couldn_t_start_the_download) else plural(R.plurals.downloading_items, n, n), Toast.LENGTH_SHORT).show()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.padding(top = 4.dp)) {
        if (item.type != "Person") {
            val label = when {
                item.type == "Series" && d.nextUp != null -> {
                    val n = d.nextUp
                    if (n.parentIndexNumber != null && n.indexNumber != null) stringResource(R.string.play_s_x_e_x, n.parentIndexNumber, n.indexNumber) else stringResource(R.string.play)
                }
                resumeMs > 0 && isVideo -> stringResource(R.string.resume_from_x, formatClock(resumeMs))
                isAudioSet || item.type == "MusicArtist" -> stringResource(R.string.play)
                else -> stringResource(R.string.play)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton(
                    label,
                    icon = Icons.Filled.PlayArrow,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        when {
                            isVideo -> playVideo(false)
                            item.type == "Series" && d.nextUp != null -> nav.play(d.nextUp)
                            isAudioSet -> c.music.playItems(d.children, 0)
                            else -> nav.play(item)
                        }
                    },
                )
                item.resumeProgress()?.let { p ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f).clip(CircleShape)) { ArtProgress(p) }
                        Spacer(Modifier.width(12.dp))
                        Text(item.timeLeft().orEmpty(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            if (item.type != "Person") {
                RoundAction(Icons.Filled.Check, if (played) stringResource(R.string.watched) else stringResource(R.string.mark_watched), { toggle { c.api.setPlayed(item.id, !played) } }, active = played)
            }
            RoundAction(
                if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                stringResource(R.string.favourite),
                { toggle { c.api.setFavorite(item.id, !fav) } },
                active = fav,
            )
            if ((isVideo && item.canDownload) || item.type in setOf("Season", "Series", "MusicAlbum")) {
                RoundAction(
                    if (dl?.status == DownloadStatus.DONE) Icons.Filled.DownloadDone else Icons.Filled.Download,
                    when (dl?.status) {
                        DownloadStatus.DONE -> stringResource(R.string.downloaded)
                        DownloadStatus.RUNNING, DownloadStatus.QUEUED -> stringResource(R.string.downloading)
                        else -> stringResource(R.string.download)
                    },
                    ::download,
                    active = dl?.status == DownloadStatus.DONE,
                )
            }
            if (item.remoteTrailers.isNotEmpty()) {
                RoundAction(Icons.Outlined.Movie, stringResource(R.string.trailer), {
                    item.remoteTrailers.firstOrNull()?.url?.let { openWebLink(context, it) }
                })
            }
            RoundAction(Icons.Filled.MoreHoriz, stringResource(R.string.more), { more = true })
        }

        if (isVideo && source != null && (audioStreams.size > 1 || subStreams.isNotEmpty() || sources.size > 1)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .clickable { tracksOpen = true }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Subtitles, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(audio?.label() ?: stringResource(R.string.default_audio), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        sub?.let { stringResource(R.string.subtitles_x, it.label()) } ?: stringResource(R.string.subtitles_off),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        item.taglines.firstOrNull()?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
        item.overview?.takeIf { it.isNotBlank() }?.let {
            ExpandableText(it.replace(Regex("<br\\s*/?>"), "\n").replace(Regex("<[^>]+>"), ""))
        }
        if (item.genres.isNotEmpty()) TagRow(item.genres) { g -> nav.navigate(ListRoute(g, "genre", g)) }

        val directors = item.people.filter { it.type == "Director" }.mapNotNull { it.name }
        val writers = item.people.filter { it.type == "Writer" }.mapNotNull { it.name }
        val credits = listOfNotNull(
            directors.takeIf { it.isNotEmpty() }?.let { stringResource(R.string.directed_by_x, it.joinToString()) },
            writers.takeIf { it.isNotEmpty() }?.let { stringResource(R.string.written_by_x, it.joinToString()) },
            item.studios.mapNotNull { it.name }.takeIf { it.isNotEmpty() }?.let { stringResource(R.string.from_x, it.joinToString()) },
            if (item.type == "Episode") airDateText(item)?.let { stringResource(R.string.aired_x, it) } else null,
        )
        if (credits.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                credits.forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }

    if (tracksOpen) {
        TracksSheet(
            sources, source, audioStreams, audio, subStreams, sub,
            onSource = { source = it },
            onAudio = { audio = it },
            onSub = { sub = it },
            onDismiss = { tracksOpen = false },
        )
    }
    if (more) {
        MoreSheet(
            d, resumeMs = resumeMs, isVideo = isVideo,
            onPlayFromStart = { playVideo(true) },
            onChanged = reload,
            onDismiss = { more = false },
        )
    }
}

@Composable
private fun TracksSheet(
    sources: List<MediaSourceInfo>,
    source: MediaSourceInfo?,
    audioStreams: List<MediaStream>,
    audio: MediaStream?,
    subStreams: List<MediaStream>,
    sub: MediaStream?,
    onSource: (MediaSourceInfo) -> Unit,
    onAudio: (MediaStream) -> Unit,
    onSub: (MediaStream?) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 16.dp)) {
            if (sources.size > 1) {
                SheetTitle(stringResource(R.string.version))
                sources.forEach { s -> CheckLine(s.name ?: stringResource(R.string.default_label), s.id == source?.id) { onSource(s) } }
            }
            if (audioStreams.isNotEmpty()) {
                SheetTitle(stringResource(R.string.audio))
                audioStreams.forEach { a ->
                    CheckLine(a.label(), a.index == audio?.index, a.channelLayout) { onAudio(a) }
                }
            }
            if (subStreams.isNotEmpty()) {
                SheetTitle(stringResource(R.string.subtitles))
                CheckLine(stringResource(R.string.off), sub == null) { onSub(null) }
                subStreams.forEach { s -> CheckLine(s.label(), s.index == sub?.index) { onSub(s) } }
            }
        }
    }
}

@Composable
private fun SheetTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun CheckLine(title: String, selected: Boolean, subtitle: String? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (selected) Icon(Icons.Filled.Check, stringResource(R.string.selected), tint = MaterialTheme.colorScheme.primary)
    }
}

/** Everything that isn't the main action: shuffle, playlists, admin tools. */
@Composable
private fun MoreSheet(
    d: Detail,
    resumeMs: Long,
    isVideo: Boolean,
    onPlayFromStart: () -> Unit,
    onChanged: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val c = context.container
    val nav = LocalNavigator.current
    val item = d.item
    val session by c.sessions.active.collectAsStateWithLifecycle()
    val isAdmin = session?.account?.isAdmin == true
    val scope = rememberCoroutineScope()
    var playlist by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    if (playlist) {
        AddToPlaylistDialog(item, onDismiss = { playlist = false; onDismiss() })
        return
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 12.dp)) {
            Text(
                item.name.orEmpty(),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            fun act(block: () -> Unit) { block(); onDismiss() }
            if (resumeMs > 0 && isVideo) SheetAction(Icons.Filled.Replay, stringResource(R.string.play_from_the_beginning)) { act(onPlayFromStart) }
            if (item.isFolder && item.type != "Person") SheetAction(Icons.Filled.Shuffle, stringResource(R.string.shuffle)) { act { nav.shuffle(item) } }
            if (item.type in setOf("MusicArtist", "MusicAlbum", "Audio")) SheetAction(Icons.Outlined.AutoAwesome, stringResource(R.string.instant_mix)) { act { c.music.instantMix(item) } }
            if (item.type != "Playlist" && item.type != "Person") SheetAction(Icons.AutoMirrored.Filled.PlaylistAdd, stringResource(R.string.add_to_playlist)) { playlist = true }
            if (item.type == "Episode" && item.seriesId != null) SheetAction(Icons.Filled.Tv, stringResource(R.string.go_to_show)) { act { nav.navigate(ItemRoute(item.seriesId)) } }
            if (isAdmin) {
                SheetAction(Icons.Filled.Edit, stringResource(R.string.edit_details)) { act { nav.navigate(EditMetadataRoute(item.id)) } }
                SheetAction(Icons.Filled.Refresh, stringResource(R.string.refresh_metadata)) {
                    act {
                        scope.launch {
                            runCatching { c.api.refreshItem(item.id, replaceAll = false) }
                                .onSuccess { Toast.makeText(context, str(R.string.refreshing_metadata), Toast.LENGTH_SHORT).show(); onChanged() }
                                .onFailure { Toast.makeText(context, it.friendlyMessage(), Toast.LENGTH_SHORT).show() }
                        }
                    }
                }
            }
            if (item.canDelete) {
                SheetAction(Icons.Filled.Delete, stringResource(R.string.delete_from_server), destructive = true) { confirmDelete = true }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_x, item.name)) },
            text = { Text(stringResource(R.string.this_removes_the_files_from_the)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDismiss()
                    scope.launch {
                        runCatching { c.api.deleteItem(item.id) }
                            .onSuccess { Toast.makeText(context, str(R.string.deleted), Toast.LENGTH_SHORT).show(); nav.close(com.vknxworks.jellyctrl.ui.nav.ItemRoute::class) }
                            .onFailure { Toast.makeText(context, it.friendlyMessage(), Toast.LENGTH_SHORT).show() }
                    }
                }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.keep)) } },
        )
    }
}

@Composable
private fun SheetAction(icon: ImageVector, label: String, destructive: Boolean = false, onClick: () -> Unit) {
    GroupRow(
        label,
        icon = icon,
        iconTint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        titleColor = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        onClick = onClick,
    )
}

// ---------- seasons, episodes, tracks ----------

@Composable
private fun SeasonChips(seasons: List<BaseItemDto>, selected: String?, onSelect: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(start = Gutter, end = Gutter, top = 24.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(seasons, key = { it.id }) { s ->
            val isSel = s.id == selected
            Text(
                s.name ?: stringResource(R.string.season_x, s.indexNumber?.toString().orEmpty()),
                style = MaterialTheme.typography.labelLarge,
                color = if (isSel) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isSel) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable { onSelect(s.id) }
                    .padding(horizontal = 16.dp, vertical = 9.dp),
            )
        }
    }
}

@Composable
fun EpisodeRow(ep: BaseItemDto, client: JellyfinClient, onClick: () -> Unit, onPlay: () -> Unit, onLong: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLong)
            .padding(horizontal = Gutter, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(Modifier.width(148.dp).aspectRatio(16f / 9f).clip(MaterialTheme.shapes.small).clickable(onClick = onPlay)) {
            ArtImage(client.landscapeImage(ep, 400), Modifier.fillMaxSize(), placeholderText = ep.name)
            Box(
                Modifier.align(Alignment.Center).size(36.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.PlayArrow, stringResource(R.string.play), tint = Color.White, modifier = Modifier.size(22.dp)) }
            ep.resumeProgress()?.let { ArtProgress(it, Modifier.align(Alignment.BottomCenter)) }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    listOfNotNull(ep.indexNumber?.let { "$it." }, ep.name).joinToString(" "),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (ep.userData?.played == true) {
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Filled.Check, stringResource(R.string.watched), Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
            Text(
                listOfNotNull(ep.timeLeft() ?: formatRuntime(ep.runTimeTicks), airDateText(ep)).joinToString("   "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ep.overview?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun LazyListScope.trackList(d: Detail, client: JellyfinClient, reload: () -> Unit, onLong: (BaseItemDto) -> Unit) {
    val isPlaylist = d.item.type == "Playlist"
    item("tracks-gap") { Spacer(Modifier.height(16.dp)) }
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
    val music by c.music.state.collectAsStateWithLifecycle()
    val playing = music.itemId == track.id
    val multiDisc = d.children.mapNotNull { it.parentIndexNumber }.distinct().size > 1
    if (multiDisc && !isPlaylist && (index == 0 || d.children[index - 1].parentIndexNumber != track.parentIndexNumber)) {
        Text(
            stringResource(R.string.disc_x, track.parentIndexNumber ?: 1),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = Gutter, top = 16.dp, bottom = 4.dp),
        )
    }
    val audioTracks = d.children.filter { it.mediaType == "Audio" }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .combinedClickable(
                onClick = {
                    if (track.mediaType == "Audio") c.music.playItems(audioTracks, audioTracks.indexOf(track).coerceAtLeast(0))
                    else nav.play(track)
                },
                onLongClick = { onLong(track) },
            )
            .padding(horizontal = Gutter, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isPlaylist) {
            Box(Modifier.size(44.dp).clip(MaterialTheme.shapes.extraSmall)) { ArtImage(client.primaryImage(track, 120), Modifier.fillMaxSize()) }
        } else {
            Text(
                "${track.indexNumber ?: index + 1}",
                style = MaterialTheme.typography.labelLarge,
                color = if (playing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(28.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                track.name.orEmpty(),
                style = MaterialTheme.typography.bodyLarge,
                color = if (playing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val sub = if (isPlaylist) track.artists.joinToString().ifBlank { track.seriesName ?: track.productionYear?.toString() ?: "" }
            else track.artists.joinToString().takeIf { it != d.item.albumArtist } ?: ""
            if (sub.isNotBlank()) {
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Text(formatClock((track.runTimeTicks ?: 0).ticksToMs()), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (isPlaylist && track.playlistItemId != null) {
            IconButton(onClick = {
                scope.launch {
                    runCatching { c.api.removeFromPlaylist(d.item.id, listOf(track.playlistItemId)) }
                        .onSuccess { reload() }
                        .onFailure { Toast.makeText(context, it.friendlyMessage(), Toast.LENGTH_SHORT).show() }
                }
            }) { Icon(Icons.Filled.Delete, stringResource(R.string.remove_from_playlist), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

/**
 * Opens a link that came from the server (external IDs, trailers) in a browser. Only http/https web links
 * are opened, so server metadata can't launch other apps' deep links or local file/content URIs.
 */
private fun openWebLink(context: android.content.Context, url: String) {
    val uri = Uri.parse(url.trim())
    if (uri.scheme?.lowercase() !in setOf("http", "https") || uri.host.isNullOrBlank()) return
    runCatching { com.vknxworks.jellyctrl.ui.lock.AppLock.expectReturn(); context.startActivity(Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE)) }
}

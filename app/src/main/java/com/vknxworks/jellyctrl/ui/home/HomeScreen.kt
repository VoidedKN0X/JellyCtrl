package com.vknxworks.jellyctrl.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.jellyctrl.ui.components.LocalBottomClearance
import com.vknxworks.jellyctrl.container
import com.vknxworks.jellyctrl.data.api.BaseItemDto
import com.vknxworks.jellyctrl.data.api.JellyfinClient
import com.vknxworks.jellyctrl.data.api.latest
import com.vknxworks.jellyctrl.data.api.liveTvChannels
import com.vknxworks.jellyctrl.data.api.nextUp
import com.vknxworks.jellyctrl.data.api.resumeItems
import com.vknxworks.jellyctrl.data.api.userViews
import com.vknxworks.jellyctrl.ui.components.ArtImage
import com.vknxworks.jellyctrl.ui.components.ItemActionsSheet
import com.vknxworks.jellyctrl.ui.components.ItemRow
import com.vknxworks.jellyctrl.ui.components.LoadContent
import com.vknxworks.jellyctrl.ui.components.MessageView
import com.vknxworks.jellyctrl.ui.components.MetaRow
import com.vknxworks.jellyctrl.ui.components.PillButton
import com.vknxworks.jellyctrl.ui.components.backdropImage
import com.vknxworks.jellyctrl.ui.components.formatRuntime
import com.vknxworks.jellyctrl.ui.components.logoImage
import com.vknxworks.jellyctrl.ui.components.rememberLoader
import com.vknxworks.jellyctrl.ui.nav.ListRoute
import com.vknxworks.jellyctrl.ui.nav.LiveTvRoute
import com.vknxworks.jellyctrl.ui.nav.LocalNavigator
import com.vknxworks.jellyctrl.ui.nav.ProfileRoute
import com.vknxworks.jellyctrl.ui.nav.RemoteRoute
import com.vknxworks.jellyctrl.ui.theme.Gutter
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import androidx.compose.ui.res.stringResource
import com.vknxworks.jellyctrl.R
import com.vknxworks.jellyctrl.str
import androidx.compose.ui.res.pluralStringResource

private data class HomeData(
    val views: List<BaseItemDto>,
    val featured: List<BaseItemDto>,
    val resume: List<BaseItemDto>,
    val resumeAudio: List<BaseItemDto>,
    val nextUp: List<BaseItemDto>,
    val latest: List<Pair<BaseItemDto, List<BaseItemDto>>>,
    val liveNow: List<BaseItemDto>,
)

@Composable
fun HomeScreen() {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val session by c.sessions.active.collectAsStateWithLifecycle()
    val client = session?.client ?: return
    var menuItem by remember { mutableStateOf<BaseItemDto?>(null) }

    val loader = rememberLoader("home") {
        coroutineScope {
            val views = client.userViews()
            val resume = async { runCatching { client.resumeItems("Video") }.getOrDefault(emptyList()) }
            val resumeAudio = async { runCatching { client.resumeItems("Audio", 12) }.getOrDefault(emptyList()) }
            val next = async { runCatching { client.nextUp() }.getOrDefault(emptyList()) }
            val latest = views
                .filter { it.collectionType !in setOf("livetv", "playlists", "boxsets", "folders") }
                .map { v -> async { v to runCatching { client.latest(v.id) }.getOrDefault(emptyList()) } }
            val live = async {
                if (views.any { it.collectionType == "livetv" }) {
                    runCatching { client.liveTvChannels().filter { it.currentProgram != null }.take(20) }.getOrDefault(emptyList())
                } else emptyList()
            }
            val latestAll = latest.awaitAll()
            // Featured: the newest films and shows that have proper artwork, interleaved.
            val featured = latestAll
                .filter { (v, _) -> v.collectionType == "movies" || v.collectionType == "tvshows" || v.collectionType == null }
                .map { (_, items) -> items.filter { it.backdropImageTags.isNotEmpty() && (it.type == "Movie" || it.type == "Series") }.take(4) }
                .let { lists -> (0 until 4).flatMap { i -> lists.mapNotNull { it.getOrNull(i) } } }
                .distinctBy { it.id }
                .take(6)
            HomeData(views, featured, resume.await(), resumeAudio.await(), next.await(), latestAll, live.await())
        }
    }

    PullToRefreshBox(
        isRefreshing = loader.refreshing,
        onRefresh = { loader.reload(silent = true) },
        modifier = Modifier.fillMaxSize(),
    ) {
        LoadContent(loader) { data ->
            if (data.views.isEmpty()) {
                MessageView(
                    Icons.Outlined.VideoLibrary,
                    stringResource(R.string.no_libraries_yet),
                    stringResource(R.string.this_account_can_t_see_any),
                )
                return@LoadContent
            }
            Box(Modifier.fillMaxSize()) {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp + LocalBottomClearance.current)) {
                    if (data.featured.isNotEmpty()) {
                        item("hero") { Hero(data.featured, client) }
                    } else {
                        item("spacer") { Spacer(Modifier.statusBarsPadding().height(64.dp)) }
                    }
                    item("resume") {
                        ItemRow(
                            stringResource(R.string.continue_watching), data.resume, client, nav::openItem, landscape = true,
                            onMore = if (data.resume.size > 6) ({ nav.navigate(ListRoute(str(R.string.continue_watching), "resume")) }) else null,
                            onLongItem = { menuItem = it },
                        )
                    }
                    item("nextup") {
                        ItemRow(
                            stringResource(R.string.next_up), data.nextUp, client, nav::openItem, landscape = true,
                            onMore = if (data.nextUp.size > 6) ({ nav.navigate(ListRoute(str(R.string.next_up), "nextup")) }) else null,
                            onLongItem = { menuItem = it },
                        )
                    }
                    item("live") {
                        ItemRow(stringResource(R.string.on_tv_now), data.liveNow, client, nav::openItem, landscape = true, onMore = { nav.navigate(LiveTvRoute) })
                    }
                    item("resumeAudio") {
                        ItemRow(stringResource(R.string.keep_listening), data.resumeAudio, client, nav::openItem, onLongItem = { menuItem = it })
                    }
                    items(data.latest, key = { "latest-" + it.first.id }) { (view, items) ->
                        ItemRow(
                            stringResource(R.string.new_in_x, view.name.orEmpty()), items, client, nav::openItem,
                            onMore = { nav.openItem(view) },
                            onLongItem = { menuItem = it },
                        )
                    }
                }
                TopActions(client, Modifier.align(Alignment.TopEnd))
            }
        }
    }

    menuItem?.let { ItemActionsSheet(it, onDismiss = { menuItem = null }, onChanged = { loader.reload(silent = true) }) }
}

@Composable
private fun TopActions(client: JellyfinClient, modifier: Modifier) {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val session by c.sessions.active.collectAsStateWithLifecycle()
    val acc = session?.account
    Row(modifier.statusBarsPadding().padding(end = 8.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { nav.navigate(RemoteRoute) }) {
            Icon(Icons.Filled.Cast, stringResource(R.string.control_another_device), tint = Color.White)
        }
        IconButton(onClick = { nav.navigate(ProfileRoute) }) {
            Box(Modifier.size(32.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                if (acc?.userImageTag != null) {
                    ArtImage(client.userImageUrl(acc.userId, acc.userImageTag), Modifier.fillMaxSize())
                } else {
                    Text(acc?.userName?.take(1)?.uppercase().orEmpty(), style = MaterialTheme.typography.labelLarge, color = Color.White)
                }
            }
        }
    }
}

/** Full-bleed artwork carousel of new arrivals with one clear action. */
@Composable
private fun Hero(items: List<BaseItemDto>, client: JellyfinClient) {
    val nav = LocalNavigator.current
    val pager = rememberPagerState { items.size }
    val bg = MaterialTheme.colorScheme.background
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val wide = maxWidth >= 720.dp
        val heroHeight = if (wide) (maxWidth * 0.42f).coerceIn(380.dp, 560.dp) else (maxWidth * 1.25f).coerceIn(440.dp, 600.dp)
        Column {
            HorizontalPager(pager, Modifier.fillMaxWidth().height(heroHeight), key = { items[it].id }) { page ->
                val item = items[page]
                Box(Modifier.fillMaxSize().clickable { nav.openItem(item) }) {
                    ArtImage(client.backdropImage(item, if (wide) 1920 else 1280), Modifier.fillMaxSize())
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.verticalGradient(
                                0f to Color.Black.copy(alpha = 0.45f),
                                0.25f to Color.Transparent,
                                0.55f to bg.copy(alpha = 0.35f),
                                1f to bg,
                            )
                        )
                    )
                    Column(
                        Modifier.align(Alignment.BottomStart).padding(horizontal = Gutter).padding(bottom = 8.dp).widthIn(max = 560.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        val logo = client.logoImage(item, 800)
                        if (logo != null) {
                            ArtImage(
                                logo,
                                Modifier.fillMaxWidth(if (wide) 0.6f else 0.75f).heightIn(max = 96.dp).height(96.dp),
                                contentScale = ContentScale.Fit,
                            )
                        } else {
                            Text(
                                item.name.orEmpty(),
                                style = if (wide) MaterialTheme.typography.displayMedium else MaterialTheme.typography.displaySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        MetaRow(
                            listOfNotNull(
                                item.productionYear?.toString(),
                                item.genres.take(2).joinToString(", ").ifBlank { null },
                                if (item.type == "Movie") formatRuntime(item.runTimeTicks) else item.childCount?.let { pluralStringResource(R.plurals.count_seasons, it, it) },
                            ),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            PillButton(
                                stringResource(R.string.play),
                                onClick = { nav.play(item) },
                                icon = Icons.Filled.PlayArrow,
                                container = Color.White,
                                content = Color.Black,
                            )
                            FilledTonalButton(
                                onClick = { nav.openItem(item) },
                                shape = CircleShape,
                                modifier = Modifier.heightIn(min = 52.dp),
                                contentPadding = PaddingValues(horizontal = 20.dp),
                            ) {
                                Icon(Icons.Outlined.Info, null, Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.details), style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }
            if (items.size > 1) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    repeat(items.size) { i ->
                        val active = i == pager.currentPage
                        Box(
                            Modifier
                                .padding(horizontal = 3.dp)
                                .height(6.dp)
                                .width(if (active) 18.dp else 6.dp)
                                .clip(CircleShape)
                                .background(if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant),
                        )
                    }
                }
            }
        }
    }
}

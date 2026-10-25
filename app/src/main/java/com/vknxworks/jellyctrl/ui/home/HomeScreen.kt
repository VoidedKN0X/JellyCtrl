package com.vknxworks.jellyctrl.ui.home

import com.vknxworks.jellyctrl.ui.components.CtrlTopBar
import com.vknxworks.jellyctrl.ui.components.CtrlIconSquare
import com.vknxworks.jellyctrl.ui.components.CtrlHeroCard
import com.vknxworks.jellyctrl.ui.components.CtrlSectionLabel
import com.vknxworks.jellyctrl.ui.components.CtrlProgressBar
import com.vknxworks.jellyctrl.ui.components.CtrlButton
import com.vknxworks.jellyctrl.ui.components.CtrlSecondaryButton
import com.vknxworks.jellyctrl.ui.components.episodeCode
import com.vknxworks.jellyctrl.ui.components.timeLeft
import com.vknxworks.jellyctrl.ui.components.resumeProgress
import com.vknxworks.jellyctrl.ui.theme.CtrlColors
import com.vknxworks.jellyctrl.ui.theme.CtrlShapes
import com.vknxworks.jellyctrl.ui.theme.CtrlType
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.foundation.border
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.sp
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
                    item("topbar") { TopActions(Modifier.statusBarsPadding()) }
                    data.resume.firstOrNull()?.let { first ->
                        item("resume-hero") { ResumeHeroCard(first, client) }
                    }
                    if (data.featured.isNotEmpty()) {
                        item("hero") { Hero(data.featured, client) }
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
            }
        }
    }

    menuItem?.let { ItemActionsSheet(it, onDismiss = { menuItem = null }, onChanged = { loader.reload(silent = true) }) }
}

/** JELLY/CTRL wordmark, the remote-control button and your profile picture. */
@Composable
private fun TopActions(modifier: Modifier) {
    val nav = LocalNavigator.current
    CtrlTopBar(modifier) {
        CtrlIconSquare(Icons.Outlined.Cast, stringResource(R.string.control_another_device), onClick = { nav.navigate(RemoteRoute) })
    }
}

/** The one thing you're most likely to want: the title you were last watching, on an accent card. */
@Composable
private fun ResumeHeroCard(item: BaseItemDto, client: JellyfinClient) {
    val nav = LocalNavigator.current
    val progress = item.resumeProgress() ?: 0f
    val left = item.timeLeft()
    CtrlHeroCard(
        Modifier.fillMaxWidth().padding(horizontal = Gutter).padding(top = 10.dp, bottom = 4.dp),
        onClick = { nav.openItem(item) },
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            CtrlSectionLabel(stringResource(R.string.home_label_continue), Modifier.weight(1f), color = CtrlColors.OnAccent, strong = true)
            item.episodeCode()?.let { CtrlSectionLabel(it, color = CtrlColors.OnAccent, strong = true) }
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "${(progress * 100).toInt()}",
                style = CtrlType.BigNumber,
                color = CtrlColors.OnAccent,
            )
            Text("%", style = CtrlType.BigNumber.copy(fontSize = 28.sp, lineHeight = 32.sp), color = CtrlColors.OnAccent, modifier = Modifier.padding(bottom = 4.dp))
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.size(56.dp).clip(CtrlShapes.MainButton).background(CtrlColors.OnAccent).clickable(role = Role.Button) { nav.play(item) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.PlayArrow, stringResource(R.string.play), tint = CtrlColors.Accent, modifier = Modifier.size(28.dp))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                if (item.type == "Episode") item.seriesName ?: item.name.orEmpty() else item.name.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                color = CtrlColors.OnAccent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val sub = listOfNotNull(if (item.type == "Episode") item.name else null, left?.let { stringResource(R.string.home_x_left, it) }).joinToString(" · ")
            if (sub.isNotBlank()) Text(sub, style = CtrlType.Mono, color = CtrlColors.OnAccent.copy(alpha = 0.75f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        CtrlProgressBar({ progress }, color = CtrlColors.OnAccent, trackColor = CtrlColors.OnAccent.copy(alpha = 0.18f))
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
        val heroHeight = if (wide) (maxWidth * 0.40f).coerceIn(360.dp, 520.dp) else (maxWidth * 1.15f).coerceIn(400.dp, 540.dp)
        Column(Modifier.padding(top = 14.dp)) {
            HorizontalPager(
                pager,
                Modifier.fillMaxWidth().height(heroHeight),
                key = { items[it].id },
                contentPadding = PaddingValues(horizontal = Gutter),
                pageSpacing = 10.dp,
            ) { page ->
                val item = items[page]
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(CtrlShapes.Hero)
                        .border(1.dp, CtrlColors.Outline, CtrlShapes.Hero)
                        .clickable { nav.openItem(item) },
                ) {
                    ArtImage(client.backdropImage(item, if (wide) 1920 else 1280), Modifier.fillMaxSize())
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.45f to bg.copy(alpha = 0.25f),
                                1f to bg.copy(alpha = 0.96f),
                            )
                        )
                    )
                    Column(
                        Modifier.align(Alignment.BottomStart).padding(18.dp).widthIn(max = 560.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CtrlSectionLabel(stringResource(R.string.home_label_featured, page + 1, items.size), color = CtrlColors.AccentText)
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
                                style = if (wide) MaterialTheme.typography.displayLarge else MaterialTheme.typography.displaySmall,
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
                            CtrlButton(
                                stringResource(R.string.play),
                                onClick = { nav.play(item) },
                                icon = Icons.Filled.PlayArrow,
                                minHeight = 52.dp,
                            )
                            CtrlSecondaryButton(
                                stringResource(R.string.details),
                                onClick = { nav.openItem(item) },
                                icon = Icons.Outlined.Info,
                                minHeight = 52.dp,
                            )
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
                                .clip(CtrlShapes.Dot)
                                .background(if (active) CtrlColors.Accent else CtrlColors.ControlOutline),
                        )
                    }
                }
            }
        }
    }
}

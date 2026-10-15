package com.vknxworks.jellyctrl.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.jellyctrl.container
import com.vknxworks.jellyctrl.data.api.BaseItemDto
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
import com.vknxworks.jellyctrl.ui.components.rememberLoader
import com.vknxworks.jellyctrl.ui.nav.AccountsRoute
import com.vknxworks.jellyctrl.ui.nav.ListRoute
import com.vknxworks.jellyctrl.ui.nav.LiveTvRoute
import com.vknxworks.jellyctrl.ui.nav.LocalNavigator
import com.vknxworks.jellyctrl.ui.nav.RemoteRoute
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

private data class HomeData(
    val views: List<BaseItemDto>,
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
            HomeData(views, resume.await(), resumeAudio.await(), next.await(), latest.awaitAll(), live.await())
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {
                    Text(session?.account?.serverName ?: "JellyCtrl", fontWeight = FontWeight.SemiBold)
                },
                actions = {
                    IconButton(onClick = { nav.navigate(RemoteRoute) }) { Icon(Icons.Filled.Cast, "Control other devices") }
                    IconButton(onClick = { nav.navigate(AccountsRoute) }) {
                        val acc = session?.account
                        if (acc?.userImageTag != null) {
                            ArtImage(
                                client.userImageUrl(acc.userId, acc.userImageTag),
                                Modifier.size(30.dp).clip(CircleShape),
                            )
                        } else {
                            Icon(Icons.Filled.AccountCircle, "Accounts")
                        }
                    }
                },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = loader.refreshing,
            onRefresh = { loader.reload(silent = true) },
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            LoadContent(loader) { data ->
                if (data.views.isEmpty()) {
                    MessageView(Icons.Outlined.VideoLibrary, "No libraries", "This account can't see any libraries yet.")
                    return@LoadContent
                }
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                    item("views") {
                        ItemRow("My media", data.views, client, onItem = nav::openItem, landscape = true)
                    }
                    item("resume") {
                        ItemRow(
                            "Continue watching", data.resume, client, nav::openItem, landscape = true,
                            onMore = { nav.navigate(ListRoute("Continue watching", "resume")) },
                            onLongItem = { menuItem = it },
                        )
                    }
                    item("nextup") {
                        ItemRow(
                            "Next up", data.nextUp, client, nav::openItem, landscape = true,
                            onMore = { nav.navigate(ListRoute("Next up", "nextup")) },
                            onLongItem = { menuItem = it },
                        )
                    }
                    item("live") {
                        ItemRow("On now", data.liveNow, client, nav::openItem, landscape = true, onMore = { nav.navigate(LiveTvRoute) })
                    }
                    item("resumeAudio") {
                        ItemRow("Continue listening", data.resumeAudio, client, nav::openItem, onLongItem = { menuItem = it })
                    }
                    items(data.latest, key = { "latest-" + it.first.id }) { (view, items) ->
                        ItemRow(
                            "Latest ${view.name.orEmpty()}", items, client, nav::openItem,
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

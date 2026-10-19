package com.vknxworks.jellyctrl.ui.nav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.vknxworks.jellyctrl.container
import com.vknxworks.jellyctrl.music.MiniPlayer
import com.vknxworks.jellyctrl.music.NowPlayingScreen
import com.vknxworks.jellyctrl.ui.admin.AdminActivityScreen
import com.vknxworks.jellyctrl.ui.admin.AdminCatalogScreen
import com.vknxworks.jellyctrl.ui.admin.AdminConfigScreen
import com.vknxworks.jellyctrl.ui.admin.AdminDashboardScreen
import com.vknxworks.jellyctrl.ui.admin.AdminDevicesScreen
import com.vknxworks.jellyctrl.ui.admin.AdminKeysScreen
import com.vknxworks.jellyctrl.ui.admin.AdminLibrariesScreen
import com.vknxworks.jellyctrl.ui.admin.AdminLogScreen
import com.vknxworks.jellyctrl.ui.admin.AdminLogsScreen
import com.vknxworks.jellyctrl.ui.admin.AdminPluginsScreen
import com.vknxworks.jellyctrl.ui.admin.AdminTasksScreen
import com.vknxworks.jellyctrl.ui.admin.AdminUserScreen
import com.vknxworks.jellyctrl.ui.admin.AdminUsersScreen
import com.vknxworks.jellyctrl.ui.admin.EditMetadataScreen
import com.vknxworks.jellyctrl.ui.auth.ServerSetupFlow
import com.vknxworks.jellyctrl.ui.components.LoadingView
import com.vknxworks.jellyctrl.ui.components.LocalBottomClearance
import com.vknxworks.jellyctrl.ui.components.LocalHazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import com.vknxworks.jellyctrl.ui.detail.ItemDetailScreen
import com.vknxworks.jellyctrl.ui.downloads.DownloadsScreen
import com.vknxworks.jellyctrl.ui.favorites.FavoritesScreen
import com.vknxworks.jellyctrl.ui.home.HomeScreen
import com.vknxworks.jellyctrl.ui.library.LibraryScreen
import com.vknxworks.jellyctrl.ui.library.ListScreen
import com.vknxworks.jellyctrl.ui.livetv.LiveTvScreen
import com.vknxworks.jellyctrl.ui.photo.PhotoViewerScreen
import com.vknxworks.jellyctrl.ui.remote.RemoteScreen
import com.vknxworks.jellyctrl.ui.search.SearchScreen
import com.vknxworks.jellyctrl.ui.settings.AccountsScreen
import com.vknxworks.jellyctrl.ui.settings.ProfileScreen
import com.vknxworks.jellyctrl.ui.library.LibrariesScreen
import com.vknxworks.jellyctrl.ui.settings.QuickConnectApproveScreen
import com.vknxworks.jellyctrl.ui.settings.SettingsScreen
import com.vknxworks.jellyctrl.ui.settings.UsageStatsPrompt

@Composable
fun AppRoot() {
    val c = LocalContext.current.container
    val ready by c.sessions.ready.collectAsStateWithLifecycle()
    val active by c.sessions.active.collectAsStateWithLifecycle()
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when {
            !ready -> LoadingView()
            active == null -> ServerSetupFlow(onDone = {}, onCancel = null)
            else -> key(active!!.account.key) { MainScaffold() }
        }
    }
}

private data class TopDest(val route: Any, val item: TabItem)

private val topDestinations = listOf(
    TopDest(HomeRoute, TabItem("Home", Icons.Outlined.Home, Icons.Filled.Home)),
    TopDest(SearchRoute, TabItem("Search", Icons.Outlined.Search, Icons.Filled.Search)),
    TopDest(LibrariesRoute, TabItem("Library", Icons.Outlined.VideoLibrary, Icons.Filled.VideoLibrary)),
)

@Composable
private fun MainScaffold() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val navigator = remember(navController) { Navigator(navController, context) }
    val backStack by navController.currentBackStackEntryAsState()
    val dest = backStack?.destination
    val selectedIndex = topDestinations.indexOfFirst { top -> dest?.hierarchy?.any { it.hasRoute(top.route::class) } == true }
    val onNowPlaying = dest?.hasRoute(NowPlayingRoute::class) == true
    val settings by context.container.settings.settings.collectAsStateWithLifecycle()
    val onAdminScreen = dest != null && ADMIN_ROUTES.any { dest.hasRoute(it) }
    ProtectWindow(onAdminScreen && settings.protectAdminScreens)

    // Usage stats (only if the user opted in): the kind of screen, never its contents or the item it shows.
    val entryId = backStack?.id
    val screen = dest?.route?.let(::screenName)
    LaunchedEffect(entryId) {
        if (screen != null) context.container.telemetry.track("screen_viewed", mapOf("screen" to screen))
    }
    UsageStatsPrompt()

    // "Play in JellyCtrl" from ArrCtrl: find the movie or episode in this library and open its page.
    val link by context.container.pendingLink.collectAsStateWithLifecycle()
    LaunchedEffect(link) {
        val l = link ?: return@LaunchedEffect
        context.container.pendingLink.value = null
        val id = runCatching { context.container.api.resolve(l) }.getOrNull()
        if (id != null) {
            navController.navigate(ItemRoute(id)) { launchSingleTop = true }
        } else {
            android.widget.Toast.makeText(
                context,
                if (l is ExternalLink.Movie) "This movie isn't in your Jellyfin library yet" else "This episode isn't in your Jellyfin library yet",
                android.widget.Toast.LENGTH_LONG,
            ).show()
        }
    }

    fun selectTab(i: Int) {
        navController.navigate(topDestinations[i].route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    CompositionLocalProvider(LocalNavigator provides navigator) {
        BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            if (maxWidth >= 600.dp) {
                // Tablets and unfolded foldables: a side rail, with the mini player along the bottom.
                Row(Modifier.fillMaxSize()) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.background,
                        modifier = Modifier.fillMaxHeight().statusBarsPadding(),
                    ) {
                        Spacer(Modifier.weight(1f))
                        topDestinations.forEachIndexed { i, top ->
                            val selected = i == selectedIndex
                            NavigationRailItem(
                                selected = selected,
                                onClick = { selectTab(i) },
                                icon = { Icon(if (selected) top.item.selectedIcon else top.item.icon, contentDescription = top.item.label) },
                                label = { Text(top.item.label) },
                            )
                        }
                        Spacer(Modifier.weight(1f))
                    }
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        Box(Modifier.weight(1f).fillMaxWidth()) { AppNavHost(navController) }
                        if (!onNowPlaying) {
                            Box(Modifier.navigationBarsPadding()) { MiniPlayer(onOpen = { navigator.navigate(NowPlayingRoute) }) }
                        }
                    }
                }
            } else {
                PhoneLayout(
                    navController = navController,
                    showTabs = selectedIndex >= 0,
                    showMiniPlayer = !onNowPlaying,
                    selectedIndex = selectedIndex,
                    onSelect = ::selectTab,
                    onOpenNowPlaying = { navigator.navigate(NowPlayingRoute) },
                )
            }
        }
    }
}

/**
 * Phones: content runs edge to edge and scrolls under a floating glass island.
 * The island shows on the main tabs; pushed screens get the full height.
 */
@Composable
private fun PhoneLayout(
    navController: androidx.navigation.NavHostController,
    showTabs: Boolean,
    showMiniPlayer: Boolean,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onOpenNowPlaying: () -> Unit,
) {
    val hazeState = rememberHazeState()
    val density = LocalDensity.current
    var overlayHeight by remember { mutableStateOf(0.dp) }

    CompositionLocalProvider(
        LocalHazeState provides hazeState,
        LocalBottomClearance provides if (showTabs) overlayHeight else 0.dp,
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .hazeSource(hazeState)
                    .then(if (showTabs) Modifier else Modifier.padding(bottom = overlayHeight)),
            ) {
                AppNavHost(navController)
            }
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .onSizeChanged { overlayHeight = with(density) { it.height.toDp() } }
                    .navigationBarsPadding()
                    .padding(bottom = if (showTabs) 12.dp else 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (showMiniPlayer) MiniPlayer(onOpen = onOpenNowPlaying)
                AnimatedVisibility(
                    visible = showTabs,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 },
                ) {
                    GlassTabBar(
                        items = topDestinations.map { it.item },
                        selectedIndex = selectedIndex,
                        onSelect = onSelect,
                    )
                }
            }
        }
    }
}

/**
 * "com.vknxworks.jellyctrl.ui.nav.ItemRoute/{id}?…" -> "Item". Only the route's class name: arguments (item IDs,
 * library and person names, titles) are dropped.
 */
private fun screenName(route: String): String =
    route.substringBefore('/').substringBefore('?').substringAfterLast('.').removeSuffix("Route")

/** Server dashboard screens show API keys, logs, users and server settings. */
private val ADMIN_ROUTES = listOf(
    AdminRoute::class, AdminUsersRoute::class, AdminUserRoute::class, AdminLibrariesRoute::class, AdminTasksRoute::class,
    AdminPluginsRoute::class, AdminCatalogRoute::class, AdminLogsRoute::class, AdminLogRoute::class, AdminActivityRoute::class,
    AdminDevicesRoute::class, AdminKeysRoute::class, AdminConfigRoute::class,
)

/**
 * While [protect] is on, the window can't be captured (screenshots, screen recording, casting the screen)
 * and the recent-apps preview is blank. Dialogs opened from the screen inherit the flag.
 */
@Composable
private fun ProtectWindow(protect: Boolean) {
    // Unwrap: the composition's context can be a wrapper around the activity; a plain cast would then
    // silently skip the protection.
    var ctx: android.content.Context? = LocalContext.current
    while (ctx is android.content.ContextWrapper && ctx !is android.app.Activity) ctx = ctx.baseContext
    val activity = ctx as? android.app.Activity ?: return
    androidx.compose.runtime.DisposableEffect(protect, activity) {
        if (protect) {
            activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
            activity.setRecentsScreenshotEnabled(false)
        }
        onDispose {
            if (protect) {
                activity.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                activity.setRecentsScreenshotEnabled(true)
            }
        }
    }
}

@Composable
private fun AppNavHost(navController: androidx.navigation.NavHostController) {
    NavHost(navController, startDestination = HomeRoute) {
        composable<HomeRoute> { HomeScreen() }
        composable<SearchRoute> { SearchScreen() }
        composable<FavoritesRoute> { FavoritesScreen() }
        composable<DownloadsRoute> { DownloadsScreen() }
        composable<LibrariesRoute> { LibrariesScreen() }
        composable<ProfileRoute> { ProfileScreen() }
        composable<LibraryRoute> { LibraryScreen(it.toRoute()) }
        composable<ListRoute> { ListScreen(it.toRoute()) }
        composable<ItemRoute> { ItemDetailScreen(it.toRoute<ItemRoute>().id) }
        composable<PhotoRoute> { val r = it.toRoute<PhotoRoute>(); PhotoViewerScreen(r.parentId, r.startId) }
        composable<LiveTvRoute> { LiveTvScreen() }
        composable<SettingsRoute> { SettingsScreen() }
        composable<AccountsRoute> { AccountsScreen() }
        composable<AddAccountRoute> {
            ServerSetupFlow(onDone = { navController.popBackStack(HomeRoute, false) }, onCancel = {
                // Same guard as Navigator.back(): ignore repeat taps mid-animation and never empty the stack.
                val top = navController.currentBackStackEntry
                if (navController.previousBackStackEntry != null && top?.destination?.hasRoute(AddAccountRoute::class) == true &&
                    top.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)
                ) navController.popBackStack()
            })
        }
        composable<RemoteRoute> { RemoteScreen() }
        composable<NowPlayingRoute> { NowPlayingScreen() }
        composable<QuickConnectApproveRoute> { QuickConnectApproveScreen() }
        composable<EditMetadataRoute> { EditMetadataScreen(it.toRoute<EditMetadataRoute>().id) }

        composable<AdminRoute> { AdminDashboardScreen() }
        composable<AdminUsersRoute> { AdminUsersScreen() }
        composable<AdminUserRoute> { AdminUserScreen(it.toRoute<AdminUserRoute>().id) }
        composable<AdminLibrariesRoute> { AdminLibrariesScreen() }
        composable<AdminTasksRoute> { AdminTasksScreen() }
        composable<AdminPluginsRoute> { AdminPluginsScreen() }
        composable<AdminCatalogRoute> { AdminCatalogScreen() }
        composable<AdminLogsRoute> { AdminLogsScreen() }
        composable<AdminLogRoute> { AdminLogScreen(it.toRoute<AdminLogRoute>().name) }
        composable<AdminActivityRoute> { AdminActivityScreen() }
        composable<AdminDevicesRoute> { AdminDevicesScreen() }
        composable<AdminKeysRoute> { AdminKeysScreen() }
        composable<AdminConfigRoute> { val r = it.toRoute<AdminConfigRoute>(); AdminConfigScreen(r.section, r.title) }
    }
}

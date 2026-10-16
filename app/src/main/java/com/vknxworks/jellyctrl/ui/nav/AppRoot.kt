package com.vknxworks.jellyctrl.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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

private data class TopDest(val route: Any, val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

private val topDestinations = listOf(
    TopDest(HomeRoute, "Home", Icons.Outlined.Home, Icons.Filled.Home),
    TopDest(SearchRoute, "Search", Icons.Outlined.Search, Icons.Filled.Search),
    TopDest(LibrariesRoute, "Library", Icons.Outlined.VideoLibrary, Icons.Filled.VideoLibrary),
    TopDest(ProfileRoute, "You", Icons.Outlined.AccountCircle, Icons.Filled.AccountCircle),
)

@Composable
private fun MainScaffold() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val navigator = remember(navController) { Navigator(navController, context) }
    val backStack by navController.currentBackStackEntryAsState()
    val dest = backStack?.destination

    CompositionLocalProvider(LocalNavigator provides navigator) {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                topDestinations.forEach { top ->
                    val selected = dest?.hierarchy?.any { it.hasRoute(top.route::class) } == true
                    item(
                        selected = selected,
                        onClick = {
                            navController.navigate(top.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(if (selected) top.selectedIcon else top.icon, contentDescription = top.label) },
                        label = { Text(top.label) },
                    )
                }
            },
            navigationSuiteColors = NavigationSuiteDefaults.colors(
                navigationBarContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                navigationRailContainerColor = MaterialTheme.colorScheme.background,
            ),
            containerColor = MaterialTheme.colorScheme.background,
        ) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    AppNavHost(navController)
                }
                if (dest?.hasRoute(NowPlayingRoute::class) != true) {
                    MiniPlayer(onOpen = { navigator.navigate(NowPlayingRoute) })
                }
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
            ServerSetupFlow(onDone = { navController.popBackStack(HomeRoute, false) }, onCancel = { navController.popBackStack() })
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

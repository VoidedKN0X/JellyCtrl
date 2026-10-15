package com.vknxworks.jellyctrl.ui.nav

import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
@Serializable data object SearchRoute
@Serializable data object FavoritesRoute
@Serializable data object DownloadsRoute
@Serializable data object MoreRoute

@Serializable data class LibraryRoute(val id: String, val name: String, val collectionType: String? = null)

/** Generic item list: kind = genre | studio | resume | nextup | latest | person | tag | year. */
@Serializable data class ListRoute(
    val title: String,
    val kind: String,
    val param: String? = null,
    val parentId: String? = null,
    val itemTypes: String? = null,
)

@Serializable data class ItemRoute(val id: String)
@Serializable data class PhotoRoute(val parentId: String, val startId: String)
@Serializable data object LiveTvRoute
@Serializable data object SettingsRoute
@Serializable data object AccountsRoute
@Serializable data object AddAccountRoute
@Serializable data object RemoteRoute
@Serializable data object NowPlayingRoute
@Serializable data object QuickConnectApproveRoute
@Serializable data class EditMetadataRoute(val id: String)

// Admin
@Serializable data object AdminRoute
@Serializable data object AdminUsersRoute
@Serializable data class AdminUserRoute(val id: String)
@Serializable data object AdminLibrariesRoute
@Serializable data object AdminTasksRoute
@Serializable data object AdminPluginsRoute
@Serializable data object AdminCatalogRoute
@Serializable data object AdminLogsRoute
@Serializable data class AdminLogRoute(val name: String)
@Serializable data object AdminActivityRoute
@Serializable data object AdminDevicesRoute
@Serializable data object AdminKeysRoute
@Serializable data class AdminConfigRoute(val section: String, val title: String)

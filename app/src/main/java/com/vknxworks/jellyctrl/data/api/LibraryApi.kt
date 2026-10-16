package com.vknxworks.jellyctrl.data.api

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// Endpoints target Jellyfin 10.9+ (the userId-as-query-parameter API layout).

val LIST_FIELDS = listOf(
    "PrimaryImageAspectRatio", "Overview", "ChildCount", "RecursiveItemCount", "ParentId", "DateCreated",
    "Genres", "MediaSourceCount",
)
val DETAIL_FIELDS = listOf(
    "Overview", "Genres", "Studios", "People", "Taglines", "ExternalUrls", "ProviderIds", "MediaSources",
    "MediaStreams", "Chapters", "Trickplay", "RemoteTrailers", "Tags", "ParentId", "DateCreated",
    "PrimaryImageAspectRatio", "ChildCount", "RecursiveItemCount", "CanDelete", "CanDownload", "Path",
    "OriginalTitle", "AirTime",
)

// ---------- server & auth ----------

suspend fun JellyfinClient.publicSystemInfo(): PublicSystemInfo = get("System/Info/Public")

suspend fun JellyfinClient.publicUsers(): List<UserDto> = runCatching { get<List<UserDto>>("Users/Public") }.getOrDefault(emptyList())

suspend fun JellyfinClient.authenticateByName(username: String, password: String): AuthenticationResult =
    post(
        "Users/AuthenticateByName",
        body = buildJsonObject {
            put("Username", username)
            put("Pw", password)
        },
    )

suspend fun JellyfinClient.quickConnectEnabled(): Boolean = runCatching { get<Boolean>("QuickConnect/Enabled") }.getOrDefault(false)

suspend fun JellyfinClient.quickConnectInitiate(): QuickConnectResult = post("QuickConnect/Initiate")

suspend fun JellyfinClient.quickConnectState(secret: String): QuickConnectResult =
    get("QuickConnect/Connect", mapOf("secret" to secret))

suspend fun JellyfinClient.authenticateWithQuickConnect(secret: String): AuthenticationResult =
    post("Users/AuthenticateWithQuickConnect", body = buildJsonObject { put("Secret", secret) })

/** Approve a Quick Connect code shown on another device. */
suspend fun JellyfinClient.quickConnectAuthorize(code: String): Boolean =
    post("QuickConnect/Authorize", mapOf("code" to code, "userId" to userId))

suspend fun JellyfinClient.logout() = postUnit("Sessions/Logout")

suspend fun JellyfinClient.currentUser(): UserDto = get("Users/Me")

suspend fun JellyfinClient.currentUserRaw(): JsonObject = get("Users/Me")

suspend fun JellyfinClient.updateUserConfiguration(config: JsonObject) =
    postUnit("Users/Configuration", mapOf("userId" to requireUserId()), config)

suspend fun JellyfinClient.changeOwnPassword(current: String, new: String) =
    postUnit(
        "Users/Password",
        mapOf("userId" to requireUserId()),
        buildJsonObject {
            put("CurrentPw", current)
            put("NewPw", new)
        },
    )

// ---------- browsing ----------

suspend fun JellyfinClient.userViews(): List<BaseItemDto> =
    get<ItemsResult>("UserViews", mapOf("userId" to requireUserId())).items

suspend fun JellyfinClient.resumeItems(mediaType: String = "Video", limit: Int = 24): List<BaseItemDto> =
    get<ItemsResult>(
        "UserItems/Resume",
        mapOf(
            "userId" to requireUserId(),
            "mediaTypes" to mediaType,
            "limit" to limit,
            "fields" to LIST_FIELDS,
            "enableImageTypes" to "Primary,Backdrop,Thumb",
            "imageTypeLimit" to 1,
        ),
    ).items

suspend fun JellyfinClient.nextUp(limit: Int = 24, seriesId: String? = null): List<BaseItemDto> =
    get<ItemsResult>(
        "Shows/NextUp",
        mapOf(
            "userId" to requireUserId(),
            "limit" to limit,
            "seriesId" to seriesId,
            "fields" to LIST_FIELDS,
            "enableResumable" to false,
            "enableRewatching" to false,
        ),
    ).items

suspend fun JellyfinClient.latest(parentId: String, limit: Int = 20): List<BaseItemDto> =
    get(
        "Items/Latest",
        mapOf(
            "userId" to requireUserId(),
            "parentId" to parentId,
            "limit" to limit,
            "fields" to LIST_FIELDS,
            "groupItems" to true,
        ),
    )

data class ItemQuery(
    val parentId: String? = null,
    val includeItemTypes: List<String> = emptyList(),
    val excludeItemTypes: List<String> = emptyList(),
    val recursive: Boolean = true,
    val sortBy: List<String> = listOf("SortName"),
    val sortOrder: String = "Ascending",
    val filters: List<String> = emptyList(), // IsPlayed, IsUnplayed, IsFavorite, IsResumable
    val genres: List<String> = emptyList(),
    val years: List<Int> = emptyList(),
    val searchTerm: String? = null,
    val personIds: List<String> = emptyList(),
    val artistIds: List<String> = emptyList(),
    val albumArtistIds: List<String> = emptyList(),
    val studioIds: List<String> = emptyList(),
    val mediaTypes: List<String> = emptyList(),
    val nameStartsWith: String? = null,
    val isFavorite: Boolean? = null,
    val fields: List<String> = LIST_FIELDS,
)

suspend fun JellyfinClient.items(q: ItemQuery, startIndex: Int = 0, limit: Int = 100): ItemsResult =
    get(
        "Items",
        mapOf(
            "userId" to requireUserId(),
            "parentId" to q.parentId,
            "includeItemTypes" to q.includeItemTypes,
            "excludeItemTypes" to q.excludeItemTypes,
            "recursive" to q.recursive,
            "sortBy" to q.sortBy,
            "sortOrder" to q.sortOrder,
            "filters" to q.filters,
            "genres" to q.genres.joinToString("|").ifEmpty { null },
            "years" to q.years,
            "searchTerm" to q.searchTerm,
            "personIds" to q.personIds,
            "artistIds" to q.artistIds,
            "albumArtistIds" to q.albumArtistIds,
            "studioIds" to q.studioIds,
            "mediaTypes" to q.mediaTypes,
            "nameStartsWith" to q.nameStartsWith,
            "isFavorite" to q.isFavorite,
            "fields" to q.fields,
            "startIndex" to startIndex,
            "limit" to limit,
            "enableTotalRecordCount" to true,
            "imageTypeLimit" to 1,
        ),
    )

suspend fun JellyfinClient.item(id: String): BaseItemDto =
    get("Items/$id", mapOf("userId" to requireUserId(), "fields" to DETAIL_FIELDS))

suspend fun JellyfinClient.itemRaw(id: String): JsonObject =
    get("Items/$id", mapOf("userId" to requireUserId(), "fields" to DETAIL_FIELDS))

suspend fun JellyfinClient.seasons(seriesId: String): List<BaseItemDto> =
    get<ItemsResult>(
        "Shows/$seriesId/Seasons",
        mapOf("userId" to requireUserId(), "fields" to LIST_FIELDS),
    ).items

suspend fun JellyfinClient.episodes(seriesId: String, seasonId: String? = null): List<BaseItemDto> =
    get<ItemsResult>(
        "Shows/$seriesId/Episodes",
        mapOf(
            "userId" to requireUserId(),
            "seasonId" to seasonId,
            "fields" to LIST_FIELDS + listOf("MediaSources", "Path"),
        ),
    ).items

suspend fun JellyfinClient.similar(id: String, limit: Int = 16): List<BaseItemDto> =
    get<ItemsResult>("Items/$id/Similar", mapOf("userId" to requireUserId(), "limit" to limit, "fields" to LIST_FIELDS)).items

suspend fun JellyfinClient.specialFeatures(id: String): List<BaseItemDto> =
    runCatching { get<List<BaseItemDto>>("Items/$id/SpecialFeatures", mapOf("userId" to requireUserId())) }.getOrDefault(emptyList())

suspend fun JellyfinClient.localTrailers(id: String): List<BaseItemDto> =
    runCatching { get<List<BaseItemDto>>("Items/$id/LocalTrailers", mapOf("userId" to requireUserId())) }.getOrDefault(emptyList())

suspend fun JellyfinClient.genres(parentId: String?, includeItemTypes: List<String> = emptyList()): List<BaseItemDto> =
    get<ItemsResult>(
        "Genres",
        mapOf(
            "userId" to requireUserId(),
            "parentId" to parentId,
            "includeItemTypes" to includeItemTypes,
            "sortBy" to "SortName",
        ),
    ).items

suspend fun JellyfinClient.albumArtists(parentId: String?, startIndex: Int = 0, limit: Int = 100): ItemsResult =
    get(
        "Artists/AlbumArtists",
        mapOf(
            "userId" to requireUserId(),
            "parentId" to parentId,
            "startIndex" to startIndex,
            "limit" to limit,
            "sortBy" to "SortName",
            "fields" to LIST_FIELDS,
            "enableTotalRecordCount" to true,
        ),
    )

suspend fun JellyfinClient.instantMix(id: String, limit: Int = 100): List<BaseItemDto> =
    get<ItemsResult>("Items/$id/InstantMix", mapOf("userId" to requireUserId(), "limit" to limit, "fields" to LIST_FIELDS)).items

suspend fun JellyfinClient.lyrics(id: String): LyricDto? = runCatching { get<LyricDto>("Audio/$id/Lyrics") }.getOrNull()

// ---------- user data ----------

suspend fun JellyfinClient.setPlayed(id: String, played: Boolean) {
    val params = mapOf("userId" to requireUserId())
    if (played) postUnit("UserPlayedItems/$id", params) else deleteUnit("UserPlayedItems/$id", params)
}

suspend fun JellyfinClient.setFavorite(id: String, favorite: Boolean) {
    val params = mapOf("userId" to requireUserId())
    if (favorite) postUnit("UserFavoriteItems/$id", params) else deleteUnit("UserFavoriteItems/$id", params)
}

// ---------- playlists & collections ----------

suspend fun JellyfinClient.playlistItems(playlistId: String): List<BaseItemDto> =
    get<ItemsResult>("Playlists/$playlistId/Items", mapOf("userId" to requireUserId(), "fields" to LIST_FIELDS)).items

suspend fun JellyfinClient.createPlaylist(name: String, ids: List<String>, mediaType: String?): String {
    val res: JsonObject = post(
        "Playlists",
        body = buildJsonObject {
            put("Name", name)
            put("UserId", requireUserId())
            if (mediaType != null) put("MediaType", mediaType)
            put("Ids", buildJsonArray { ids.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } })
        },
    )
    return res.str("Id").orEmpty()
}

suspend fun JellyfinClient.addToPlaylist(playlistId: String, ids: List<String>) =
    postUnit("Playlists/$playlistId/Items", mapOf("ids" to ids, "userId" to requireUserId()))

suspend fun JellyfinClient.removeFromPlaylist(playlistId: String, entryIds: List<String>) =
    deleteUnit("Playlists/$playlistId/Items", mapOf("entryIds" to entryIds))

suspend fun JellyfinClient.createCollection(name: String, ids: List<String>): String {
    val res: JsonObject = post("Collections", mapOf("name" to name, "ids" to ids))
    return res.str("Id").orEmpty()
}

suspend fun JellyfinClient.addToCollection(collectionId: String, ids: List<String>) =
    postUnit("Collections/$collectionId/Items", mapOf("ids" to ids))

// ---------- item management ----------

suspend fun JellyfinClient.deleteItem(id: String) = deleteUnit("Items/$id")

suspend fun JellyfinClient.refreshItem(id: String, replaceAll: Boolean) = postUnit(
    "Items/$id/Refresh",
    mapOf(
        "metadataRefreshMode" to "FullRefresh",
        "imageRefreshMode" to "FullRefresh",
        "replaceAllMetadata" to replaceAll,
        "replaceAllImages" to replaceAll,
    ),
)

suspend fun JellyfinClient.updateItem(id: String, raw: JsonObject) = postUnit("Items/$id", body = raw)

/** Download URL; the worker authenticates it with the Authorization header. */
fun JellyfinClient.downloadUrl(id: String): String = url("Items/$id/Download").toString()

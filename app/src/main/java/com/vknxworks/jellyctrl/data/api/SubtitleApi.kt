package com.vknxworks.jellyctrl.data.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/** One result from the server's subtitle providers (OpenSubtitles and other plugins). */
@Serializable
data class RemoteSubtitleInfo(
    val id: String? = null,
    val threeLetterISOLanguageName: String? = null,
    val providerName: String? = null,
    val name: String? = null,
    val format: String? = null,
    val author: String? = null,
    val comment: String? = null,
    val communityRating: Float? = null,
    val downloadCount: Int? = null,
    val isHashMatch: Boolean? = null,
    val forced: Boolean? = null,
    val hearingImpaired: Boolean? = null,
    val aiTranslated: Boolean? = null,
    val machineTranslated: Boolean? = null,
)

/** A language the server knows, from /Localization/Cultures. */
@Serializable
data class CultureDto(
    val name: String? = null,
    val displayName: String? = null,
    val twoLetterISOLanguageName: String? = null,
    val threeLetterISOLanguageName: String? = null,
    val threeLetterISOLanguageNames: List<String> = emptyList(),
)

/** Asks every subtitle provider plugin on the server for subtitles in [language] (a three-letter code such as "eng"). */
suspend fun JellyfinClient.searchRemoteSubtitles(itemId: String, language: String): List<RemoteSubtitleInfo> =
    get("Items/$itemId/RemoteSearch/Subtitles/$language")

/** Has the server download [subtitleId] and store it with the item. The item's media streams update shortly after. */
suspend fun JellyfinClient.downloadRemoteSubtitle(itemId: String, subtitleId: String) =
    postUnit("Items/$itemId/RemoteSearch/Subtitles/$subtitleId")

suspend fun JellyfinClient.cultures(): List<CultureDto> = get("Localization/Cultures")

/**
 * How many subtitle providers the server has installed, or null when the server doesn't say
 * (older versions, or this account can't ask). Taken from the library options the server offers.
 */
suspend fun JellyfinClient.subtitleProviderCount(): Int? = runCatching {
    val options: JsonObject = get("Libraries/AvailableOptions", mapOf("libraryContentType" to "movies", "isNewLibrary" to false))
    (options["SubtitleFetchers"] as? JsonArray)?.size
}.getOrNull()

/** The user's preferred subtitle language from their Jellyfin settings ("" when not set). */
suspend fun JellyfinClient.preferredSubtitleLanguage(): String = runCatching {
    (currentUserRaw()["Configuration"] as? JsonObject)?.str("SubtitleLanguagePreference").orEmpty()
}.getOrDefault("")

// Indexes can move when a file is added, so streams are told apart by their file (or, without one, their description).
private fun MediaStream.subtitleKey(): String = path ?: "${displayTitle.orEmpty()}|${title.orEmpty()}|${language.orEmpty()}|${codec.orEmpty()}"

private fun BaseItemDto.subtitleStreams(sourceId: String?): List<MediaStream> {
    val source = mediaSources.firstOrNull { sourceId == null || it.id == sourceId } ?: mediaSources.firstOrNull()
    return (source?.mediaStreams ?: mediaStreams).filter { it.type == "Subtitle" }
}

/**
 * Downloads [subtitleId] to the server for [itemId], then waits (up to about 15 seconds) for the item to list it.
 * Returns the new subtitle stream of media source [sourceId], or null when it hasn't shown up yet.
 */
suspend fun JellyfinClient.downloadSubtitleAndFind(itemId: String, sourceId: String?, subtitleId: String): MediaStream? {
    val before = runCatching { item(itemId).subtitleStreams(sourceId).map { it.subtitleKey() }.toSet() }.getOrDefault(emptySet())
    downloadRemoteSubtitle(itemId, subtitleId)
    repeat(10) { attempt ->
        if (attempt > 0) kotlinx.coroutines.delay(1_500)
        val streams = runCatching { item(itemId).subtitleStreams(sourceId) }.getOrNull() ?: return@repeat
        streams.firstOrNull { it.isExternal && it.subtitleKey() !in before }?.let { return it }
    }
    return null
}

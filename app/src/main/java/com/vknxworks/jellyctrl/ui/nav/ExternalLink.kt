package com.vknxworks.jellyctrl.ui.nav

import android.content.Intent
import com.vknxworks.jellyctrl.data.api.BaseItemDto
import com.vknxworks.jellyctrl.data.api.JellyfinClient
import com.vknxworks.jellyctrl.data.api.episodes
import com.vknxworks.jellyctrl.data.api.itemsWithProviderIds
import kotlin.math.abs

/** The ids another app knows for a movie or series. Any of them can be missing. */
data class ExternalIds(val tmdb: String? = null, val tvdb: String? = null, val imdb: String? = null) {
    val any: Boolean get() = tmdb != null || tvdb != null || imdb != null
}

/**
 * A link from another Ctrl app (ArrCtrl's "Play in JellyCtrl"), e.g.
 * jellyctrl://item?type=movie&tmdb=603&imdb=tt0133093&title=The%20Matrix&year=1999 or
 * jellyctrl://item?type=episode&tvdb=81189&tmdb=1396&title=Breaking%20Bad&season=1&episode=1 (ids and title of the series).
 * Only opens an item's page; nothing plays or changes without you.
 */
sealed interface ExternalLink {
    val ids: ExternalIds
    val title: String?
    val year: Int?

    data class Movie(override val ids: ExternalIds, override val title: String?, override val year: Int?) : ExternalLink
    data class Episode(override val ids: ExternalIds, override val title: String?, override val year: Int?, val season: Int, val episode: Int) : ExternalLink

    companion object {
        private val NUMBER = Regex("^\\d{1,10}$")
        private val IMDB = Regex("^tt\\d{1,10}$")
        private val YEAR = Regex("^\\d{4}$")

        fun from(intent: Intent?): ExternalLink? {
            val uri = intent?.takeIf { it.action == Intent.ACTION_VIEW }?.data ?: return null
            if (uri.scheme != "jellyctrl" || uri.host != "item") return null
            fun num(name: String) = uri.getQueryParameter(name)?.takeIf { NUMBER.matches(it) }
            val ids = ExternalIds(
                tmdb = num("tmdb"),
                tvdb = num("tvdb"),
                imdb = uri.getQueryParameter("imdb")?.takeIf { IMDB.matches(it) },
            )
            val title = uri.getQueryParameter("title")?.trim()?.take(300)?.ifBlank { null }
            val year = uri.getQueryParameter("year")?.takeIf { YEAR.matches(it) }?.toInt()
            if (!ids.any && title == null) return null
            return when (uri.getQueryParameter("type")) {
                "movie" -> Movie(ids, title, year)
                "episode" -> {
                    val season = num("season")?.toIntOrNull() ?: return null
                    val episode = num("episode")?.toIntOrNull() ?: return null
                    Episode(ids, title, year, season, episode)
                }
                else -> null
            }
        }
    }
}

private fun BaseItemDto.matches(ids: ExternalIds): Boolean = providerIds.any { (key, value) ->
    value != null && when (key.lowercase()) {
        "tmdb" -> value == ids.tmdb
        "tvdb" -> value == ids.tvdb
        "imdb" -> value.equals(ids.imdb, ignoreCase = true)
        else -> false
    }
}

/**
 * Finds a movie or series. First a title search (quick), matched by any shared id; if no id matches, a single result
 * with the same title and year. Then, if the link has ids, every item of that type compared by id.
 * Jellyfin can't filter by an id's value, and newer Jellyfin versions often have no TVDB ids at all, so all ids count.
 */
private suspend fun JellyfinClient.find(type: String, ids: ExternalIds, title: String?, year: Int?): BaseItemDto? {
    if (title != null) {
        val hits = itemsWithProviderIds(type, title, 0, 50).items
        hits.firstOrNull { it.matches(ids) }?.let { return it }
        val same = hits.filter { h ->
            h.name.equals(title, ignoreCase = true) && (year == null || h.productionYear == null || abs(h.productionYear - year) <= 1)
        }
        if (same.size == 1) return same.single()
    }
    if (!ids.any) return null
    var start = 0
    while (true) {
        val page = itemsWithProviderIds(type, null, start, 500)
        page.items.firstOrNull { it.matches(ids) }?.let { return it }
        start += page.items.size
        if (page.items.isEmpty() || (page.totalRecordCount > 0 && start >= page.totalRecordCount)) return null
    }
}

/** The Jellyfin item id the link points at, or null when it isn't in your library. Throws when Jellyfin can't be reached. */
suspend fun JellyfinClient.resolve(link: ExternalLink): String? = when (link) {
    is ExternalLink.Movie -> find("Movie", link.ids, link.title, link.year)?.id
    is ExternalLink.Episode -> find("Series", link.ids, link.title, link.year)?.id?.let { seriesId ->
        // The episode if it's there; otherwise the series page, which is the next best thing.
        episodes(seriesId).firstOrNull { it.parentIndexNumber == link.season && it.indexNumber == link.episode }?.id ?: seriesId
    }
}

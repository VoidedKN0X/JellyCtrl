package com.vknxworks.jellyctrl.ui.nav

import android.content.Intent
import com.vknxworks.jellyctrl.data.api.JellyfinClient
import com.vknxworks.jellyctrl.data.api.episodes
import com.vknxworks.jellyctrl.data.api.findByProviderId

/**
 * A link from another Ctrl app (ArrCtrl's "Play in JellyCtrl"):
 * jellyctrl://item?type=movie&tmdb=603 or jellyctrl://item?type=episode&tvdb=81189&season=1&episode=1.
 * Only opens an item's page; nothing plays or changes without you.
 */
sealed interface ExternalLink {
    data class Movie(val tmdb: String) : ExternalLink
    data class Episode(val tvdb: String, val season: Int, val episode: Int) : ExternalLink

    companion object {
        private val NUMBER = Regex("^\\d{1,10}$")

        fun from(intent: Intent?): ExternalLink? {
            val uri = intent?.takeIf { it.action == Intent.ACTION_VIEW }?.data ?: return null
            if (uri.scheme != "jellyctrl" || uri.host != "item") return null
            fun q(name: String) = uri.getQueryParameter(name)?.takeIf { NUMBER.matches(it) }
            return when (uri.getQueryParameter("type")) {
                "movie" -> q("tmdb")?.let { Movie(it) }
                "episode" -> {
                    val tvdb = q("tvdb") ?: return null
                    val season = q("season")?.toIntOrNull() ?: return null
                    val episode = q("episode")?.toIntOrNull() ?: return null
                    Episode(tvdb, season, episode)
                }
                else -> null
            }
        }
    }
}

/** The Jellyfin item id the link points at, or null when it isn't in your library. */
suspend fun JellyfinClient.resolve(link: ExternalLink): String? = when (link) {
    is ExternalLink.Movie -> findByProviderId("Movie", "Tmdb", link.tmdb)?.id
    is ExternalLink.Episode -> {
        val series = findByProviderId("Series", "Tvdb", link.tvdb)
        series?.id?.let { sid ->
            // The episode if it's there; otherwise the series page, which is the next best thing.
            episodes(sid).firstOrNull { it.parentIndexNumber == link.season && it.indexNumber == link.episode }?.id ?: sid
        }
    }
}

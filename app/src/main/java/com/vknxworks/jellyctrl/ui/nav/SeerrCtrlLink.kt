package com.vknxworks.jellyctrl.ui.nav

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.vknxworks.jellyctrl.data.api.BaseItemDto
import com.vknxworks.jellyctrl.ui.lock.AppLock

/**
 * "Request in SeerrCtrl": hands a movie or series to SeerrCtrl (the Ctrl app for Seerr and Jellyseerr) by its
 * TMDB id, as seerrctrl://movie/<tmdbId> or seerrctrl://tv/<tmdbId> (the links SeerrCtrl's manifest accepts).
 * Only the id leaves JellyCtrl, and only to SeerrCtrl's own package. Nothing is requested here: SeerrCtrl
 * opens the title's page.
 */
object SeerrCtrlLink {
    const val PACKAGE = "com.vknxworks.seerrctrl"
    private val NUMBER = Regex("^[1-9]\\d{0,9}$")

    private fun BaseItemDto.tmdb(): String? =
        providerIds.entries.firstOrNull { it.key.equals("Tmdb", ignoreCase = true) }?.value?.trim()?.takeIf { NUMBER.matches(it) }

    /** The link for [item], or null when it isn't a movie or series, or Jellyfin has no TMDB id for it. */
    fun linkFor(item: BaseItemDto): Uri? {
        val kind = when (item.type) {
            "Movie" -> "movie"
            "Series" -> "tv"
            else -> return null
        }
        val tmdb = item.tmdb() ?: return null
        return Uri.Builder().scheme("seerrctrl").authority(kind).appendPath(tmdb).build()
    }

    /** SeerrCtrl is installed. */
    fun installed(context: Context): Boolean =
        runCatching { context.packageManager.getPackageInfo(PACKAGE, 0); true }.getOrDefault(false)

    /** Opens [link] in SeerrCtrl; a SeerrCtrl too old for the link just opens. */
    fun open(context: Context, link: Uri): Boolean {
        val view = Intent(Intent.ACTION_VIEW, link).setPackage(PACKAGE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val target = if (runCatching { view.resolveActivity(context.packageManager) }.getOrNull() != null) view
        else context.packageManager.getLaunchIntentForPackage(PACKAGE)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) ?: return false
        return runCatching {
            AppLock.expectReturn()
            context.startActivity(target)
        }.isSuccess
    }
}

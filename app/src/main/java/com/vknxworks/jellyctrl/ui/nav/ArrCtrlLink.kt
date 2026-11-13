package com.vknxworks.jellyctrl.ui.nav

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.vknxworks.jellyctrl.data.api.BaseItemDto
import com.vknxworks.jellyctrl.ui.lock.AppLock

/**
 * "Open in ArrCtrl": hands a movie or series to ArrCtrl (the Ctrl app for Radarr and Sonarr) by its ids.
 * arrctrl://movie?tmdb=<id> and arrctrl://series?tvdb=<id>&tmdb=<id> (whichever ids Jellyfin has).
 * Only ids leave JellyCtrl, and only to ArrCtrl's own package.
 */
object ArrCtrlLink {
    const val PACKAGE = "com.vknxworks.arrctrl"
    private val NUMBER = Regex("^\\d{1,10}$")

    private fun BaseItemDto.providerId(name: String): String? =
        providerIds.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value?.trim()?.takeIf { NUMBER.matches(it) }

    /** The link for [item], or null when it isn't a movie or series, or Jellyfin has no usable id for it. */
    fun linkFor(item: BaseItemDto): Uri? = when (item.type) {
        "Movie" -> item.providerId("Tmdb")?.let { tmdb ->
            Uri.Builder().scheme("arrctrl").authority("movie").appendQueryParameter("tmdb", tmdb).build()
        }
        "Series" -> {
            val tvdb = item.providerId("Tvdb")
            val tmdb = item.providerId("Tmdb")
            if (tvdb == null && tmdb == null) null
            else Uri.Builder().scheme("arrctrl").authority("series").apply {
                tvdb?.let { appendQueryParameter("tvdb", it) }
                tmdb?.let { appendQueryParameter("tmdb", it) }
            }.build()
        }
        else -> null
    }

    private fun intent(link: Uri): Intent =
        Intent(Intent.ACTION_VIEW, link).setPackage(PACKAGE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** ArrCtrl is installed. */
    fun installed(context: Context): Boolean =
        runCatching { context.packageManager.getPackageInfo(PACKAGE, 0); true }.getOrDefault(false)

    /** Opens [link] in ArrCtrl; an ArrCtrl too old for the link just opens. */
    fun open(context: Context, link: Uri): Boolean {
        val view = intent(link)
        val target = if (runCatching { view.resolveActivity(context.packageManager) }.getOrNull() != null) view
        else context.packageManager.getLaunchIntentForPackage(PACKAGE)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) ?: return false
        return runCatching {
            AppLock.expectReturn()
            context.startActivity(target)
        }.isSuccess
    }
}

package com.vknxworks.jellyctrl.ui.nav

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import com.vknxworks.jellyctrl.container
import com.vknxworks.jellyctrl.data.api.BaseItemDto
import com.vknxworks.jellyctrl.player.PlayerActivity
import kotlin.reflect.KClass

/** One place that knows where each kind of item opens and how it plays. */
class Navigator(val nav: NavHostController, private val context: Context) {

    /** Opens a screen from a tap. Taps on a screen that's still animating in or already leaving are ignored. */
    fun navigate(route: Any) {
        if (!isCurrentResumed()) return
        nav.navigate(route) { launchSingleTop = true }
    }

    /**
     * Back from the screen the user is looking at. Repeat taps while a screen change is still animating are
     * ignored, and the bottom screen is never popped (an empty back stack left a black screen).
     */
    fun back() {
        if (nav.previousBackStackEntry == null) return
        if (!isCurrentResumed()) return
        nav.popBackStack()
    }

    /**
     * Closes the top screen only if it is still a [route] screen, for screens that finish on their own after
     * a network call (save, delete). If the user already left, nothing else gets popped.
     */
    fun close(route: KClass<*>) {
        if (nav.previousBackStackEntry == null) return
        if (nav.currentBackStackEntry?.destination?.hasRoute(route) != true) return
        nav.popBackStack()
    }

    private fun isCurrentResumed(): Boolean =
        nav.currentBackStackEntry?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) != false

    fun openItem(item: BaseItemDto) {
        when (item.type) {
            "CollectionFolder", "UserView" -> navigate(LibraryRoute(item.id, item.name ?: "", item.collectionType))
            "Folder" -> navigate(LibraryRoute(item.id, item.name ?: "", "folders"))
            "Photo" -> navigate(PhotoRoute(item.parentId ?: item.id, item.id))
            "TvChannel", "Program" -> play(item)
            "Genre", "MusicGenre" -> navigate(ListRoute(item.name ?: "Genre", "genre", item.name))
            "Studio" -> navigate(ListRoute(item.name ?: "Studio", "studio", item.id))
            else -> navigate(ItemRoute(item.id))
        }
    }

    /** Start playback. Video opens the player; audio goes to the music queue. */
    fun play(item: BaseItemDto, fromStart: Boolean = false) {
        when {
            item.type == "Program" -> item.channelId?.let { PlayerActivity.start(context, it, startMs = 0) }
            item.mediaType == "Audio" || item.type in AUDIO_CONTAINERS -> context.container.music.playItem(item)
            item.type == "Series" || item.type == "Season" || item.type == "BoxSet" || item.type == "Playlist" && item.mediaType != "Audio" ->
                PlayerActivity.startContainer(context, item.id)
            item.mediaType == "Video" || item.type in BaseItemDto.PLAYABLE_TYPES -> PlayerActivity.start(
                context,
                item.id,
                startMs = if (fromStart) 0 else null,
            )
            else -> Toast.makeText(context, "This item can't be played", Toast.LENGTH_SHORT).show()
        }
    }

    fun shuffle(item: BaseItemDto) {
        if (item.mediaType == "Audio" || item.type in AUDIO_CONTAINERS || item.collectionType == "music") {
            context.container.music.playContainer(item, shuffle = true)
        } else {
            PlayerActivity.startContainer(context, item.id, shuffle = true)
        }
    }

    companion object {
        val AUDIO_CONTAINERS = setOf("MusicAlbum", "MusicArtist", "Audio", "AudioBook")
    }
}

val LocalNavigator = staticCompositionLocalOf<Navigator> { error("No navigator") }

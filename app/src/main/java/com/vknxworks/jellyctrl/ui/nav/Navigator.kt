package com.vknxworks.jellyctrl.ui.nav

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavHostController
import com.vknxworks.jellyctrl.container
import com.vknxworks.jellyctrl.data.api.BaseItemDto
import com.vknxworks.jellyctrl.player.PlayerActivity

/** One place that knows where each kind of item opens and how it plays. */
class Navigator(val nav: NavHostController, private val context: Context) {

    fun navigate(route: Any) = nav.navigate(route) { launchSingleTop = true }

    fun back() {
        nav.popBackStack()
    }

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

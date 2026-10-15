package com.vknxworks.jellyctrl.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.StateFlow

data class EngineState(
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val bufferedMs: Long = 0,
    val playWhenReady: Boolean = true,
    val buffering: Boolean = true,
    val ended: Boolean = false,
    val error: String? = null,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val speed: Float = 1f,
)

/** An external subtitle file the engine should side-load. [jellyfinIndex] is the MediaStream index. */
data class ExternalSub(val jellyfinIndex: Int, val url: String, val codec: String?, val language: String?, val title: String?)

sealed interface SubSelection {
    data object Off : SubSelection
    /** nth embedded subtitle track in file order (0-based). */
    data class Embedded(val ordinal: Int) : SubSelection
    data class External(val jellyfinIndex: Int) : SubSelection
}

enum class ResizeMode(val label: String) { FIT("Fit"), ZOOM("Crop"), FILL("Stretch") }

data class LoadRequest(
    val url: String,
    val startMs: Long,
    val isHls: Boolean,
    val externalSubs: List<ExternalSub>,
    /** nth embedded audio track to select (0-based), null to keep the file default. */
    val audioOrdinal: Int?,
    val subtitle: SubSelection,
    val title: String,
)

interface PlayerEngine {
    val name: String
    val state: StateFlow<EngineState>

    fun load(request: LoadRequest)
    fun play()
    fun pause()
    fun seekTo(ms: Long)
    fun setSpeed(speed: Float)
    fun selectAudio(ordinal: Int)
    fun selectSubtitle(selection: SubSelection)
    fun release()

    @Composable
    fun VideoSurface(modifier: Modifier, resizeMode: ResizeMode)
}

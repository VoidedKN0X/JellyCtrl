package com.vknxworks.jellyctrl.music

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.vknxworks.jellyctrl.MainActivity
import com.vknxworks.jellyctrl.container
import com.vknxworks.jellyctrl.data.api.ProgressReport
import com.vknxworks.jellyctrl.data.api.msToTicks
import com.vknxworks.jellyctrl.data.api.reportProgress
import com.vknxworks.jellyctrl.data.api.reportStart
import com.vknxworks.jellyctrl.data.api.reportStopped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Background music playback with a media notification, lock-screen and Bluetooth controls. */
class MusicService : MediaSessionService() {

    private var session: MediaSession? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var reportedItemId: String? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setMediaSourceFactory(DefaultMediaSourceFactory(DefaultDataSource.Factory(this, OkHttpDataSource.Factory(container.http))))
            .build()

        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        session = MediaSession.Builder(this, player).setSessionActivity(openApp).build()

        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                stopReport(player)
                if (mediaItem != null) startReport(player, mediaItem)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                progress(player)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) stopReport(player)
            }

            override fun onEvents(player: Player, events: Player.Events) {
                updateRunning(player)
            }
        })

        scope.launch {
            while (isActive) {
                delay(10_000)
                if (player.isPlaying) progress(player)
            }
        }
    }

    private fun report(player: Player, itemId: String, block: suspend com.vknxworks.jellyctrl.data.api.JellyfinClient.(ProgressReport) -> Unit) {
        val client = container.sessions.active.value?.client ?: return
        val r = ProgressReport(
            itemId = itemId,
            mediaSourceId = itemId,
            playSessionId = null,
            positionTicks = player.currentPosition.coerceAtLeast(0).msToTicks(),
            isPaused = !player.isPlaying,
            playMethod = "DirectPlay",
            audioStreamIndex = null,
            subtitleStreamIndex = null,
        )
        container.appScope.launch { runCatching { client.block(r) } }
    }

    private fun startReport(player: Player, item: MediaItem) {
        val id = item.mediaId.takeIf { it.isNotBlank() } ?: return
        reportedItemId = id
        report(player, id) { reportStart(it) }
    }

    private fun progress(player: Player) {
        val id = reportedItemId ?: player.currentMediaItem?.mediaId?.also { startReport(player, player.currentMediaItem!!) } ?: return
        report(player, id) { reportProgress(it) }
    }

    private fun stopReport(player: Player) {
        val id = reportedItemId ?: return
        reportedItemId = null
        report(player, id) { reportStopped(it) }
    }

    /**
     * Only hand the session to this app, controllers the platform marks as trusted (apps holding
     * MEDIA_CONTENT_CONTROL or notification access) and apps built into the phone's firmware
     * (system UI, Bluetooth, car and voice controls). Any other app that binds to the exported
     * service is refused.
     */
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        session?.takeIf { isAllowedController(controllerInfo) }

    private fun isAllowedController(info: MediaSession.ControllerInfo): Boolean {
        val pkg = info.packageName
        val uid = info.uid
        if (uid == android.os.Process.myUid()) return true
        if (info.isTrusted) return true
        if (uid == android.os.Process.SYSTEM_UID) return true
        // The package name is reported by the caller itself, so check it against the binder UID
        // before trusting it as a built-in app.
        val pkgVerified = uid >= 0 && packageManager.getPackagesForUid(uid)?.contains(pkg) == true
        return pkgVerified && isSystemApp(pkg)
    }

    private fun isSystemApp(pkg: String): Boolean = runCatching {
        val ai = packageManager.getApplicationInfo(pkg, 0)
        ai.flags and (android.content.pm.ApplicationInfo.FLAG_SYSTEM or android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
    }.getOrDefault(false)

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        session?.run {
            stopReport(player)
            player.release()
            release()
        }
        session = null
        scope.cancel()
        if (running) {
            running = false
            // With the app locked, the sign-in was only kept in memory for playback.
            container.sessions.mediaStopped()
        }
        super.onDestroy()
    }

    /** Music is playing or paused with a queue (its notification is up): that keeps the sign-in while locked. */
    private fun updateRunning(player: Player) {
        val now = player.mediaItemCount > 0 && player.playbackState != Player.STATE_IDLE && player.playbackState != Player.STATE_ENDED
        if (now == running) return
        running = now
        if (!now) container.sessions.mediaStopped()
    }

    companion object {
        /** Music is playing or paused with a queue: it keeps the sign-in in memory while the app is locked. */
        @Volatile var running = false
            private set
    }
}

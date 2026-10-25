package com.vknxworks.jellyctrl.player

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Bundle
import android.util.Rational
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vknxworks.jellyctrl.container
import com.vknxworks.jellyctrl.ui.lock.AppLockOverlay
import com.vknxworks.jellyctrl.ui.lock.AppLock
import com.vknxworks.jellyctrl.ui.lock.LockWindow
import com.vknxworks.jellyctrl.ui.lock.lockCovering
import com.vknxworks.jellyctrl.ui.lock.lockLocked
import com.vknxworks.jellyctrl.ui.theme.JellyCtrlTheme
import com.vknxworks.jellyctrl.R
import com.vknxworks.jellyctrl.str

class PlayerActivity : ComponentActivity() {

    private val vm: PlayerViewModel by viewModels {
        viewModelFactory {
            initializer { PlayerViewModel(applicationContext.container, applicationContext, argsFrom(intent)) }
        }
    }

    private var inPip by mutableStateOf(false)
    private var playingForPip = false

    private val pipReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_PIP_TOGGLE) vm.togglePlay()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemBars()
        if (resources.configuration.smallestScreenWidthDp < 600) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
        ContextCompat.registerReceiver(this, pipReceiver, IntentFilter(ACTION_PIP_TOGGLE), ContextCompat.RECEIVER_NOT_EXPORTED)

        val c = applicationContext.container
        setContent {
            JellyCtrlTheme {
                val engine by vm.engine.collectAsState()
                val st = engine?.state?.collectAsState()?.value ?: EngineState()
                LaunchedEffect(st.playWhenReady, st.videoWidth, st.videoHeight, vm.loading) {
                    playingForPip = st.playWhenReady && !vm.loading
                    updatePipParams(st)
                }
                LaunchedEffect(vm.finished) { if (vm.finished) finish() }
                val settings by c.settings.settings.collectAsState()
                val stored by c.settings.stored.collectAsState(initial = null)
                // Until the settings are read, trust the synchronous read made at start-up.
                val lockSetting = stored?.appLock ?: c.lockAtStart
                val covered = lockCovering(lockSetting, c.vault)
                // The video follows "Hide in screenshots" and "Hide in recent apps"; it's always hidden under the lock screen.
                val secure = settings.hideScreenshots || covered
                // With the app lock on, Recents always shows a blank card (the snapshot is taken before it locks).
                com.vknxworks.jellyctrl.ui.lock.WindowPrivacy(this@PlayerActivity, secure, hideRecents = settings.hideRecents || lockSetting || c.vault.enabled)
                Box(Modifier.fillMaxSize()) {
                    // The player stays underneath the lock screen (paused), hidden from TalkBack too.
                    Box(if (covered) Modifier.fillMaxSize().clearAndSetSemantics { } else Modifier.fillMaxSize()) {
                        PlayerScreen(
                            vm = vm,
                            inPip = inPip,
                            onBack = { finish() },
                            onEnterPip = { enterPip() },
                            rotationLocked = requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
                            onToggleRotationLock = { toggleRotationLock() },
                        )
                    }
                    // Locked: the lock screen gets a window of its own, so the player's sheets that were open can't
                    // stay above it and a hardware keyboard can't reach the controls underneath. The video itself
                    // stays where it is, so it carries on after unlocking.
                    val locked = lockLocked(lockSetting, c.vault)
                    if (!inPip) {
                        if (locked) LockWindow { AppLockOverlay(lockSetting) } else AppLockOverlay(lockSetting)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // A different video was started while this one is open (e.g. from PiP): start fresh.
        setIntent(intent)
        viewModelStore.clear()
        recreate()
    }

    private fun toggleRotationLock() {
        requestedOrientation = if (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            else ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        } else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun pipParams(st: EngineState): PictureInPictureParams {
        val w = st.videoWidth.takeIf { it > 0 } ?: 16
        val h = st.videoHeight.takeIf { it > 0 } ?: 9
        val ratio = Rational(w, h).let { r ->
            // Android only accepts aspect ratios between 1:2.39 and 2.39:1.
            when {
                r.toFloat() > 2.39f -> Rational(239, 100)
                r.toFloat() < 1 / 2.39f -> Rational(100, 239)
                else -> r
            }
        }
        val playing = st.playWhenReady
        val action = RemoteAction(
            Icon.createWithResource(this, if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play),
            if (playing) str(R.string.pause) else str(R.string.play),
            if (playing) str(R.string.pause) else str(R.string.play),
            PendingIntent.getBroadcast(
                this, 0, Intent(ACTION_PIP_TOGGLE).setPackage(packageName),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            ),
        )
        return PictureInPictureParams.Builder()
            .setAspectRatio(ratio)
            .setActions(listOf(action))
            .setAutoEnterEnabled(applicationContext.container.settings.settings.value.autoPip && playingForPip)
            .setSeamlessResizeEnabled(true)
            .build()
    }

    private fun updatePipParams(st: EngineState) {
        runCatching { setPictureInPictureParams(pipParams(st)) }
    }

    private fun enterPip() {
        val st = vm.engine.value?.state?.value ?: EngineState()
        runCatching { enterPictureInPictureMode(pipParams(st)) }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        inPip = isInPictureInPictureMode
        val c = applicationContext.container
        val timeout = c.settings.settings.value.lockTimeout.millis
        if (isInPictureInPictureMode) {
            // The app stays "on screen" in picture-in-picture, so the app-wide check never sees it leave: count
            // this as leaving. "As soon as you leave" locks now; the video keeps playing in its small window
            // (it keeps the sign-in it needs) and the lock screen shows when you open it full screen.
            AppLock.onBackground(c.lockEnabled, timeout)
        } else if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            // Opened full screen again (when closed, the activity has already stopped): check the time away.
            AppLock.onForeground(c.lockEnabled, timeout)
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Leaving for picture-in-picture: lock before the small window appears.
        val c = applicationContext.container
        if (c.settings.settings.value.autoPip && playingForPip) {
            AppLock.onBackground(c.lockEnabled, c.settings.settings.value.lockTimeout.millis)
        }
    }

    override fun onStart() {
        super.onStart()
        visible++
    }

    override fun onStop() {
        super.onStop()
        // Leaving the app without PiP pauses playback (the progress gets reported on pause).
        if (!isInPictureInPictureMode) vm.engine.value?.pause()
        visible = (visible - 1).coerceAtLeast(0)
        // With the app locked, the sign-in was only kept in memory for this video.
        applicationContext.container.sessions.mediaStopped()
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(pipReceiver) }
        super.onDestroy()
    }

    companion object {
        /** Started video players (on screen or in picture-in-picture): they keep the sign-in while locked. */
        @Volatile var visible = 0
            private set

        private const val ACTION_PIP_TOGGLE = "com.vknxworks.jellyctrl.PIP_TOGGLE"
        private const val EXTRA_ITEM = "item"
        private const val EXTRA_START = "start"
        private const val EXTRA_SOURCE = "source"
        private const val EXTRA_AUDIO = "audio"
        private const val EXTRA_SUB = "sub"
        private const val EXTRA_CONTAINER = "container"
        private const val EXTRA_SHUFFLE = "shuffle"

        /** [startMs] null resumes from the saved position; [subtitleIndex] -1 means off, null the server default. */
        fun start(
            context: Context,
            itemId: String,
            startMs: Long? = null,
            mediaSourceId: String? = null,
            audioIndex: Int? = null,
            subtitleIndex: Int? = null,
        ) {
            val i = Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_ITEM, itemId)
                startMs?.let { putExtra(EXTRA_START, it) }
                mediaSourceId?.let { putExtra(EXTRA_SOURCE, it) }
                audioIndex?.let { putExtra(EXTRA_AUDIO, it) }
                subtitleIndex?.let { putExtra(EXTRA_SUB, it) }
                if (context !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(i)
        }

        fun startContainer(context: Context, containerId: String, shuffle: Boolean = false) {
            val i = Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_CONTAINER, containerId)
                putExtra(EXTRA_SHUFFLE, shuffle)
                if (context !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(i)
        }

        private fun argsFrom(i: Intent) = PlayerArgs(
            itemId = i.getStringExtra(EXTRA_ITEM),
            startMs = if (i.hasExtra(EXTRA_START)) i.getLongExtra(EXTRA_START, 0) else null,
            mediaSourceId = i.getStringExtra(EXTRA_SOURCE),
            audioIndex = if (i.hasExtra(EXTRA_AUDIO)) i.getIntExtra(EXTRA_AUDIO, 0) else null,
            subtitleIndex = if (i.hasExtra(EXTRA_SUB)) i.getIntExtra(EXTRA_SUB, -1) else null,
            containerId = i.getStringExtra(EXTRA_CONTAINER),
            shuffle = i.getBooleanExtra(EXTRA_SHUFFLE, false),
        )
    }
}

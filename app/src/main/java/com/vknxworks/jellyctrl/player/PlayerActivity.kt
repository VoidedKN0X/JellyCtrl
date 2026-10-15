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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vknxworks.jellyctrl.container
import com.vknxworks.jellyctrl.ui.theme.JellyCtrlTheme
import com.vknxworks.jellyctrl.data.settings.ThemeChoice

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

        setContent {
            JellyCtrlTheme(ThemeChoice.DARK) {
                val engine by vm.engine.collectAsState()
                val st = engine?.state?.collectAsState()?.value ?: EngineState()
                LaunchedEffect(st.playWhenReady, st.videoWidth, st.videoHeight, vm.loading) {
                    playingForPip = st.playWhenReady && !vm.loading
                    updatePipParams(st)
                }
                LaunchedEffect(vm.finished) { if (vm.finished) finish() }
                PlayerScreen(
                    vm = vm,
                    inPip = inPip,
                    onBack = { finish() },
                    onEnterPip = { enterPip() },
                    rotationLocked = requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
                    onToggleRotationLock = { toggleRotationLock() },
                )
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
            if (playing) "Pause" else "Play",
            if (playing) "Pause" else "Play",
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
    }

    override fun onStop() {
        super.onStop()
        // Leaving the app without PiP pauses playback (the progress gets reported on pause).
        if (!isInPictureInPictureMode) vm.engine.value?.pause()
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(pipReceiver) }
        super.onDestroy()
    }

    companion object {
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

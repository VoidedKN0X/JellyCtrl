package com.vknxworks.jellyctrl

import android.Manifest
import android.content.Intent
import android.os.Bundle
import com.vknxworks.jellyctrl.ui.lock.WindowPrivacy
import android.graphics.Color
import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.vknxworks.jellyctrl.ui.components.NotificationPermission
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.jellyctrl.ui.nav.AppRoot
import com.vknxworks.jellyctrl.ui.nav.ExternalLink
import com.vknxworks.jellyctrl.ui.lock.AppLock
import com.vknxworks.jellyctrl.ui.lock.AppLockOverlay
import com.vknxworks.jellyctrl.ui.lock.ScreenPrivacy
import com.vknxworks.jellyctrl.ui.lock.lockCovering
import com.vknxworks.jellyctrl.ui.lock.lockLocked
import com.vknxworks.jellyctrl.ui.theme.JellyCtrlTheme

/** The Ctrl / Signal backgrounds (dark #08080A, light #F5F5F1), used for the status and navigation bars. */
private val SIGNAL_BACKGROUND_DARK = Color.rgb(0x08, 0x08, 0x0A)
private val SIGNAL_BACKGROUND_LIGHT = Color.rgb(0xF5, 0xF5, 0xF1)

/** Bar style for the theme: light icons over the dark background, dark icons over the light one. */
private fun signalBars(dark: Boolean): SystemBarStyle =
    if (dark) SystemBarStyle.dark(SIGNAL_BACKGROUND_DARK)
    else SystemBarStyle.light(SIGNAL_BACKGROUND_LIGHT, SIGNAL_BACKGROUND_DARK)

class MainActivity : ComponentActivity() {

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val startDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        enableEdgeToEdge(statusBarStyle = signalBars(startDark), navigationBarStyle = signalBars(startDark))
        // Recents shows a blank card until the settings are read (then "Hide in recent apps" decides).
        setRecentsScreenshotEnabled(false)
        val c = container
        if (savedInstanceState == null) {
            ExternalLink.from(intent)?.let { c.pendingLink.value = it }
        }
        setContent {
            val settings by c.settings.settings.collectAsStateWithLifecycle()
            // Follows the phone's dark theme setting; the status and navigation bar icons switch with it.
            val dark = isSystemInDarkTheme()
            DisposableEffect(dark) {
                enableEdgeToEdge(statusBarStyle = signalBars(dark), navigationBarStyle = signalBars(dark))
                onDispose { }
            }
            val stored by c.settings.stored.collectAsState(initial = null)
            // Until the settings are read, trust the synchronous read made at start-up.
            val lockSetting = stored?.appLock ?: c.lockAtStart
            val covered = lockCovering(lockSetting, c.vault)
            // "Hide in screenshots", screens where secrets are typed or shown and the lock and PIN screens keep the
            // window out of screenshots and recordings; "Hide in recent apps" blanks the recent-apps card.
            val secure = settings.hideScreenshots || covered || ScreenPrivacy.forced > 0
            // "Hide in recent apps" (off by default). With the app lock on, Recents always shows a blank
            // card: a preview there would show what's behind the PIN.
            WindowPrivacy(this, secure, hideRecents = settings.hideRecents || lockSetting || c.vault.enabled)
            // Locked (not just asked for the PIN again): the app's screens aren't composed at all. Dialogs, menus
            // and sheets are windows of their own that would stay above (and usable over) the lock screen, and a
            // hardware keyboard could still reach covered controls. Their state, including where you were, is
            // kept here and comes back on unlock. Music and downloads carry on: they don't depend on these screens.
            val locked = lockLocked(lockSetting, c.vault)
            val savedRoot = rememberSaveableStateHolder()
            JellyCtrlTheme(dark = dark) {
                Box(Modifier.fillMaxSize()) {
                    // While the PIN is asked again, the app stays underneath (so nothing you typed is lost), hidden
                    // from TalkBack too.
                    if (!locked) {
                        Box(if (covered) Modifier.fillMaxSize().clearAndSetSemantics { } else Modifier.fillMaxSize()) {
                            savedRoot.SaveableStateProvider("root") { AppRoot() }
                        }
                    }
                    // Leaving the app (Back, swiped away) can lock it while this activity is already finishing:
                    // no lock screen, dialog or fingerprint prompt then. It opens locked next time.
                    if (!isFinishing && !isDestroyed) AppLockOverlay(lockSetting)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        ExternalLink.from(intent)?.let { container.pendingLink.value = it }
    }

    override fun onStart() {
        super.onStart()
        // Coming back while a video plays in picture-in-picture: the app never left the screen as a whole, so
        // check the time away here (it counts from when the video went to picture-in-picture).
        AppLock.onForeground(container.lockEnabled, container.settings.settings.value.lockTimeout.millis)
    }

    /** Asked the first time something wants to show a notification (downloads, music). */
    fun ensureNotificationPermission() {
        // Only if Android's question was never shown: asked automatically at most once, so it never nags.
        if (!NotificationPermission.hasPermission(this) && !NotificationPermission.asked(this)) {
            NotificationPermission.markAsked(this)
            AppLock.expectReturn()
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

package com.vknxworks.jellyctrl

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.vknxworks.jellyctrl.ui.lock.AppLock
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory

class JellyCtrlApp : Application(), SingletonImageLoader.Factory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        AppStrings.context = this
        container = AppContainer(this)
        // The app lock: lock when the app leaves the screen, or after the chosen time away.
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                AppLock.onForeground(container.lockEnabled, container.settings.settings.value.lockTimeout.millis)
            }

            override fun onStop(owner: LifecycleOwner) {
                AppLock.onBackground(container.lockEnabled, container.settings.settings.value.lockTimeout.millis)
            }
        })
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_DOWNLOADS, getString(R.string.channel_downloads), NotificationManager.IMPORTANCE_LOW)
        )
    }

    /** Coil shares the app's OkHttp client, so artwork and trickplay requests are authenticated by header. */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { container.http })) }
            .build()

    companion object {
        const val CHANNEL_DOWNLOADS = "downloads"
    }
}

package com.vknxworks.jellyctrl.player

import android.content.Context
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import dev.jdtech.mpv.MPVLib
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.vknxworks.jellyctrl.data.api.JellyfinClient
import java.io.File

/** libmpv-backed engine. Handles formats Media3 can't (styled ASS subtitles, DTS/TrueHD audio, odd containers) without transcoding. */
class MpvEngine(
    private val context: Context,
    hwDecoding: Boolean,
    /** Returns an HTTP header line ("Name: value") authenticating [url], or null when it isn't our server. */
    private val authHeaderFor: (url: String) -> String? = { null },
) : PlayerEngine {
    override val name = "mpv"

    // mpv does its own TLS. Never run it without certificate verification: no CA bundle, no mpv.
    private val caFile: File = caBundle(context)
        ?: throw IllegalStateException("mpv can't verify secure connections on this device (no CA certificates found)")

    private val _state = MutableStateFlow(EngineState())
    override val state: StateFlow<EngineState> = _state.asStateFlow()

    private val mpv: MPVLib = MPVLib.create(context) ?: throw IllegalStateException("libmpv failed to start")
    private var released = false
    private var pending: LoadRequest? = null
    private var surfaceAttached = false

    private val observer = object : MPVLib.EventObserver {
        override fun eventProperty(property: String) {}

        override fun eventProperty(property: String, value: Long) {
            when (property) {
                "dwidth" -> _state.update { it.copy(videoWidth = value.toInt()) }
                "dheight" -> _state.update { it.copy(videoHeight = value.toInt()) }
            }
        }

        override fun eventProperty(property: String, value: Double) {
            when (property) {
                "time-pos" -> _state.update { it.copy(positionMs = (value * 1000).toLong()) }
                "duration" -> _state.update { it.copy(durationMs = (value * 1000).toLong()) }
                "demuxer-cache-time" -> _state.update { it.copy(bufferedMs = (value * 1000).toLong()) }
                "speed" -> _state.update { it.copy(speed = value.toFloat()) }
            }
        }

        override fun eventProperty(property: String, value: Boolean) {
            when (property) {
                "pause" -> _state.update { it.copy(playWhenReady = !value) }
                "paused-for-cache" -> _state.update { it.copy(buffering = value) }
                "eof-reached" -> _state.update { it.copy(ended = value) }
                "seeking" -> if (value) _state.update { it.copy(buffering = true) }
            }
        }

        override fun eventProperty(property: String, value: String) {}

        override fun event(eventId: Int) {
            when (eventId) {
                MPVLib.MpvEvent.MPV_EVENT_FILE_LOADED -> {
                    _state.update { it.copy(buffering = false, error = null) }
                    pending?.let { onFileLoaded(it) }
                }
                MPVLib.MpvEvent.MPV_EVENT_PLAYBACK_RESTART -> _state.update { it.copy(buffering = false) }
                MPVLib.MpvEvent.MPV_EVENT_END_FILE -> {
                    if (_state.value.durationMs <= 0 && !released) {
                        _state.update { it.copy(error = lastError ?: "mpv couldn't open this file", buffering = false) }
                    }
                }
            }
        }
    }

    @Volatile private var lastError: String? = null
    private val logObserver = object : MPVLib.LogObserver {
        override fun logMessage(prefix: String, level: Int, text: String) {
            if (level <= MPVLib.MpvLogLevel.MPV_LOG_LEVEL_ERROR) lastError = JellyfinClient.redact("$prefix: ${text.trim()}")
        }
    }

    init {
        val configDir = File(context.filesDir, "mpv").apply { mkdirs() }
        with(mpv) {
            setOptionString("config", "no")
            setOptionString("config-dir", configDir.path)
            setOptionString("vo", "gpu")
            setOptionString("gpu-context", "android")
            setOptionString("opengl-es", "yes")
            setOptionString("hwdec", if (hwDecoding) "mediacodec-copy" else "no")
            setOptionString("hwdec-codecs", "h264,hevc,mpeg4,mpeg2video,vp8,vp9,av1")
            setOptionString("ao", "audiotrack,opensles")
            setOptionString("audio-set-media-role", "yes")
            setOptionString("force-window", "no")
            setOptionString("idle", "yes")
            setOptionString("keep-open", "yes")
            setOptionString("save-position-on-quit", "no")
            setOptionString("input-default-bindings", "no")
            setOptionString("demuxer-max-bytes", (128 * 1024 * 1024).toString())
            setOptionString("demuxer-max-back-bytes", (64 * 1024 * 1024).toString())
            setOptionString("cache", "yes")
            setOptionString("sub-fonts-dir", "/system/fonts")
            setOptionString("osd-fonts-dir", "/system/fonts")
            setOptionString("sub-font", "Roboto")
            setOptionString("sub-scale-with-window", "yes")
            setOptionString("sub-use-margins", "no")
            setOptionString("user-agent", "JellyCtrl mpv")
            setOptionString("tls-verify", "yes")
            setOptionString("tls-ca-file", caFile.path)
            init()
            addObserver(observer)
            addLogObserver(logObserver)
            observeProperty("time-pos", MPVLib.MpvFormat.MPV_FORMAT_DOUBLE)
            observeProperty("duration", MPVLib.MpvFormat.MPV_FORMAT_DOUBLE)
            observeProperty("demuxer-cache-time", MPVLib.MpvFormat.MPV_FORMAT_DOUBLE)
            observeProperty("speed", MPVLib.MpvFormat.MPV_FORMAT_DOUBLE)
            observeProperty("pause", MPVLib.MpvFormat.MPV_FORMAT_FLAG)
            observeProperty("paused-for-cache", MPVLib.MpvFormat.MPV_FORMAT_FLAG)
            observeProperty("eof-reached", MPVLib.MpvFormat.MPV_FORMAT_FLAG)
            observeProperty("seeking", MPVLib.MpvFormat.MPV_FORMAT_FLAG)
            observeProperty("dwidth", MPVLib.MpvFormat.MPV_FORMAT_INT64)
            observeProperty("dheight", MPVLib.MpvFormat.MPV_FORMAT_INT64)
        }
    }

    override fun load(request: LoadRequest) {
        pending = request
        lastError = null
        _state.value = EngineState()
        mpv.setPropertyString("start", if (request.startMs > 0) "%.3f".format(java.util.Locale.US, request.startMs / 1000.0) else "none")
        mpv.setPropertyBoolean("pause", false)
        setAuthFor(request.url)
        mpv.command(arrayOf("loadfile", request.url, "replace"))
    }

    private fun onFileLoaded(request: LoadRequest) {
        mpv.setPropertyString("start", "none")
        request.externalSubs.forEach { s ->
            val title = s.title ?: s.language ?: "External"
            setAuthFor(s.url)
            mpv.command(arrayOf("sub-add", s.url, "auto", title, s.language ?: ""))
        }
        pending?.let { setAuthFor(it.url) }
        request.audioOrdinal?.let { selectAudio(it) }
        selectSubtitle(request.subtitle)
    }

    /** The token goes in a request header, and only to the signed-in server - never into the URL. */
    private fun setAuthFor(url: String) {
        mpv.setPropertyString("http-header-fields", authHeaderFor(url) ?: "")
    }

    private data class Track(val id: Int, val type: String, val external: Boolean, val externalFile: String?)

    private fun tracks(): List<Track> {
        val count = mpv.getPropertyInt("track-list/count") ?: return emptyList()
        return (0 until count).mapNotNull { i ->
            val id = mpv.getPropertyInt("track-list/$i/id") ?: return@mapNotNull null
            val type = mpv.getPropertyString("track-list/$i/type") ?: return@mapNotNull null
            Track(
                id, type,
                mpv.getPropertyBoolean("track-list/$i/external") ?: false,
                mpv.getPropertyString("track-list/$i/external-filename"),
            )
        }
    }

    override fun play() = mpv.setPropertyBoolean("pause", false)
    override fun pause() = mpv.setPropertyBoolean("pause", true)

    override fun seekTo(ms: Long) {
        _state.update { it.copy(positionMs = ms) }
        mpv.command(arrayOf("seek", "%.3f".format(java.util.Locale.US, ms.coerceAtLeast(0) / 1000.0), "absolute"))
    }

    override fun setSpeed(speed: Float) = mpv.setPropertyDouble("speed", speed.toDouble())

    override fun selectAudio(ordinal: Int) {
        val audio = tracks().filter { it.type == "audio" }
        audio.getOrNull(ordinal)?.let { mpv.setPropertyInt("aid", it.id) }
    }

    override fun selectSubtitle(selection: SubSelection) {
        val subs = tracks().filter { it.type == "sub" }
        when (selection) {
            SubSelection.Off -> mpv.setPropertyString("sid", "no")
            is SubSelection.Embedded -> subs.filter { !it.external }.getOrNull(selection.ordinal)
                ?.let { mpv.setPropertyInt("sid", it.id) } ?: mpv.setPropertyString("sid", "no")
            is SubSelection.External -> {
                val url = pending?.externalSubs?.firstOrNull { it.jellyfinIndex == selection.jellyfinIndex }?.url
                subs.firstOrNull { it.external && it.externalFile == url }
                    ?.let { mpv.setPropertyInt("sid", it.id) }
            }
        }
    }

    override fun release() {
        if (released) return
        released = true
        runCatching {
            mpv.removeObserver(observer)
            mpv.removeLogObserver(logObserver)
            mpv.command(arrayOf("stop"))
            mpv.destroy()
        }
    }

    private fun attach(holder: SurfaceHolder) {
        if (released) return
        mpv.attachSurface(holder.surface)
        mpv.setOptionString("force-window", "yes")
        mpv.setPropertyString("vo", "gpu")
        surfaceAttached = true
    }

    private fun detach() {
        if (released || !surfaceAttached) return
        mpv.setPropertyString("vo", "null")
        mpv.setOptionString("force-window", "no")
        mpv.detachSurface()
        surfaceAttached = false
    }

    private fun applyResize(mode: ResizeMode) {
        if (released) return
        when (mode) {
            ResizeMode.FIT -> { mpv.setPropertyString("keepaspect", "yes"); mpv.setPropertyDouble("panscan", 0.0) }
            ResizeMode.ZOOM -> { mpv.setPropertyString("keepaspect", "yes"); mpv.setPropertyDouble("panscan", 1.0) }
            ResizeMode.FILL -> { mpv.setPropertyString("keepaspect", "no"); mpv.setPropertyDouble("panscan", 0.0) }
        }
    }

    @Composable
    override fun VideoSurface(modifier: Modifier, resizeMode: ResizeMode) {
        AndroidView(
            factory = { ctx ->
                SurfaceView(ctx).apply {
                    holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: SurfaceHolder) = attach(holder)
                        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                            if (!released) mpv.setPropertyString("android-surface-size", "${width}x$height")
                        }
                        override fun surfaceDestroyed(holder: SurfaceHolder) = detach()
                    })
                }
            },
            modifier = modifier,
        )
        LaunchedEffect(resizeMode) { applyResize(resizeMode) }
    }

    companion object {
        private val USER_CA_DIR = File("/data/misc/user/0/cacerts-added")
        private const val PEM_BEGIN = "-----BEGIN CERTIFICATE-----"
        private const val PEM_END = "-----END CERTIFICATE-----"

        /** System CA files are PEM (with a text dump in front); user-added ones are stored as raw DER. */
        private fun toPem(bytes: ByteArray): String? {
            val text = String(bytes, Charsets.ISO_8859_1)
            val begin = text.indexOf(PEM_BEGIN)
            val end = text.indexOf(PEM_END)
            if (begin >= 0 && end > begin) return text.substring(begin, end + PEM_END.length)
            val cert = java.security.cert.CertificateFactory.getInstance("X.509")
                .generateCertificate(bytes.inputStream()) as java.security.cert.X509Certificate
            val b64 = android.util.Base64.encodeToString(cert.encoded, android.util.Base64.NO_WRAP).chunked(64).joinToString("\n")
            return "$PEM_BEGIN\n$b64\n$PEM_END"
        }

        /**
         * User-installed CAs, which the app's network security config trusts. Read through AndroidCAStore
         * (entries prefixed "user:"); the raw cacerts-added directory is tried too but is usually not readable.
         * Any failure just means no user CAs, never a weaker check.
         */
        private fun userCaPems(): List<String> {
            val fromStore = runCatching {
                val ks = java.security.KeyStore.getInstance("AndroidCAStore").apply { load(null) }
                ks.aliases().toList().filter { it.startsWith("user:") }.mapNotNull { alias ->
                    (ks.getCertificate(alias) as? java.security.cert.X509Certificate)?.let { runCatching { toPem(it.encoded) }.getOrNull() }
                }
            }.getOrNull().orEmpty()
            val fromDir = runCatching {
                USER_CA_DIR.listFiles()?.mapNotNull { f -> runCatching { toPem(f.readBytes()) }.getOrNull() }
            }.getOrNull().orEmpty()
            return (fromStore + fromDir).distinct()
        }

        /**
         * mpv uses its own TLS stack and can't see Android's trust store, so build a PEM bundle
         * from the system CA directory (plus user-installed CAs when readable, matching the app's
         * network security config) and point mpv at it.
         */
        fun caBundle(context: Context): File? {
            val out = File(context.filesDir, "mpv/cacert.pem")
            val stamp = File(context.filesDir, "mpv/cacert.user")
            val userPems = userCaPems()
            val userHash = userPems.sorted().joinToString("|").hashCode().toString()
            val sameUser = runCatching { stamp.readText() }.getOrNull() == userHash
            if (out.exists() && out.length() > 0 && sameUser && System.currentTimeMillis() - out.lastModified() < 30L * 24 * 3600 * 1000) return out
            val dirs = listOf(File("/apex/com.android.conscrypt/cacerts"), File("/system/etc/security/cacerts"))
            val certs = dirs.firstOrNull { it.isDirectory && (it.listFiles()?.isNotEmpty() == true) }?.listFiles() ?: return null
            return runCatching {
                out.parentFile?.mkdirs()
                val tmp = File(out.parentFile, "cacert.pem.tmp")
                tmp.bufferedWriter().use { w ->
                    certs.forEach { f ->
                        val pem = runCatching { toPem(f.readBytes()) }.getOrNull() ?: return@forEach
                        w.write(pem)
                        w.write("\n")
                    }
                    userPems.forEach { pem ->
                        w.write(pem)
                        w.write("\n")
                    }
                }
                if (!tmp.renameTo(out)) { tmp.copyTo(out, overwrite = true); tmp.delete() }
                runCatching { stamp.writeText(userHash) }
                out
            }.getOrNull()?.takeIf { it.length() > 0 }
        }
    }
}

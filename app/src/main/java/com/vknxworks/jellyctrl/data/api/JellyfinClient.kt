package com.vknxworks.jellyctrl.data.api

import com.vknxworks.jellyctrl.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.net.URLEncoder
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.vknxworks.jellyctrl.R
import com.vknxworks.jellyctrl.str

/** Json used for the Jellyfin REST API (PascalCase on the wire). */
val JellyfinJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
    encodeDefaults = true
    isLenient = true
    namingStrategy = JsonNamingStrategy { _, _, serialName ->
        serialName.replaceFirstChar { it.uppercaseChar() }
    }
}

/** Plain camelCase Json, used for app storage and the few camelCase Jellyfin endpoints (/Packages). */
val PlainJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
    encodeDefaults = true
    isLenient = true
}

class ApiException(val code: Int, message: String) : IOException(message)

/**
 * Thin, typed wrapper over the Jellyfin HTTP API for one server + (optionally) one signed-in user.
 * Endpoint methods live in the extension files next to this one.
 */
class JellyfinClient(
    baseUrl: String,
    val accessToken: String?,
    val userId: String?,
    val deviceId: String,
    val deviceName: String,
    val http: OkHttpClient,
) {
    val baseUrl: String = normalizeBaseUrl(baseUrl)
    private val base: HttpUrl = this.baseUrl.toHttpUrl()

    val authHeader: String
        get() = buildString {
            append("MediaBrowser Client=\"").append(CLIENT_NAME)
            append("\", Device=\"").append(deviceName.replace("\"", ""))
            append("\", DeviceId=\"").append(deviceId)
            append("\", Version=\"").append(BuildConfig.VERSION_NAME).append('"')
            if (!accessToken.isNullOrBlank()) append(", Token=\"").append(accessToken).append('"')
        }

    fun requireUserId(): String = userId ?: error(str(R.string.not_signed_in))

    fun url(path: String, params: Map<String, Any?> = emptyMap()): HttpUrl {
        val b = base.newBuilder()
        path.trim('/').split('/').filter { it.isNotEmpty() }.forEach { b.addPathSegment(it) }
        params.forEach { (k, v) ->
            when (v) {
                null -> Unit
                is Collection<*> -> if (v.isNotEmpty()) b.addQueryParameter(k, v.joinToString(","))
                else -> b.addQueryParameter(k, v.toString())
            }
        }
        return b.build()
    }

    /** True when [u] points at this server: exact scheme, host and port, under the server's base path. */
    fun ownsUrl(u: HttpUrl): Boolean {
        if (!u.scheme.equals(base.scheme, true) || !u.host.equals(base.host, true) || u.port != base.port) return false
        val basePath = base.encodedPath.trimEnd('/')
        return basePath.isEmpty() || u.encodedPath == basePath || u.encodedPath.startsWith("$basePath/")
    }

    fun ownsUrl(u: String): Boolean = u.toHttpUrlOrNull()?.let { ownsUrl(it) } == true

    /**
     * A server-provided relative URL (e.g. TranscodingUrl, DeliveryUrl) made absolute. Any token the server
     * embedded in the query is removed: requests to this server are authenticated with a header instead.
     */
    fun absolute(relative: String): String {
        val full = if (relative.startsWith("http://", true) || relative.startsWith("https://", true)) relative
        else baseUrl + (if (relative.startsWith("/")) "" else "/") + relative
        val parsed = full.toHttpUrlOrNull() ?: return full
        if (!ownsUrl(parsed)) return full
        return stripTokenParams(parsed).toString()
    }

    fun imageUrl(
        itemId: String,
        type: String = "Primary",
        tag: String? = null,
        maxWidth: Int? = null,
        index: Int? = null,
        quality: Int = 90,
    ): String {
        val path = if (index != null) "Items/$itemId/Images/$type/$index" else "Items/$itemId/Images/$type"
        return url(path, mapOf("tag" to tag, "fillWidth" to maxWidth, "quality" to quality)).toString()
    }

    fun userImageUrl(userId: String, tag: String?): String =
        url("Users/$userId/Images/Primary", mapOf("tag" to tag, "fillWidth" to 200)).toString()

    // ---------- raw requests ----------

    @PublishedApi
    internal suspend fun execute(
        method: String,
        path: String,
        params: Map<String, Any?> = emptyMap(),
        body: RequestBody? = null,
    ): String = withContext(Dispatchers.IO) {
        val reqBody = body ?: if (method == "POST" || method == "PUT") EMPTY_BODY else null
        val request = Request.Builder()
            .url(url(path, params))
            .header("Authorization", authHeader)
            .header("Accept", "application/json")
            .method(method, reqBody)
            .build()
        http.newCall(request).await().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                val msg = when (resp.code) {
                    401 -> str(R.string.unauthorized_sign_in_again)
                    403 -> str(R.string.not_allowed_for_this_user)
                    404 -> str(R.string.not_found)
                    else -> text.take(300).ifBlank { "HTTP ${resp.code}" }
                }
                throw ApiException(resp.code, msg)
            }
            text
        }
    }

    suspend inline fun <reified T> get(path: String, params: Map<String, Any?> = emptyMap()): T =
        decode(execute("GET", path, params))

    suspend fun getText(path: String, params: Map<String, Any?> = emptyMap()): String = execute("GET", path, params)

    suspend inline fun <reified T> post(
        path: String,
        params: Map<String, Any?> = emptyMap(),
        body: JsonElement? = null,
    ): T = decode(execute("POST", path, params, body?.toRequestBodyJson()))

    suspend fun postUnit(path: String, params: Map<String, Any?> = emptyMap(), body: JsonElement? = null) {
        execute("POST", path, params, body?.toRequestBodyJson())
    }

    suspend fun deleteUnit(path: String, params: Map<String, Any?> = emptyMap()) {
        execute("DELETE", path, params)
    }

    @PublishedApi
    internal inline fun <reified T> decode(text: String): T {
        if (T::class == Unit::class) return Unit as T
        val element = JellyfinJson.parseToJsonElement(text.ifBlank { "null" })
        return JellyfinJson.decodeFromJsonElement(element)
    }

    inline fun <reified T> encode(value: T): JsonElement = JellyfinJson.encodeToJsonElement(
        kotlinx.serialization.serializer<T>(), value
    )

    companion object {
        const val CLIENT_NAME = "JellyCtrl"
        private val JSON_MEDIA = "application/json".toMediaType()
        private val EMPTY_BODY = ByteArray(0).toRequestBody(null)

        fun JsonElement.toRequestBodyJson(): RequestBody = toString().toRequestBody(JSON_MEDIA)

        fun normalizeBaseUrl(input: String): String {
            var s = input.trim().trimEnd('/')
            if (!s.startsWith("http://", true) && !s.startsWith("https://", true)) s = "https://$s"
            // Strip a pasted web UI path like /web/index.html#!/home
            val webIdx = s.indexOf("/web", ignoreCase = true)
            if (webIdx > 8) s = s.substring(0, webIdx)
            return s.trimEnd('/')
        }

        fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

        private val TOKEN_PARAMS = setOf("api_key", "apikey")

        fun stripTokenParams(u: HttpUrl): HttpUrl {
            val names = u.queryParameterNames.filter { it.lowercase() in TOKEN_PARAMS }
            if (names.isEmpty()) return u
            val b = u.newBuilder()
            names.forEach { b.removeAllQueryParameters(it) }
            return b.build()
        }

        // "CurrentPw"/"NewPw" are the password-change fields; a quoted value is masked up to its closing quote
        // (passwords can contain spaces and commas).
        private val SECRET_RE = Regex("(?i)(api_?key|token|password|passwd|(?:\\b|current|new|old)pw|secret)(\"?\\s*[=:]\\s*)(\"(?:[^\"\\\\]|\\\\.)*\"|[^&\\s\",]+)")
        private val BEARER_RE = Regex("(?i)(bearer\\s+)[A-Za-z0-9._~+/=-]+")

        /** Removes access tokens and passwords from text that may be shown to the user (player errors, exception messages, server logs). */
        fun redact(text: String): String {
            val masked = SECRET_RE.replace(text) { m ->
                m.groupValues[1] + m.groupValues[2] + if (m.groupValues[3].startsWith("\"")) "\"***\"" else "***"
            }
            return BEARER_RE.replace(masked) { m -> m.groupValues[1] + "***" }
        }
    }
}

/** Suspend until the OkHttp call completes; cancels the call if the coroutine is cancelled. */
suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) = cont.resume(response)
        override fun onFailure(call: Call, e: IOException) {
            if (!cont.isCancelled) cont.resumeWithException(e)
        }
    })
    cont.invokeOnCancellation { runCatching { cancel() } }
}

fun JsonObject.str(key: String): String? = (this[key] as? kotlinx.serialization.json.JsonPrimitive)?.let {
    if (it.isString) it.content else it.content.takeIf { c -> c != "null" }
}

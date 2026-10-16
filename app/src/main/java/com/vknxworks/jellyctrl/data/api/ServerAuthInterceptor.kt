package com.vknxworks.jellyctrl.data.api

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Authenticates media, image and trickplay requests with a header instead of an `api_key` query parameter.
 *
 * The header is only added when the request goes to the signed-in server (exact scheme, host and port), so the
 * token is never sent to third-party hosts. Requests that carry credentials over plain HTTP are refused unless
 * the user has accepted an unencrypted connection to that server.
 */
class ServerAuthInterceptor(
    private val activeClient: () -> JellyfinClient?,
    private val cleartextAllowed: (HttpUrl) -> Boolean,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val client = activeClient()
        val hasAuth = request.header("Authorization") != null || request.header("X-Emby-Token") != null
        if (!hasAuth && client != null && !client.accessToken.isNullOrBlank() && client.ownsUrl(request.url)) {
            request = request.newBuilder().header("Authorization", client.authHeader).build()
        }
        val carriesCredentials = request.header("Authorization")?.contains("Token=") == true || request.header("X-Emby-Token") != null
        if (carriesCredentials && !request.url.isHttps && !cleartextAllowed(request.url)) {
            throw IOException("Blocked an unencrypted connection to ${request.url.host}. Add the server again and accept the HTTP warning to use it.")
        }
        return chain.proceed(request)
    }
}

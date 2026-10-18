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
        val carriesCredentials = request.header("Authorization")?.contains("Token=") == true || request.header("X-Emby-Token") != null ||
            isPasswordPath(request.url)
        if (carriesCredentials && !request.url.isHttps && !cleartextAllowed(request.url)) {
            throw IOException("Blocked an unencrypted connection to ${request.url.host}. Add the server again and accept the HTTP warning to use it.")
        }
        return chain.proceed(request)
    }

    internal companion object {
        /** Requests whose body holds a password or Quick Connect secret, even before there's a token. */
        val PASSWORD_PATHS = listOf(
            "/users/authenticatebyname", "/users/authenticatewithquickconnect", "/users/password", "/users/new",
        )

        fun isPasswordPath(url: HttpUrl): Boolean {
            val path = url.encodedPath.lowercase()
            return PASSWORD_PATHS.any { path.endsWith(it) }
        }
    }
}

/**
 * Network interceptor: the checks above again, on every redirect hop. OkHttp follows redirects after the
 * application interceptors have run, and a 307/308 redirect re-sends the request body, so without this a
 * sign-in or password change could be redirected to plain HTTP, or to another site, with the password in it.
 * - a password body is only ever sent to the address the call was made to;
 * - the token never goes to another address (OkHttp only drops "Authorization" there);
 * - nothing with a token or a password goes over plain HTTP unless the user accepted that for the server.
 */
class CredentialRedirectGuard(private val cleartextAllowed: (HttpUrl) -> Boolean) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val origin = chain.call().request().url
        val sameOrigin = request.url.scheme == origin.scheme && request.url.host.equals(origin.host, true) && request.url.port == origin.port
        val passwordBody = request.body != null && ServerAuthInterceptor.isPasswordPath(origin)
        if (!sameOrigin) {
            if (passwordBody) throw IOException("Blocked a redirect that would have sent your password to ${request.url.host}")
            if (request.header("Authorization") != null || request.header("X-Emby-Token") != null) {
                request = request.newBuilder().removeHeader("Authorization").removeHeader("X-Emby-Token").build()
            }
        }
        val carriesCredentials = passwordBody || request.header("Authorization")?.contains("Token=") == true ||
            request.header("X-Emby-Token") != null
        if (carriesCredentials && !request.url.isHttps && !cleartextAllowed(request.url)) {
            throw IOException("Blocked an unencrypted connection to ${request.url.host}. Add the server again and accept the HTTP warning to use it.")
        }
        return chain.proceed(request)
    }
}

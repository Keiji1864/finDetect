package com.ghostteam.findetect.core.network

import com.ghostteam.findetect.BuildConfig
import com.ghostteam.findetect.data.auth.SessionManager
import com.ghostteam.findetect.domain.model.SessionIdentity
import dagger.Lazy
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.util.AttributeKey
import kotlinx.coroutines.CancellationException

/** Response bodies and tokens are deliberately excluded from exception messages. */
class ApiStatusException(val status: Int) : Exception("API request failed: $status")

val SessionOwner = AttributeKey<SessionIdentity>("findetect.session.owner")

internal fun HttpClient.attachSession(session: Lazy<SessionManager>): HttpClient = apply {
    val api = Url(BuildConfig.BASE_URL.trimEnd('/') + "/")
    plugin(HttpSend).intercept { request ->
        val url = request.url.build()
        val isApi = url.protocol == api.protocol && url.host == api.host && url.port == api.port &&
            url.encodedPath.startsWith(api.encodedPath)
        check(isApi) { "Protected client only accepts the configured API origin" }
        val manager = session.get()
        val used = manager.credentials() ?: throw CancellationException("No active session")
        val owner = request.attributes.getOrNull(SessionOwner)
        checkNotNull(owner) { "Protected requests require a captured session owner" }
        if (owner != used.identity) throw CancellationException("Request belongs to another session")
        request.headers.remove(HttpHeaders.Authorization)
        request.header(HttpHeaders.Authorization, "Bearer ${used.token}")
        var response = execute(request)
        if (response.response.status.value == 401) {
            response.response.bodyAsText() // Release the first connection before retrying once.
            val refreshed = manager.refreshAfterUnauthorized(used)
                ?: throw CancellationException("Reauthentication required")
            if (!manager.isCurrent(used.identity)) throw CancellationException("Session changed")
            request.headers.remove(HttpHeaders.Authorization)
            request.header(HttpHeaders.Authorization, "Bearer ${refreshed.token}")
            response = execute(request)
        }
        if (!manager.isCurrent(used.identity)) throw CancellationException("Session changed")
        if (response.response.status.value == 401) {
            response.response.bodyAsText()
            manager.invalidate(used.identity)
            throw CancellationException("Reauthentication required")
        }
        if (response.response.status.value !in 200..299) {
            val status = response.response.status.value
            response.response.bodyAsText()
            throw ApiStatusException(status)
        }
        response
    }
}

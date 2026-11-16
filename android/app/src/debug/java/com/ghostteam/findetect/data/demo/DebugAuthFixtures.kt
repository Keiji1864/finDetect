package com.ghostteam.findetect.data.demo

import android.util.Base64
import com.ghostteam.findetect.data.remote.dto.*
import com.ghostteam.findetect.domain.model.SessionUser
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.content.OutgoingContent
import java.io.IOException
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.delay

/** Stateless debug examples, not an authentication server or a credential database. */
object DebugAuthFixtures {
    val demoUserIds = setOf(id("demo-a@example.com"), id("demo-b@example.com"))
    data class Response(val status: Int, val body: String, val retryAfterSeconds: Int? = null, val contentType: String = "application/json")

    @Serializable
    private data class DebugToken(val user: SessionUser, val scenario: String, val rotated: Boolean = false)

    suspend fun response(path: String, request: HttpRequestData, json: Json): Response? {
        fun error(status: Int, code: String, details: List<ApiFieldErrorDto> = emptyList()) =
            Response(status, json.encodeToString(ApiErrorDto(code, "Debug response", details)))
        fun body(): String = (request.body as? OutgoingContent.ByteArrayContent)?.bytes()?.toString(Charsets.UTF_8).orEmpty()
        fun decode(token: String): DebugToken? = runCatching {
            val encoded = token.substringAfter('.', "")
            json.decodeFromString<DebugToken>(String(Base64.decode(encoded, Base64.URL_SAFE or Base64.NO_WRAP), Charsets.UTF_8))
        }.getOrNull()
        fun tokens(payload: DebugToken): TokenPairDto {
            val encoded = Base64.encodeToString(json.encodeToString(payload).toByteArray(Charsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            return TokenPairDto("debug-access.$encoded", "debug-refresh.$encoded", 900)
        }
        if (request.method == HttpMethod.Post) when (path) {
            "/auth/login", "/auth/register" -> {
                val registration = path.endsWith("register")
                val credentials = if (registration) {
                    val value = json.decodeFromString<RegisterRequestDto>(body())
                    Triple(value.email, value.password, value.name)
                } else {
                    val value = json.decodeFromString<LoginRequestDto>(body())
                    Triple(value.email, value.password, if (value.email.startsWith("demo-b")) "Ирина" else "Алексей")
                }
                val email = credentials.first.trim().lowercase()
                // Fixed scenarios deliberately do not remember accounts or retry counts.
                when (email) {
                    "auth-validation@example.com" -> return error(400, "VALIDATION_ERROR", listOf(
                        ApiFieldErrorDto("email", "Debug validation message"), ApiFieldErrorDto("password", "Debug validation message")))
                    "auth-validation-unknown@example.com" -> return error(400, "VALIDATION_ERROR", listOf(ApiFieldErrorDto("unknown", "Debug message")))
                    "auth-rate-limit@example.com" -> return error(429, "RATE_LIMITED").copy(retryAfterSeconds = 5)
                    "auth-rate-limit-no-header@example.com" -> return error(429, "RATE_LIMITED")
                    "auth-server@example.com" -> return error(503, "SERVICE_UNAVAILABLE")
                    "auth-non-json@example.com" -> return Response(500, "<html>Debug server error</html>", contentType = "text/html")
                    "auth-malformed@example.com" -> return Response(if (registration) 201 else 200, "{invalid-json")
                    "auth-timeout@example.com" -> delay(16_000) // The common client timeout is 15 seconds.
                    "auth-slow@example.com" -> delay(2_000)
                }
                if (registration && email == "register-created@example.com") return Response(201, "{invalid-json")
                if (registration && email == "register-uncertain@example.com") throw IOException("Debug response lost")
                if (credentials.second == "wrong-password") return error(401, "INVALID_CREDENTIALS")
                if (registration && email == "taken@example.com") return error(409, "EMAIL_TAKEN")
                if (email == "login-offline@example.com") throw IOException("Debug offline")
                val user = SessionUser(id(email), email, credentials.third)
                val scenario = when (email) {
                    "refresh@example.com" -> "rotate"
                    "refresh-expired@example.com" -> "denied"
                    "refresh-offline@example.com" -> "offline"
                    "refresh-server@example.com" -> "server"
                    "refresh-rate-limit@example.com" -> "rate_limit"
                    else -> "normal"
                }
                return Response(if (registration) 201 else 200, json.encodeToString(AuthResponseDto(user, tokens(DebugToken(user, scenario)))))
            }
            "/auth/refresh" -> {
                val payload = decode(json.decodeFromString<RefreshRequestDto>(body()).refreshToken)
                    ?: return error(401, "REFRESH_TOKEN_INVALID")
                when (payload.scenario) {
                    "denied" -> return error(401, "REFRESH_TOKEN_EXPIRED")
                    "offline" -> throw IOException("Debug offline")
                    "server" -> return error(503, "SERVICE_UNAVAILABLE")
                    "rate_limit" -> return error(429, "RATE_LIMITED")
                }
                return Response(200, json.encodeToString(tokens(payload.copy(rotated = true))))
            }
            "/auth/logout" -> return Response(204, "")
        }
        if (request.method == HttpMethod.Get && path == "/me") {
            val token = request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ").orEmpty()
            val payload = decode(token) ?: return error(401, "TOKEN_INVALID")
            if (payload.scenario != "normal" && !payload.rotated) return error(401, "TOKEN_EXPIRED")
            return Response(200, json.encodeToString(payload.user))
        }
        return null
    }

    private fun id(email: String) = UUID.nameUUIDFromBytes(email.toByteArray(Charsets.UTF_8)).toString()
}

package com.ghostteam.findetect.data.demo

import android.util.Base64
import com.ghostteam.findetect.data.remote.dto.*
import com.ghostteam.findetect.domain.model.SessionUser
import com.ghostteam.findetect.domain.model.UserNameValidation
import com.ghostteam.findetect.domain.model.PasswordValidation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.content.OutgoingContent
import java.io.IOException
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.coroutines.delay

/** Fixed failure scenarios plus persistent debug password verification and account-wide token revocation. */
object DebugAuthFixtures {
    val demoUserIds = setOf(DebugProfileStore.initialId("demo-a@example.com"), DebugProfileStore.initialId("demo-b@example.com"))
    data class Response(val status: Int, val body: String, val retryAfterSeconds: Int? = null, val contentType: String = "application/json")

    @Serializable
    private data class DebugToken(val user: SessionUser, val scenario: String, val rotated: Boolean = false,
        val authVersion: Long = 0, val sessionId: String = "")

    private fun decodeToken(token: String, prefix: String, json: Json): DebugToken? = runCatching {
        require(token.startsWith(prefix))
        json.decodeFromString<DebugToken>(String(Base64.decode(token.removePrefix(prefix),
            Base64.URL_SAFE or Base64.NO_WRAP), Charsets.UTF_8))
    }.getOrNull()

    private suspend fun authorizeAccess(request: HttpRequestData, json: Json, profiles: DebugProfileStore): DebugToken? {
        val header = request.headers[HttpHeaders.Authorization].orEmpty()
        if (!header.startsWith("Bearer ")) return null
        val payload = decodeToken(header.removePrefix("Bearer "), "debug-access.", json) ?: return null
        if (payload.scenario in setOf("rotate", "denied", "offline", "server", "rate_limit") && !payload.rotated) return null
        val user = profiles.authorize(payload.user, payload.authVersion) ?: return null
        return payload.copy(user = user)
    }

    /** Applies the same epoch check to financial fixtures, not only /me and password requests. */
    suspend fun authorizationError(request: HttpRequestData, json: Json, profiles: DebugProfileStore): Response? =
        if (authorizeAccess(request, json, profiles) != null) null
        else Response(401, json.encodeToString(ApiErrorDto("UNAUTHORIZED", "Debug authorization required")))

    suspend fun response(path: String, request: HttpRequestData, json: Json, profiles: DebugProfileStore): Response? {
        fun error(status: Int, code: String, details: List<ApiFieldErrorDto> = emptyList()) =
            Response(status, json.encodeToString(ApiErrorDto(code, "Debug response", details)))
        fun body(): String = (request.body as? OutgoingContent.ByteArrayContent)?.bytes()?.toString(Charsets.UTF_8).orEmpty()
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
                // Failures remain fixed; successful profiles are shared by auth and /me.
                when (email) {
                    "auth-validation@example.com" -> return error(400, "VALIDATION_ERROR", listOf(
                        ApiFieldErrorDto("email", "Debug validation message"), ApiFieldErrorDto("password", "Debug validation message")))
                    "auth-validation-unknown@example.com" -> return error(400, "VALIDATION_ERROR", listOf(ApiFieldErrorDto("unknown", "Debug message")))
                    "auth-rate-limit@example.com" -> return error(429, "RATE_LIMITED").copy(retryAfterSeconds = 5)
                    "auth-rate-limit-no-header@example.com" -> return error(429, "RATE_LIMITED")
                    "auth-server@example.com" -> return error(503, "SERVICE_UNAVAILABLE")
                    "auth-non-json@example.com" -> return Response(500, "<html>Debug server error</html>", contentType = "text/html")
                    "auth-timeout@example.com" -> delay(16_000) // The common client timeout is 15 seconds.
                    "auth-slow@example.com" -> delay(2_000)
                }
                if (registration && email == "taken@example.com") return error(409, "EMAIL_TAKEN")
                if (email == "login-offline@example.com") throw IOException("Debug offline")
                if (registration) {
                    val invalid = buildList {
                        if (credentials.second.length !in PasswordValidation.MIN_LENGTH..PasswordValidation.MAX_LENGTH)
                            add(ApiFieldErrorDto("password", "Debug invalid password length"))
                        if (UserNameValidation.error(credentials.third) != null) add(ApiFieldErrorDto("name", "Debug invalid name"))
                    }
                    if (invalid.isNotEmpty()) return error(400, "VALIDATION_ERROR", invalid)
                }
                val account = if (registration) profiles.register(email, credentials.third, credentials.second)
                    ?: return error(409, "EMAIL_TAKEN")
                else profiles.login(email, credentials.second, credentials.third) ?: return error(401, "INVALID_CREDENTIALS")
                val user = account.user
                if (email == "auth-malformed@example.com" || (registration && email == "register-created@example.com")) {
                    return Response(if (registration) 201 else 200, "{invalid-json")
                }
                if (registration && email == "register-uncertain@example.com") throw IOException("Debug response lost")
                val scenario = when (email) {
                    "refresh@example.com" -> "rotate"
                    "refresh-expired@example.com" -> "denied"
                    "refresh-offline@example.com" -> "offline"
                    "refresh-server@example.com" -> "server"
                    "refresh-rate-limit@example.com" -> "rate_limit"
                    "password-refresh@example.com" -> "password_rotate"
                    "password-refresh-expired@example.com" -> "password_denied"
                    "password-refresh-offline@example.com" -> "password_offline"
                    "password-refresh-server@example.com" -> "password_server"
                    "password-refresh-rate-limit@example.com" -> "password_rate_limit"
                    else -> "normal"
                }
                return Response(if (registration) 201 else 200, json.encodeToString(AuthResponseDto(user,
                    tokens(DebugToken(user, scenario, authVersion = account.authVersion, sessionId = UUID.randomUUID().toString())))))
            }
            "/auth/refresh" -> {
                val payload = decodeToken(json.decodeFromString<RefreshRequestDto>(body()).refreshToken, "debug-refresh.", json)
                    ?: return error(401, "REFRESH_TOKEN_INVALID")
                val user = profiles.authorize(payload.user, payload.authVersion) ?: return error(401, "REFRESH_TOKEN_INVALID")
                when (payload.scenario) {
                    "denied", "password_denied" -> return error(401, "REFRESH_TOKEN_EXPIRED")
                    "offline", "password_offline" -> throw IOException("Debug offline")
                    "server", "password_server" -> return error(503, "SERVICE_UNAVAILABLE")
                    "rate_limit" -> return error(429, "RATE_LIMITED")
                    "password_rate_limit" -> return error(429, "RATE_LIMITED").copy(retryAfterSeconds = 5)
                }
                return Response(200, json.encodeToString(tokens(payload.copy(user = user, rotated = true))))
            }
            "/auth/logout" -> return Response(204, "")
            "/auth/password/change" -> {
                val payload = authorizeAccess(request, json, profiles) ?: return error(401, "UNAUTHORIZED")
                if (payload.scenario.startsWith("password_") && !payload.rotated) return error(401, "UNAUTHORIZED")
                val change = runCatching {
                    val value = json.parseToJsonElement(body()) as JsonObject
                    require(value.keys == setOf("currentPassword", "newPassword"))
                    json.decodeFromString<ChangePasswordRequestDto>(body())
                }.getOrNull() ?: return error(400, "VALIDATION_ERROR")
                val invalid = buildList {
                    if (change.currentPassword.isEmpty() || change.currentPassword.length > PasswordValidation.MAX_LENGTH)
                        add(ApiFieldErrorDto("currentPassword", "Debug invalid current password"))
                    if (change.newPassword.length !in PasswordValidation.MIN_LENGTH..PasswordValidation.MAX_LENGTH)
                        add(ApiFieldErrorDto("newPassword", "Debug invalid new password"))
                }
                if (invalid.isNotEmpty()) return error(400, "VALIDATION_ERROR", invalid)
                val email = payload.user.email
                when (email) {
                    "password-validation@example.com" -> return error(400, "VALIDATION_ERROR",
                        listOf(ApiFieldErrorDto("newPassword", "Debug validation message")))
                    "password-validation-unknown@example.com" -> return error(400, "VALIDATION_ERROR",
                        listOf(ApiFieldErrorDto("unknown", "Debug validation message")))
                    "password-same@example.com" -> return error(400, "NEW_PASSWORD_SAME_AS_OLD")
                    "password-rate-limit@example.com" -> return error(429, "RATE_LIMITED").copy(retryAfterSeconds = 5)
                    "password-rate-limit-no-header@example.com" -> return error(429, "RATE_LIMITED")
                    "password-server@example.com" -> return error(503, "SERVICE_UNAVAILABLE")
                    "password-offline@example.com" -> throw IOException("Debug password request offline")
                    "password-slow@example.com" -> delay(2_000)
                }
                when (profiles.changePassword(payload.user.id, payload.authVersion, change.currentPassword, change.newPassword)) {
                    DebugProfileStore.PasswordChange.UNAUTHORIZED -> return error(401, "UNAUTHORIZED")
                    DebugProfileStore.PasswordChange.CURRENT_PASSWORD_INVALID -> return error(403, "CURRENT_PASSWORD_INVALID")
                    DebugProfileStore.PasswordChange.SAME_PASSWORD -> return error(400, "NEW_PASSWORD_SAME_AS_OLD")
                    DebugProfileStore.PasswordChange.INVALID_NEW_PASSWORD -> return error(400, "VALIDATION_ERROR",
                        listOf(ApiFieldErrorDto("newPassword", "Debug invalid new password")))
                    DebugProfileStore.PasswordChange.CHANGED -> Unit
                }
                when (email) {
                    "password-lost@example.com" -> throw IOException("Debug password response lost after change")
                    "password-timeout@example.com" -> delay(16_000) // Change has committed before the client timeout.
                    "password-unexpected-success@example.com" -> return Response(200, "{}")
                }
                return Response(204, "")
            }
        }
        if (path == "/me") {
            val payload = authorizeAccess(request, json, profiles) ?: return error(401, "UNAUTHORIZED")
            val user = payload.user
            when (request.method) {
                HttpMethod.Get -> {
                    if (user.email == "profile-load-error@example.com") return error(503, "SERVICE_UNAVAILABLE")
                    if (user.email == "profile-slow@example.com") delay(3_000) // Snapshot precedes a later PATCH.
                    return Response(200, json.encodeToString(user))
                }
                HttpMethod.Patch -> {
                    val patch = runCatching { json.parseToJsonElement(body()) as? JsonObject }.getOrNull()
                    val name = patch?.get("name") as? JsonPrimitive
                    if (patch?.keys != setOf("name") || name == null || !name.isString || UserNameValidation.error(name.content) != null ||
                        user.email == "profile-save-invalid@example.com") return error(400, "VALIDATION_ERROR", listOf(ApiFieldErrorDto("name", "Debug invalid name")))
                    if (user.email == "profile-save-error@example.com") return error(503, "SERVICE_UNAVAILABLE")
                    if (user.email == "profile-slow@example.com") delay(1_000)
                    val updated = profiles.updateName(user.id, payload.authVersion, name.content.trim()) ?: return error(401, "TOKEN_INVALID")
                    if (user.email == "profile-save-lost@example.com") throw IOException("Debug PATCH response lost")
                    return Response(200, json.encodeToString(updated))
                }
                HttpMethod.Delete -> {
                    if (user.email == "profile-delete-error@example.com") return error(503, "SERVICE_UNAVAILABLE")
                    if (user.email == "profile-slow@example.com") delay(1_000)
                    if (!profiles.delete(user.id, payload.authVersion)) return error(401, "TOKEN_INVALID")
                    if (user.email == "profile-delete-lost@example.com") throw IOException("Debug DELETE response lost")
                    return Response(204, "")
                }
            }
        }
        return null
    }

}

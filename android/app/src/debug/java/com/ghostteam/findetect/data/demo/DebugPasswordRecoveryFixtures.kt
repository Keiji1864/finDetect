package com.ghostteam.findetect.data.demo

import android.util.Patterns
import com.ghostteam.findetect.data.remote.dto.*
import com.ghostteam.findetect.domain.model.PasswordValidation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpMethod
import io.ktor.http.content.OutgoingContent
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/** Public recovery protocol plus explicit fault fixtures. Never included in release. */
object DebugPasswordRecoveryFixtures {
    suspend fun response(path: String, request: HttpRequestData, json: Json, profiles: DebugProfileStore): DebugAuthFixtures.Response? {
        if (request.method != HttpMethod.Post || !path.startsWith("/auth/password/reset/")) return null
        val body = (request.body as? OutgoingContent.ByteArrayContent)?.bytes()?.toString(Charsets.UTF_8).orEmpty()
        fun error(status: Int, code: String, field: String? = null) = DebugAuthFixtures.Response(status,
            json.encodeToString(ApiErrorDto(code, "Debug recovery response",
                field?.let { listOf(ApiFieldErrorDto(it, "Debug invalid field")) }.orEmpty())))
        fun validKeys(vararg keys: String) = runCatching {
            (json.parseToJsonElement(body) as JsonObject).keys == keys.toSet()
        }.getOrDefault(false)
        fun reply(result: DebugProfileStore.RecoveryReply): DebugAuthFixtures.Response = when (result.status) {
            202 -> DebugAuthFixtures.Response(202, json.encodeToString(PasswordResetChallengeDto(checkNotNull(result.requestId), 600, 60)))
            200 -> DebugAuthFixtures.Response(200, json.encodeToString(PasswordResetGrantDto(checkNotNull(result.token), 600)))
            204 -> DebugAuthFixtures.Response(204, "")
            else -> error(result.status, checkNotNull(result.error)).copy(retryAfterSeconds = result.retrySeconds)
        }
        return when (path) {
            "/auth/password/reset/request" -> {
                val payload = if (validKeys("email")) runCatching { json.decodeFromString<PasswordResetRequestDto>(body) }.getOrNull() else null
                val email = payload?.email?.trim()?.lowercase()
                if (email == null || email.length > 254 || !Patterns.EMAIL_ADDRESS.matcher(email).matches())
                    return error(400, "VALIDATION_ERROR", "email")
                when (email) {
                    "reset-request-validation@example.com" -> return error(400, "VALIDATION_ERROR", "email")
                    "reset-request-validation-unknown@example.com" -> return error(400, "VALIDATION_ERROR", "unknown")
                    "reset-request-rate-limit@example.com" -> return error(429, "RATE_LIMITED").copy(retryAfterSeconds = 5)
                    "reset-request-rate-limit-no-header@example.com" -> return error(429, "RATE_LIMITED")
                    "reset-request-server@example.com" -> return error(503, "SERVICE_UNAVAILABLE")
                    "reset-request-offline@example.com" -> throw IOException("Debug recovery request offline")
                    "reset-request-slow@example.com" -> delay(2_000)
                }
                val result = profiles.requestPasswordReset(email)
                if (result.status == 202) when (email) {
                    "reset-request-lost@example.com" -> throw IOException("Debug mail request response lost")
                    "reset-request-malformed@example.com" -> return DebugAuthFixtures.Response(202, "{invalid-json")
                }
                reply(result)
            }
            "/auth/password/reset/verify" -> {
                val payload = if (validKeys("requestId", "code")) runCatching { json.decodeFromString<VerifyPasswordResetRequestDto>(body) }.getOrNull() else null
                if (payload == null) return error(400, "VALIDATION_ERROR")
                if (runCatching { UUID.fromString(payload.requestId).toString() }.getOrNull() != payload.requestId.lowercase())
                    return error(400, "VALIDATION_ERROR", "requestId")
                if (!Regex("^[0-9]{6}$").matches(payload.code)) return error(400, "VALIDATION_ERROR", "code")
                val scenario = profiles.recoveryScenario(requestId = payload.requestId)
                when (scenario) {
                    "reset-verify-server" -> return error(500, "INTERNAL_ERROR")
                    "reset-verify-offline" -> throw IOException("Debug verify offline")
                    "reset-verify-slow" -> delay(2_000)
                }
                val result = profiles.verifyPasswordReset(payload.requestId, payload.code)
                if (result.status == 200) when (scenario) {
                    "reset-verify-lost" -> throw IOException("Debug verify response lost after consuming code")
                    "reset-verify-malformed" -> return DebugAuthFixtures.Response(200, "{invalid-json")
                }
                reply(result)
            }
            "/auth/password/reset/complete" -> {
                val payload = if (validKeys("resetToken", "newPassword")) runCatching { json.decodeFromString<CompletePasswordResetRequestDto>(body) }.getOrNull() else null
                if (payload == null) return error(400, "VALIDATION_ERROR")
                if (!Regex("^[A-Za-z0-9_-]{43}$").matches(payload.resetToken)) return error(400, "VALIDATION_ERROR", "resetToken")
                if (payload.newPassword.length !in PasswordValidation.MIN_LENGTH..PasswordValidation.MAX_LENGTH)
                    return error(400, "VALIDATION_ERROR", "newPassword")
                val scenario = profiles.recoveryScenario(token = payload.resetToken)
                when (scenario) {
                    "reset-complete-validation" -> return error(400, "VALIDATION_ERROR", "newPassword")
                    "reset-complete-validation-unknown" -> return error(400, "VALIDATION_ERROR", "unknown")
                    "reset-complete-rate-limit" -> return error(429, "RATE_LIMITED").copy(retryAfterSeconds = 5)
                    "reset-complete-rate-limit-no-header" -> return error(429, "RATE_LIMITED")
                    "reset-complete-server" -> return error(503, "SERVICE_UNAVAILABLE")
                    "reset-complete-offline" -> throw IOException("Debug complete offline")
                    "reset-complete-slow" -> delay(2_000)
                    "reset-token-invalid" -> return error(400, "RESET_TOKEN_INVALID")
                }
                val result = profiles.completePasswordReset(payload.resetToken, payload.newPassword)
                if (result.status == 204) when (scenario) {
                    "reset-complete-lost" -> throw IOException("Debug complete response lost after password change")
                    "reset-complete-timeout" -> delay(16_000)
                    "reset-complete-unexpected-success" -> return DebugAuthFixtures.Response(200, "{}")
                }
                reply(result)
            }
            else -> null
        }
    }
}

package com.ghostteam.findetect.data.auth

import com.ghostteam.findetect.core.di.AuthClient
import com.ghostteam.findetect.data.remote.dto.*
import com.ghostteam.findetect.domain.model.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

/** No bearer interceptor or automatic POST replay. Only changePassword attaches its captured token. */
@Singleton
class PasswordApi @Inject constructor(@AuthClient private val client: HttpClient, private val json: Json) {
    suspend fun changePassword(accessToken: String, currentPassword: String, newPassword: String) {
        request("auth/password/change", ChangePasswordRequestDto(currentPassword, newPassword), 204,
            accessToken = accessToken) { }
    }

    suspend fun requestReset(email: String): PasswordResetChallengeDto =
        request("auth/password/reset/request", PasswordResetRequestDto(email.trim()), 202) { response ->
            val challenge = response.body<PasswordResetChallengeDto>()
            val id = runCatching { UUID.fromString(challenge.requestId).toString() }.getOrNull()
            if (!challenge.requestId.equals(id, ignoreCase = true) || challenge.expiresInSeconds != 600 ||
                challenge.resendAfterSeconds != 60) throw PasswordException(PasswordFailure.BAD_RESPONSE)
            challenge
        }

    suspend fun verifyReset(requestId: String, code: String): PasswordResetGrantDto =
        request("auth/password/reset/verify", VerifyPasswordResetRequestDto(requestId, code), 200) { response ->
            val grant = response.body<PasswordResetGrantDto>()
            if (!RESET_TOKEN.matches(grant.resetToken) || grant.expiresInSeconds != 600)
                throw PasswordException(PasswordFailure.BAD_RESPONSE)
            grant
        }

    suspend fun completeReset(resetToken: String, newPassword: String) {
        request("auth/password/reset/complete", CompletePasswordResetRequestDto(resetToken, newPassword), 204) { }
    }

    private suspend inline fun <reified B : Any, R> request(
        path: String,
        payload: B,
        expectedStatus: Int,
        accessToken: String? = null,
        read: (HttpResponse) -> R,
    ): R {
        try {
            val response = client.post(path) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.CacheControl, "no-store")
                if (accessToken != null) header(HttpHeaders.Authorization, "Bearer $accessToken")
                setBody(payload)
            }
            if (response.status.value == expectedStatus) return read(response)
            if (response.status.value in 200..299) {
                throw PasswordException(PasswordFailure.BAD_RESPONSE, outcomeUnknown = true)
            }
            // A rejected mail queue request preserves the previous challenge. Other 5xx may follow a commit.
            throw failure(response, path != "auth/password/reset/request" || response.status.value != 503)
        } catch (error: CancellationException) { throw error }
        catch (error: PasswordException) {
            if (error.reason == PasswordFailure.BAD_RESPONSE)
                throw PasswordException(PasswordFailure.BAD_RESPONSE, outcomeUnknown = true)
            throw error
        }
        catch (_: HttpRequestTimeoutException) { throw PasswordException(PasswordFailure.TIMEOUT, outcomeUnknown = true) }
        catch (_: SocketTimeoutException) { throw PasswordException(PasswordFailure.TIMEOUT, outcomeUnknown = true) }
        catch (_: IOException) { throw PasswordException(PasswordFailure.NETWORK, outcomeUnknown = true) }
        catch (_: Exception) { throw PasswordException(PasswordFailure.BAD_RESPONSE, outcomeUnknown = true) }
    }

    private suspend fun failure(response: HttpResponse, uncertainServerFailure: Boolean): PasswordException {
        // The status remains authoritative even if the error body is malformed or cannot be read.
        val status = response.status.value
        val retryAfter = response.headers[HttpHeaders.RetryAfter]?.trim()?.toLongOrNull()
            ?.takeIf { it >= 0 }?.coerceAtMost(Int.MAX_VALUE.toLong())
        val error = try { json.decodeFromString<ApiErrorDto>(response.bodyAsText()) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { null }
        val fields = error?.details.orEmpty().mapNotNull { detail ->
            val field = when (detail.field) {
                "currentPassword" -> PasswordField.CURRENT_PASSWORD
                "newPassword" -> PasswordField.NEW_PASSWORD
                "email" -> PasswordField.EMAIL
                "requestId" -> PasswordField.REQUEST_ID
                "code" -> PasswordField.CODE
                "resetToken" -> PasswordField.RESET_TOKEN
                else -> null
            }
            field?.let { it to PasswordFieldError.INVALID_VALUE }
        }.toMap()
        return when {
            status == 401 -> PasswordException(PasswordFailure.UNAUTHORIZED)
            status == 403 && error?.code == "CURRENT_PASSWORD_INVALID" -> PasswordException(
                PasswordFailure.CURRENT_PASSWORD_INVALID,
                mapOf(PasswordField.CURRENT_PASSWORD to PasswordFieldError.CURRENT_PASSWORD_INVALID))
            status == 400 -> when (error?.code) {
                "NEW_PASSWORD_SAME_AS_OLD" -> PasswordException(PasswordFailure.NEW_PASSWORD_SAME_AS_OLD,
                    mapOf(PasswordField.NEW_PASSWORD to PasswordFieldError.SAME_AS_CURRENT))
                "RESET_CODE_INVALID" -> PasswordException(PasswordFailure.RESET_CODE_INVALID)
                "RESET_CODE_EXPIRED" -> PasswordException(PasswordFailure.RESET_CODE_EXPIRED)
                "RESET_TOKEN_INVALID" -> PasswordException(PasswordFailure.RESET_TOKEN_INVALID)
                else -> PasswordException(PasswordFailure.VALIDATION, fields)
            }
            status == 429 -> PasswordException(PasswordFailure.RATE_LIMITED, retryAfterSeconds = retryAfter)
            else -> PasswordException(PasswordFailure.UNAVAILABLE, outcomeUnknown = uncertainServerFailure && status >= 500)
        }
    }

    private companion object { val RESET_TOKEN = Regex("^[A-Za-z0-9_-]{43}$") }
}

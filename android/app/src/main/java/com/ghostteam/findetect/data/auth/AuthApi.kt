package com.ghostteam.findetect.data.auth

import com.ghostteam.findetect.core.di.AuthClient
import com.ghostteam.findetect.data.remote.dto.*
import com.ghostteam.findetect.domain.model.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

/** Dedicated client without bearer/refresh interception, preventing recursive refresh requests. */
@Singleton
class AuthApi @Inject constructor(@AuthClient private val client: HttpClient, private val json: Json) {
    suspend fun login(email: String, password: String): AuthResponseDto = request("auth/login", LoginRequestDto(email, password))
    suspend fun register(name: String, email: String, password: String): AuthResponseDto = request("auth/register", RegisterRequestDto(name, email, password), expectedStatus = 201)
    suspend fun refresh(refreshToken: String): TokenPairDto = request("auth/refresh", RefreshRequestDto(refreshToken))
    suspend fun logout(refreshToken: String) {
        client.post("auth/logout") { contentType(ContentType.Application.Json); setBody(RefreshRequestDto(refreshToken)) }.bodyAsText()
    }

    private suspend inline fun <reified B : Any, reified R : Any> request(path: String, payload: B, expectedStatus: Int = 200): R {
        try {
            val response = client.post(path) { contentType(ContentType.Application.Json); setBody(payload) }
            if (response.status.value in 200..299) {
                val created = path == "auth/register" && response.status.value == 201
                if (response.status.value != expectedStatus) {
                    response.bodyAsText()
                    throw AuthException(AuthFailure.BAD_RESPONSE, accountCreated = created)
                }
                try { return response.body() }
                catch (error: CancellationException) { throw error }
                catch (_: HttpRequestTimeoutException) { throw AuthException(AuthFailure.TIMEOUT, accountCreated = created) }
                catch (_: SocketTimeoutException) { throw AuthException(AuthFailure.TIMEOUT, accountCreated = created) }
                catch (_: IOException) { throw AuthException(AuthFailure.NETWORK, accountCreated = created) }
                catch (_: Exception) { throw AuthException(AuthFailure.BAD_RESPONSE, accountCreated = created) }
            }
            val errorBody = response.bodyAsText()
            val error = runCatching { json.decodeFromString<ApiErrorDto>(errorBody) }.getOrNull()
            val fields = error?.details.orEmpty().mapNotNull { detail ->
                val field = when (detail.field) {
                    "name" -> AuthField.NAME
                    "email" -> AuthField.EMAIL
                    "password" -> AuthField.PASSWORD
                    else -> null
                }
                field?.let { it to AuthFieldError.INVALID_VALUE }
            }.toMap()
            throw when (response.status.value) {
                400 -> AuthException(AuthFailure.VALIDATION, fieldErrors = fields)
                401, 403 -> AuthException(AuthFailure.INVALID_CREDENTIALS)
                409 -> AuthException(AuthFailure.EMAIL_TAKEN, fieldErrors = mapOf(AuthField.EMAIL to AuthFieldError.EMAIL_TAKEN))
                429 -> AuthException(AuthFailure.RATE_LIMITED, retryAfterSeconds = response.headers[HttpHeaders.RetryAfter]
                    ?.trim()?.toLongOrNull()?.takeIf { it >= 0 }?.coerceAtMost(Int.MAX_VALUE.toLong()))
                else -> AuthException(AuthFailure.UNAVAILABLE)
            }
        } catch (error: CancellationException) { throw error }
        catch (error: AuthException) { throw error }
        catch (_: HttpRequestTimeoutException) { throw AuthException(AuthFailure.TIMEOUT) }
        catch (_: SocketTimeoutException) { throw AuthException(AuthFailure.TIMEOUT) }
        catch (_: IOException) { throw AuthException(AuthFailure.NETWORK) }
        catch (_: Exception) { throw AuthException(AuthFailure.UNAVAILABLE) }
    }
}

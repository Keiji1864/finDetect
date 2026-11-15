package com.ghostteam.findetect.data.auth

import com.ghostteam.findetect.core.di.AuthClient
import com.ghostteam.findetect.data.remote.dto.*
import com.ghostteam.findetect.domain.model.AuthException
import com.ghostteam.findetect.domain.model.AuthFailure
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

/** Dedicated client without bearer/refresh interception, preventing recursive refresh requests. */
@Singleton
class AuthApi @Inject constructor(@AuthClient private val client: HttpClient) {
    suspend fun login(email: String, password: String): AuthResponseDto = request("auth/login", LoginRequestDto(email, password))
    suspend fun register(name: String, email: String, password: String): AuthResponseDto = request("auth/register", RegisterRequestDto(name, email, password))
    suspend fun refresh(refreshToken: String): TokenPairDto = request("auth/refresh", RefreshRequestDto(refreshToken))
    suspend fun logout(refreshToken: String) {
        client.post("auth/logout") { contentType(ContentType.Application.Json); setBody(RefreshRequestDto(refreshToken)) }.bodyAsText()
    }

    private suspend inline fun <reified B : Any, reified R : Any> request(path: String, payload: B): R {
        try {
            val response = client.post(path) { contentType(ContentType.Application.Json); setBody(payload) }
            when (response.status.value) {
                in 200..299 -> return response.body()
                else -> {
                    response.bodyAsText()
                    throw AuthException(when (response.status.value) {
                        401, 403 -> AuthFailure.INVALID_CREDENTIALS
                        409 -> AuthFailure.EMAIL_TAKEN
                        else -> AuthFailure.UNAVAILABLE
                    })
                }
            }
        } catch (error: CancellationException) { throw error }
        catch (error: AuthException) { throw error }
        catch (_: IOException) { throw AuthException(AuthFailure.NETWORK) }
        catch (_: Exception) { throw AuthException(AuthFailure.UNAVAILABLE) }
    }
}

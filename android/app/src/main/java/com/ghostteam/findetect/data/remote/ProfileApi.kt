package com.ghostteam.findetect.data.remote

import com.ghostteam.findetect.core.network.ApiStatusException
import com.ghostteam.findetect.core.network.AcceptDeletionReceipt
import com.ghostteam.findetect.core.network.SessionOwner
import com.ghostteam.findetect.data.remote.dto.UpdateProfileRequestDto
import com.ghostteam.findetect.domain.model.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

/** All operations carry the identity captured by their screen, with bearer/refresh handled centrally. */
@Singleton
class ProfileApi @Inject constructor(private val client: HttpClient) {
    suspend fun read(owner: SessionIdentity): SessionUser = request {
        val response = client.get("me") { attributes.put(SessionOwner, owner) }
        if (response.status.value != 200) { response.bodyAsText(); throw ProfileException(ProfileFailure.BAD_RESPONSE) }
        response.body<SessionUser>()
    }

    suspend fun updateName(owner: SessionIdentity, name: String): SessionUser = request {
        val response = client.patch("me") {
            attributes.put(SessionOwner, owner)
            contentType(ContentType.Application.Json)
            setBody(UpdateProfileRequestDto(name))
        }
        if (response.status.value != 200) { response.bodyAsText(); throw ProfileException(ProfileFailure.BAD_RESPONSE) }
        response.body<SessionUser>()
    }

    suspend fun deleteAccount(owner: SessionIdentity): Unit = request {
        val response = client.delete("me") {
            attributes.put(SessionOwner, owner)
            attributes.put(AcceptDeletionReceipt, true)
        }
        if (response.status.value != 204) { response.bodyAsText(); throw ProfileException(ProfileFailure.BAD_RESPONSE) }
    }

    private suspend fun <T> request(action: suspend () -> T): T = try { action() }
    catch (error: CancellationException) { throw error }
    catch (error: ProfileException) { throw error }
    catch (error: ApiStatusException) {
        throw ProfileException(when (error.status) {
            400 -> ProfileFailure.INVALID_NAME
            429 -> ProfileFailure.RATE_LIMITED
            else -> ProfileFailure.UNAVAILABLE
        })
    } catch (error: AuthException) {
        throw ProfileException(when (error.reason) {
            AuthFailure.NETWORK -> ProfileFailure.NETWORK
            AuthFailure.TIMEOUT -> ProfileFailure.TIMEOUT
            AuthFailure.RATE_LIMITED -> ProfileFailure.RATE_LIMITED
            AuthFailure.STORAGE -> ProfileFailure.STORAGE
            else -> ProfileFailure.UNAVAILABLE
        })
    } catch (_: HttpRequestTimeoutException) { throw ProfileException(ProfileFailure.TIMEOUT) }
    catch (_: SocketTimeoutException) { throw ProfileException(ProfileFailure.TIMEOUT) }
    catch (_: IOException) { throw ProfileException(ProfileFailure.NETWORK) }
    catch (_: Exception) { throw ProfileException(ProfileFailure.BAD_RESPONSE) }
}

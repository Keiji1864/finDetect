package com.ghostteam.findetect.data.sync

import com.ghostteam.findetect.core.di.AuthClient
import com.ghostteam.findetect.core.network.SessionOwner
import com.ghostteam.findetect.domain.model.SessionIdentity
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.*
import javax.inject.Inject

class FinanceSyncApi @Inject constructor(private val client: HttpClient, @AuthClient private val startup: HttpClient) {
    internal suspend fun exchange(data: Changes, owner: SessionIdentity): Changes = client.post("sync") {
        attributes.put(SessionOwner,owner)
        contentType(ContentType.Application.Json)
        setBody(data)
    }.body()

    // During activation the bearer belongs to the newly authenticated user; SessionManager is still locked.
    internal suspend fun initial(data: Changes, token: String): Changes {
        val response = startup.post("sync") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(data)
        }
        check(response.status == HttpStatusCode.OK)
        return response.body()
    }
}

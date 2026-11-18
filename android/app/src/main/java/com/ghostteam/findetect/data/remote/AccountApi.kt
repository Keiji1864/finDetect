package com.ghostteam.findetect.data.remote

import com.ghostteam.findetect.data.remote.dto.AccountPageDto
import com.ghostteam.findetect.core.network.SessionOwner
import com.ghostteam.findetect.domain.model.SessionIdentity
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import javax.inject.Inject

class AccountApi @Inject constructor(private val client: HttpClient) {
    suspend fun listAccounts(owner: SessionIdentity): AccountPageDto = client.get("accounts") {
        attributes.put(SessionOwner, owner)
    }.body()
}

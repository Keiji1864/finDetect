package com.ghostteam.findetect.data.remote

import com.ghostteam.findetect.data.remote.dto.AccountPageDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import javax.inject.Inject

class AccountApi @Inject constructor(private val client: HttpClient) {
    suspend fun listAccounts(): AccountPageDto = client.get("accounts").body()
}

package com.ghostteam.findetect.domain.repository

import com.ghostteam.findetect.domain.model.Account
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun observeActive(currency: String? = null): Flow<List<Account>>

    suspend fun getById(id: String): Account?

    suspend fun create(account: Account): Account

    suspend fun update(account: Account): Account

    suspend fun softDelete(id: String): Boolean

    suspend fun getPendingChanges(): List<Account>

    suspend fun acknowledgeUpsert(sent: Account, confirmed: Account): Boolean

    suspend fun acknowledgeDeletion(sent: Account): Boolean
}


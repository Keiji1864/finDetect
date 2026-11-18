package com.ghostteam.findetect.domain.repository

import com.ghostteam.findetect.domain.model.Account
import com.ghostteam.findetect.domain.model.PendingAccountDeletion
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

interface AccountRepository {
    fun observeActive(currency: String? = null): Flow<List<Account>>

    suspend fun getById(id: String): Account?

    suspend fun create(account: Account): Account

    suspend fun update(account: Account): Account

    fun observeHasMovements(id: String): Flow<Boolean>

    /** Durable undo window; a held deletion is not returned by getPendingChanges. */
    suspend fun deleteWithUndo(id: String, expectedModifiedAt: Instant): PendingAccountDeletion

    fun observePendingDeletions(): Flow<List<PendingAccountDeletion>>

    suspend fun undoDelete(deletion: PendingAccountDeletion): Boolean

    suspend fun softDelete(id: String): Boolean

    suspend fun getPendingChanges(): List<Account>

    suspend fun acknowledgeUpsert(sent: Account, confirmed: Account): Boolean

    suspend fun acknowledgeDeletion(sent: Account): Boolean
}


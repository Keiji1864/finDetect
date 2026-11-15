package com.ghostteam.findetect.domain.repository

import com.ghostteam.findetect.domain.model.Transaction
import com.ghostteam.findetect.domain.model.TransactionFilter
import com.ghostteam.findetect.domain.model.TransactionDetails
import com.ghostteam.findetect.domain.model.PendingTransactionDeletion
import kotlinx.datetime.Instant
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun observe(filter: TransactionFilter = TransactionFilter()): Flow<List<Transaction>>

    /**
     * Historical category metadata, including deleted accounts and archived categories.
     * Ordered by operation date, newest first; an optional positive limit is applied in storage.
     */
    fun observeDetails(filter: TransactionFilter, limit: Int? = null): Flow<List<TransactionDetails>>

    /** Includes tombstones so an open editor can detect deletion. */
    fun observeDetailsById(id: String): Flow<TransactionDetails?>

    fun observeTotalAmount(filter: TransactionFilter): Flow<BigDecimal>

    suspend fun getById(id: String): Transaction?

    /**
     * Checks the expected category type together with account/category availability before insertion.
     * Expected validation failures throw TransactionCreationException; database failures propagate.
     */
    suspend fun create(transaction: Transaction, expectedIsIncome: Boolean): Transaction

    /** Checks modifiedAt and the selected account's currency atomically with the update. */
    suspend fun update(transaction: Transaction, expectedCurrency: String): Transaction

    /** Keeps the deletion out of pending sync until the durable undo deadline expires. */
    suspend fun deleteWithUndo(id: String, expectedModifiedAt: Instant): PendingTransactionDeletion

    fun observePendingDeletions(): Flow<List<PendingTransactionDeletion>>

    suspend fun undoDelete(deletion: PendingTransactionDeletion): Boolean

    suspend fun softDelete(id: String): Boolean

    suspend fun getPendingChanges(): List<Transaction>

    suspend fun acknowledgeUpsert(sent: Transaction, confirmed: Transaction): Boolean

    suspend fun acknowledgeDeletion(sent: Transaction): Boolean
}


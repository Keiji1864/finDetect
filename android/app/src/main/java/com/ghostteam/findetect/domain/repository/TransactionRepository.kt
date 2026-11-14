package com.ghostteam.findetect.domain.repository

import com.ghostteam.findetect.domain.model.Transaction
import com.ghostteam.findetect.domain.model.TransactionFilter
import com.ghostteam.findetect.domain.model.TransactionDetails
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun observe(filter: TransactionFilter = TransactionFilter()): Flow<List<Transaction>>

    /**
     * Historical category metadata, including deleted accounts and archived categories.
     * Ordered by operation date, newest first; an optional positive limit is applied in storage.
     */
    fun observeDetails(filter: TransactionFilter, limit: Int? = null): Flow<List<TransactionDetails>>

    fun observeTotalAmount(filter: TransactionFilter): Flow<BigDecimal>

    suspend fun getById(id: String): Transaction?

    /**
     * Checks the expected category type together with account/category availability before insertion.
     * Expected validation failures throw TransactionCreationException; database failures propagate.
     */
    suspend fun create(transaction: Transaction, expectedIsIncome: Boolean): Transaction

    suspend fun update(transaction: Transaction): Transaction

    suspend fun softDelete(id: String): Boolean

    suspend fun getPendingChanges(): List<Transaction>

    suspend fun acknowledgeUpsert(sent: Transaction, confirmed: Transaction): Boolean

    suspend fun acknowledgeDeletion(sent: Transaction): Boolean
}


package com.ghostteam.findetect.domain.repository

import com.ghostteam.findetect.domain.model.Transaction
import com.ghostteam.findetect.domain.model.TransactionFilter
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun observe(filter: TransactionFilter = TransactionFilter()): Flow<List<Transaction>>

    fun observeTotalAmount(filter: TransactionFilter): Flow<BigDecimal>

    suspend fun getById(id: String): Transaction?

    suspend fun create(transaction: Transaction): Transaction

    suspend fun update(transaction: Transaction): Transaction

    suspend fun softDelete(id: String): Boolean

    suspend fun getPendingChanges(): List<Transaction>

    suspend fun acknowledgeUpsert(sent: Transaction, confirmed: Transaction): Boolean

    suspend fun acknowledgeDeletion(sent: Transaction): Boolean
}


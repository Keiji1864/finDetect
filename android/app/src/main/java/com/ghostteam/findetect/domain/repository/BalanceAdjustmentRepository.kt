package com.ghostteam.findetect.domain.repository

import com.ghostteam.findetect.domain.model.AdjustmentFilter
import com.ghostteam.findetect.domain.model.BalanceAdjustment
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow

interface BalanceAdjustmentRepository {
    fun observe(filter: AdjustmentFilter = AdjustmentFilter()): Flow<List<BalanceAdjustment>>

    fun observeTotalAmount(filter: AdjustmentFilter): Flow<BigDecimal>

    suspend fun getById(id: String): BalanceAdjustment?

    suspend fun create(adjustment: BalanceAdjustment): BalanceAdjustment

    suspend fun getPendingChanges(): List<BalanceAdjustment>

    suspend fun acknowledgeCreate(sent: BalanceAdjustment, confirmed: BalanceAdjustment): Boolean
}

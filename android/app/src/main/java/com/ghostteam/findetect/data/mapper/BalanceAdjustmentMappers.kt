package com.ghostteam.findetect.data.mapper

import com.ghostteam.findetect.data.local.entity.AdjustmentEntity
import com.ghostteam.findetect.data.remote.dto.BalanceAdjustmentDto
import com.ghostteam.findetect.data.remote.dto.BalanceAdjustmentRequest
import com.ghostteam.findetect.domain.model.BalanceAdjustment
import com.ghostteam.findetect.domain.model.SyncStatus
import com.ghostteam.findetect.domain.model.toMinorUnits
import com.ghostteam.findetect.domain.model.toMoneyAmount

fun AdjustmentEntity.toDomain(): BalanceAdjustment = BalanceAdjustment(
    id = id,
    accountId = accountId,
    amount = amountMinor.toMoneyAmount(),
    occurredAt = occurredAt,
    createdAt = createdAt,
    syncStatus = syncStatus,
)

fun BalanceAdjustment.toEntity(): AdjustmentEntity = AdjustmentEntity(
    id = id,
    accountId = accountId,
    amountMinor = checkedMoney(amount, MoneyRule.SIGNED_NON_ZERO).toMinorUnits(),
    occurredAt = occurredAt,
    createdAt = createdAt,
    syncStatus = syncStatus,
)

fun BalanceAdjustmentDto.toDomain(): BalanceAdjustment = BalanceAdjustment(
    id = id,
    accountId = accountId,
    amount = parseMoney(amount, MoneyRule.SIGNED_NON_ZERO),
    occurredAt = occurredAt,
    createdAt = createdAt,
    syncStatus = SyncStatus.SYNCED,
)

fun BalanceAdjustmentDto.toEntity(): AdjustmentEntity = toDomain().toEntity()

fun BalanceAdjustment.toRequest(): BalanceAdjustmentRequest = BalanceAdjustmentRequest(
    accountId = accountId,
    amount = formatMoney(amount, MoneyRule.SIGNED_NON_ZERO),
    occurredAt = occurredAt,
)

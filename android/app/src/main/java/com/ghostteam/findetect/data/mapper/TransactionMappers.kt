package com.ghostteam.findetect.data.mapper

import com.ghostteam.findetect.data.local.entity.TransactionEntity
import com.ghostteam.findetect.data.remote.dto.TransactionDto
import com.ghostteam.findetect.data.remote.dto.TransactionUpsertRequest
import com.ghostteam.findetect.domain.model.SyncStatus
import com.ghostteam.findetect.domain.model.Transaction
import com.ghostteam.findetect.domain.model.toMinorUnits
import com.ghostteam.findetect.domain.model.toMoneyAmount

fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    accountId = accountId,
    categoryId = categoryId,
    amount = amountMinor.toMoneyAmount(),
    transactionDate = transactionDate,
    enteredAt = enteredAt,
    comment = comment,
    modifiedAt = modifiedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncStatus = syncStatus,
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    accountId = accountId,
    categoryId = categoryId,
    amountMinor = checkedMoney(amount, MoneyRule.POSITIVE).toMinorUnits(),
    transactionDate = transactionDate,
    enteredAt = enteredAt,
    comment = comment,
    modifiedAt = modifiedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncStatus = syncStatus,
)

fun TransactionDto.toDomain(): Transaction = Transaction(
    id = id,
    accountId = accountId,
    categoryId = categoryId,
    amount = parseMoney(amount, MoneyRule.POSITIVE),
    transactionDate = transactionDate,
    enteredAt = enteredAt,
    comment = comment,
    modifiedAt = modifiedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncStatus = SyncStatus.SYNCED,
)

fun TransactionDto.toEntity(): TransactionEntity = toDomain().toEntity()

fun Transaction.toUpsertRequest(): TransactionUpsertRequest {
    require(!isDeleted) { "Deleted transactions must be sent with DELETE" }
    require(comment == null || comment.length <= 200) { "Comment exceeds 200 characters" }
    return TransactionUpsertRequest(
        accountId = accountId,
        categoryId = categoryId,
        amount = formatMoney(amount, MoneyRule.POSITIVE),
        transactionDate = transactionDate,
        enteredAt = enteredAt,
        modifiedAt = modifiedAt,
        comment = comment,
    )
}

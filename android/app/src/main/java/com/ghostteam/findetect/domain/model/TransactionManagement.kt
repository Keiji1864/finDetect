package com.ghostteam.findetect.domain.model

import kotlinx.datetime.Instant

enum class TransactionManagementError {
    INVALID_AMOUNT, INVALID_DATE, COMMENT_TOO_LONG, NOT_FOUND, CHANGED,
    ACCOUNT_UNAVAILABLE, ACCOUNT_CURRENCY_CHANGED, CATEGORY_UNAVAILABLE, STORAGE_FAILURE,
}

class TransactionManagementException(val error: TransactionManagementError) : IllegalStateException(error.name)

data class PendingTransactionDeletion(
    val transactionId: String,
    val deletedAt: Instant,
    val undoUntil: Instant,
)

sealed interface UpdateTransactionResult {
    data class Success(val transaction: Transaction) : UpdateTransactionResult
    data class Failure(val error: TransactionManagementError) : UpdateTransactionResult
}

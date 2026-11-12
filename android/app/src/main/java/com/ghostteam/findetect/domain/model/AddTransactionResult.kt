package com.ghostteam.findetect.domain.model

sealed interface AddTransactionResult {
    data class Success(val transaction: Transaction) : AddTransactionResult

    /** The cause is for diagnostics, not for displaying a message directly to the user. */
    data class Failure(
        val error: TransactionCreationError,
        val cause: Throwable? = null,
    ) : AddTransactionResult
}

package com.ghostteam.findetect.domain.model

/** Stable reasons for callers to map to their own localized messages. */
enum class TransactionCreationError {
    INVALID_AMOUNT,
    ACCOUNT_REQUIRED,
    ACCOUNT_NOT_FOUND,
    ACCOUNT_DELETED,
    CATEGORY_REQUIRED,
    CATEGORY_NOT_FOUND,
    CATEGORY_ARCHIVED,
    CATEGORY_TYPE_MISMATCH,
    COMMENT_TOO_LONG,
    INVALID_TRANSACTION_ID,
    DUPLICATE_TRANSACTION_ID,
    TRANSACTION_ALREADY_DELETED,
    INVALID_DATE,
    STORAGE_FAILURE,
}

/** Expected creation rejections from the repository; database failures keep their original type. */
class TransactionCreationException(
    val error: TransactionCreationError,
    cause: Throwable? = null,
) : IllegalStateException(error.name, cause)

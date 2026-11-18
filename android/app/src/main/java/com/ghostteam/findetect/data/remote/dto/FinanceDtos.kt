package com.ghostteam.findetect.data.remote.dto

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class AccountDto(
    val id: String,
    val name: String,
    val iconId: String,
    val currency: String,
    val initialBalance: String,
    val includeInForecast: Boolean,
    val modifiedAt: Instant,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant?,
)

@Serializable
data class AccountUpsertRequest(
    val name: String,
    val iconId: String,
    val currency: String,
    val initialBalance: String,
    val modifiedAt: Instant,
    val includeInForecast: Boolean = true,
)

@Serializable
data class CategoryNamesDto(val ru: String, val en: String)

@Serializable
data class CategoryDto(
    val id: String,
    val key: String,
    val names: CategoryNamesDto,
    val iconId: String,
    val isIncome: Boolean,
    val isArchived: Boolean,
)

@Serializable
data class TransactionDto(
    val id: String,
    val accountId: String,
    val categoryId: String,
    val amount: String,
    val transactionDate: Instant,
    val comment: String?,
    val enteredAt: Instant,
    val modifiedAt: Instant,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant?,
)

@Serializable
data class TransactionUpsertRequest(
    val accountId: String,
    val categoryId: String,
    val amount: String,
    val transactionDate: Instant,
    val enteredAt: Instant,
    val modifiedAt: Instant,
    val comment: String? = null,
)

@Serializable
data class BalanceAdjustmentDto(
    val id: String,
    val accountId: String,
    val amount: String,
    val occurredAt: Instant,
    val createdAt: Instant,
)

@Serializable
data class BalanceAdjustmentRequest(
    val accountId: String,
    val amount: String,
    val occurredAt: Instant,
)

@Serializable
data class AccountPageDto(val items: List<AccountDto>, val nextCursor: String?)

@Serializable
data class CategoryListDto(val items: List<CategoryDto>)

@Serializable
data class TransactionPageDto(val items: List<TransactionDto>, val nextCursor: String?)

@Serializable
data class BalanceAdjustmentPageDto(val items: List<BalanceAdjustmentDto>, val nextCursor: String?)

package com.ghostteam.findetect.data.mapper

import com.ghostteam.findetect.data.local.entity.AccountEntity
import com.ghostteam.findetect.data.remote.dto.AccountDto
import com.ghostteam.findetect.data.remote.dto.AccountUpsertRequest
import com.ghostteam.findetect.domain.model.Account
import com.ghostteam.findetect.domain.model.AppCurrency
import com.ghostteam.findetect.domain.model.SyncStatus
import com.ghostteam.findetect.domain.model.toMinorUnits
import com.ghostteam.findetect.domain.model.toMoneyAmount

fun AccountEntity.toDomain(): Account = Account(
    id = id,
    name = name,
    iconId = iconId,
    currency = currency,
    initialBalance = initialBalanceMinor.toMoneyAmount(),
    includeInForecast = includeInForecast,
    modifiedAt = modifiedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncStatus = syncStatus,
)

fun Account.toEntity(legacyEmoji: String? = null): AccountEntity = AccountEntity(
    id = id,
    name = name,
    iconId = iconId,
    currency = currency,
    initialBalanceMinor = checkedMoney(initialBalance, MoneyRule.NON_NEGATIVE).toMinorUnits(),
    includeInForecast = includeInForecast,
    modifiedAt = modifiedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncStatus = syncStatus,
    legacyEmoji = legacyEmoji,
)

fun AccountDto.toDomain(): Account = Account(
    id = id,
    name = name,
    iconId = iconId,
    currency = currency,
    initialBalance = parseMoney(initialBalance, MoneyRule.NON_NEGATIVE),
    includeInForecast = includeInForecast,
    modifiedAt = modifiedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncStatus = SyncStatus.SYNCED,
)

fun AccountDto.toEntity(legacyEmoji: String? = null): AccountEntity = toDomain().toEntity(legacyEmoji)

fun Account.toUpsertRequest(): AccountUpsertRequest {
    require(!isDeleted) { "Deleted accounts must be sent with DELETE" }
    require(name.length in 1..100) { "Account name must contain 1..100 characters" }
    require(iconId.isNotBlank()) { "An iconId is required" }
    require(AppCurrency.entries.any { it.name == currency }) { "Unsupported account currency" }
    return AccountUpsertRequest(
        name = name,
        iconId = iconId,
        currency = currency,
        initialBalance = formatMoney(initialBalance, MoneyRule.NON_NEGATIVE),
        includeInForecast = includeInForecast,
        modifiedAt = modifiedAt,
    )
}

package com.ghostteam.findetect.domain.repository

import com.ghostteam.findetect.domain.model.AccountBalance
import kotlinx.coroutines.flow.Flow

interface BalanceRepository {
    /** Includes deleted accounts; null only when the ID does not exist. */
    fun observeAccount(accountId: String): Flow<AccountBalance?>

    /** Only active accounts of this currency; includeInForecast does not affect this list. */
    fun observeAccounts(currency: String): Flow<List<AccountBalance>>
}

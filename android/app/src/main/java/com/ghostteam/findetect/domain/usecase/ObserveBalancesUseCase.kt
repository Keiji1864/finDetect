package com.ghostteam.findetect.domain.usecase

import com.ghostteam.findetect.domain.model.AppCurrency
import com.ghostteam.findetect.domain.model.BalanceSummary
import com.ghostteam.findetect.domain.repository.BalanceRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveBalancesUseCase @Inject constructor(
    private val balanceRepository: BalanceRepository,
) {
    operator fun invoke(currency: String): Flow<BalanceSummary> {
        require(AppCurrency.entries.any { it.name == currency }) { "Unsupported balance currency" }
        return balanceRepository.observeAccounts(currency).map { accounts -> BalanceSummary(currency, accounts) }
    }
}

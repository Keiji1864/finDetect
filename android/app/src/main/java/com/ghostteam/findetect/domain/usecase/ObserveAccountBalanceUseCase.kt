package com.ghostteam.findetect.domain.usecase

import com.ghostteam.findetect.domain.model.AccountBalance
import com.ghostteam.findetect.domain.repository.BalanceRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveAccountBalanceUseCase @Inject constructor(
    private val balanceRepository: BalanceRepository,
) {
    operator fun invoke(accountId: String): Flow<AccountBalance?> {
        require(accountId.isNotBlank()) { "An account ID is required" }
        return balanceRepository.observeAccount(accountId)
    }
}

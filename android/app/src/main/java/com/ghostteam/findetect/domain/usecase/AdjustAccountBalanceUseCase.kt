package com.ghostteam.findetect.domain.usecase

import com.ghostteam.findetect.domain.model.AccountManagementError
import com.ghostteam.findetect.domain.model.AccountManagementException
import com.ghostteam.findetect.domain.model.AdjustAccountBalanceResult
import com.ghostteam.findetect.domain.model.AppCurrency
import com.ghostteam.findetect.domain.repository.BalanceAdjustmentRepository
import com.ghostteam.findetect.domain.validation.InitialBalanceParser
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

class AdjustAccountBalanceUseCase @Inject constructor(
    private val repository: BalanceAdjustmentRepository,
    private val parser: InitialBalanceParser,
) {
    suspend operator fun invoke(
        accountId: String,
        actualBalanceText: String,
        currency: String,
        requestId: String,
    ): AdjustAccountBalanceResult {
        val actual = parser.parse(actualBalanceText)
            ?: return AdjustAccountBalanceResult.Failure(AccountManagementError.INVALID_BALANCE)
        if (AppCurrency.entries.none { it.name == currency }) {
            return AdjustAccountBalanceResult.Failure(AccountManagementError.INVALID_CURRENCY)
        }
        return try {
            AdjustAccountBalanceResult.Success(repository.setActualBalance(accountId, actual, currency, requestId))
        } catch (error: CancellationException) {
            throw error
        } catch (error: AccountManagementException) {
            AdjustAccountBalanceResult.Failure(error.error)
        } catch (_: Exception) {
            AdjustAccountBalanceResult.Failure(AccountManagementError.STORAGE_FAILURE)
        }
    }
}

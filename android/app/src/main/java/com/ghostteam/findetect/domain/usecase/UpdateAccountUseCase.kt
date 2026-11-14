package com.ghostteam.findetect.domain.usecase

import com.ghostteam.findetect.domain.model.Account
import com.ghostteam.findetect.domain.model.AccountManagementError
import com.ghostteam.findetect.domain.model.AccountManagementException
import com.ghostteam.findetect.domain.model.AppCurrency
import com.ghostteam.findetect.domain.model.UpdateAccountResult
import com.ghostteam.findetect.domain.repository.AccountRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

class UpdateAccountUseCase @Inject constructor(private val repository: AccountRepository) {
    suspend operator fun invoke(account: Account): UpdateAccountResult {
        val validation = when {
            account.name.trim().length !in 1..100 -> AccountManagementError.INVALID_NAME
            AppCurrency.entries.none { it.name == account.currency } -> AccountManagementError.INVALID_CURRENCY
            account.iconId.isBlank() -> AccountManagementError.INVALID_ICON
            else -> null
        }
        if (validation != null) return UpdateAccountResult.Failure(validation)
        return try {
            UpdateAccountResult.Success(repository.update(account.copy(name = account.name.trim())))
        } catch (error: CancellationException) {
            throw error
        } catch (error: AccountManagementException) {
            UpdateAccountResult.Failure(error.error)
        } catch (_: Exception) {
            UpdateAccountResult.Failure(AccountManagementError.STORAGE_FAILURE)
        }
    }
}

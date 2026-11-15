package com.ghostteam.findetect.domain.usecase

import com.ghostteam.findetect.domain.model.Account
import com.ghostteam.findetect.domain.model.AccountCreationError
import com.ghostteam.findetect.domain.model.CreateAccountInput
import com.ghostteam.findetect.domain.model.CreateAccountResult
import com.ghostteam.findetect.domain.repository.AccountRepository
import com.ghostteam.findetect.domain.validation.InitialBalanceParser
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.Clock

class CreateAccountUseCase @Inject constructor(
    private val repository: AccountRepository,
    private val parser: InitialBalanceParser,
    private val clock: Clock,
) {
    suspend operator fun invoke(input: CreateAccountInput): CreateAccountResult {
        input.validationError(parser)?.let { return CreateAccountResult.Failure(it) }
        val account = Account(
            id = UUID.randomUUID().toString(),
            name = input.name.trim(),
            currency = input.currency,
            initialBalance = requireNotNull(parser.parse(input.initialBalanceText)),
            iconId = input.iconId,
            includeInForecast = input.includeInForecast,
            modifiedAt = clock.now(),
        )
        return try {
            CreateAccountResult.Success(repository.create(account))
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            CreateAccountResult.Failure(AccountCreationError.STORAGE_FAILURE)
        }
    }
}

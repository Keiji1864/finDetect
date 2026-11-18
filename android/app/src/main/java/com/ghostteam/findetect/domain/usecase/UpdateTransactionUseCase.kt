package com.ghostteam.findetect.domain.usecase

import com.ghostteam.findetect.domain.model.Transaction
import com.ghostteam.findetect.domain.model.TransactionManagementError
import com.ghostteam.findetect.domain.model.TransactionManagementException
import com.ghostteam.findetect.domain.model.UpdateTransactionResult
import com.ghostteam.findetect.domain.repository.TransactionRepository
import com.ghostteam.findetect.domain.validation.TransactionAmountParser
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.DateTimeArithmeticException
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

class UpdateTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository,
    private val amountParser: TransactionAmountParser,
) {
    suspend operator fun invoke(
        original: Transaction,
        amountText: String,
        accountId: String,
        expectedCurrency: String,
        categoryId: String,
        date: LocalDate,
        zone: TimeZone,
        comment: String,
    ): UpdateTransactionResult {
        val amount = amountParser.parse(amountText)
            ?: return UpdateTransactionResult.Failure(TransactionManagementError.INVALID_AMOUNT)
        if (comment.length > 200) return UpdateTransactionResult.Failure(TransactionManagementError.COMMENT_TOO_LONG)
        val transactionDate = try {
            val local = original.transactionDate.toLocalDateTime(zone)
            // Retain the exact instant on an unchanged date, including during a DST overlap.
            if (date == local.date) original.transactionDate else LocalDateTime(date, local.time).toInstant(zone)
        } catch (error: DateTimeArithmeticException) {
            return UpdateTransactionResult.Failure(TransactionManagementError.INVALID_DATE)
        } catch (error: IllegalArgumentException) {
            return UpdateTransactionResult.Failure(TransactionManagementError.INVALID_DATE)
        }
        return try {
            UpdateTransactionResult.Success(repository.update(original.copy(
                amount = amount, accountId = accountId, categoryId = categoryId,
                transactionDate = transactionDate, comment = comment.takeUnless { it.isBlank() },
            ), expectedCurrency))
        } catch (error: CancellationException) {
            throw error
        } catch (error: TransactionManagementException) {
            UpdateTransactionResult.Failure(error.error)
        } catch (error: Exception) {
            UpdateTransactionResult.Failure(TransactionManagementError.STORAGE_FAILURE)
        }
    }
}

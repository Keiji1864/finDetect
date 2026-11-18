package com.ghostteam.findetect.domain.usecase

import com.ghostteam.findetect.domain.model.AddTransactionInput
import com.ghostteam.findetect.domain.model.AddTransactionResult
import com.ghostteam.findetect.domain.model.Transaction
import com.ghostteam.findetect.domain.model.TransactionCreationError
import com.ghostteam.findetect.domain.model.TransactionCreationException
import com.ghostteam.findetect.domain.repository.TransactionRepository
import com.ghostteam.findetect.domain.validation.TransactionAmountParser
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeArithmeticException
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

class AddTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val amountParser: TransactionAmountParser,
    private val clock: Clock,
) {
    suspend operator fun invoke(input: AddTransactionInput): AddTransactionResult {
        val amount = amountParser.parse(input.amountText)
            ?: return AddTransactionResult.Failure(TransactionCreationError.INVALID_AMOUNT)
        val accountId = input.accountId?.takeIf { it.isNotBlank() }
            ?: return AddTransactionResult.Failure(TransactionCreationError.ACCOUNT_REQUIRED)
        val categoryId = input.categoryId?.takeIf { it.isNotBlank() }
            ?: return AddTransactionResult.Failure(TransactionCreationError.CATEGORY_REQUIRED)
        val comment = input.comment?.takeUnless { it.isBlank() }
        if (comment != null && comment.length > 200) {
            return AddTransactionResult.Failure(TransactionCreationError.COMMENT_TOO_LONG)
        }

        val enteredAt = clock.now()
        val transactionDate = try {
            val zone = TimeZone.currentSystemDefault()
            val localNow = enteredAt.toLocalDateTime(zone)
            // Keep the actual instant for today, including the correct offset during a DST overlap.
            if (input.date == localNow.date) enteredAt else LocalDateTime(input.date, localNow.time).toInstant(zone)
        } catch (error: DateTimeArithmeticException) {
            return AddTransactionResult.Failure(TransactionCreationError.INVALID_DATE, error)
        } catch (error: IllegalArgumentException) {
            return AddTransactionResult.Failure(TransactionCreationError.INVALID_DATE, error)
        }
        val transaction = Transaction(
            id = UUID.randomUUID().toString(),
            accountId = accountId,
            categoryId = categoryId,
            amount = amount,
            transactionDate = transactionDate,
            enteredAt = enteredAt,
            modifiedAt = enteredAt,
            comment = comment,
        )

        return try {
            AddTransactionResult.Success(transactionRepository.create(transaction, expectedIsIncome = input.isIncome))
        } catch (error: CancellationException) {
            throw error
        } catch (error: TransactionCreationException) {
            AddTransactionResult.Failure(error.error, error)
        } catch (error: Exception) {
            AddTransactionResult.Failure(TransactionCreationError.STORAGE_FAILURE, error)
        }
    }
}

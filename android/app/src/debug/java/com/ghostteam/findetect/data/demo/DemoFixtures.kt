package com.ghostteam.findetect.data.demo

import com.ghostteam.findetect.data.remote.dto.AccountDto
import com.ghostteam.findetect.data.remote.dto.BalanceAdjustmentDto
import com.ghostteam.findetect.data.remote.dto.CategoryDto
import com.ghostteam.findetect.data.remote.dto.CategoryNamesDto
import com.ghostteam.findetect.data.remote.dto.TransactionDto
import kotlin.time.Duration.Companion.days
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

/** Stable IDs keep the static HTTP fixtures consistent with the initial Room dataset. */
object DemoFixtures {
    private const val SALARY = "10000000-0000-4000-8000-000000000001"
    private const val GROCERIES = "10000000-0000-4000-8000-000000000002"
    private const val COFFEE = "10000000-0000-4000-8000-000000000003"
    private const val RUB_ACCOUNT = "20000000-0000-4000-8000-000000000001"
    private const val EUR_ACCOUNT = "20000000-0000-4000-8000-000000000002"

    fun create(): DemoData {
        val now = Clock.System.now()
        val date = now.toLocalDateTime(TimeZone.UTC).date
        val monthStart = LocalDate(date.year, date.month, 1).atStartOfDayIn(TimeZone.UTC)
        // Keep recent fixture dates inside the current UTC month, even on its first days.
        fun recent(daysAgo: Int): Instant = maxOf(monthStart, now - daysAgo.days)

        val categories = listOf(
            CategoryDto(SALARY, "salary", CategoryNamesDto("Зарплата", "Salary"), "cash", true, false),
            CategoryDto(GROCERIES, "groceries", CategoryNamesDto("Продукты", "Groceries"), "basket", false, false),
            CategoryDto(COFFEE, "coffee", CategoryNamesDto("Кофе", "Coffee"), "coffee", false, false),
        )
        val accounts = listOf(
            AccountDto(
                id = RUB_ACCOUNT,
                name = "Основной счёт",
                iconId = "wallet",
                currency = "RUB",
                initialBalance = "10000.00",
                includeInForecast = true,
                modifiedAt = recent(7),
                createdAt = recent(7),
                updatedAt = recent(7),
                deletedAt = null,
            ),
            AccountDto(
                id = EUR_ACCOUNT,
                name = "Поездки",
                iconId = "cash",
                currency = "EUR",
                initialBalance = "300.00",
                includeInForecast = false,
                modifiedAt = recent(7),
                createdAt = recent(7),
                updatedAt = recent(7),
                deletedAt = null,
            ),
        )
        fun operation(
            id: String,
            accountId: String,
            categoryId: String,
            amount: String,
            daysAgo: Int,
            comment: String,
        ): TransactionDto {
            val occurredAt = recent(daysAgo)
            return TransactionDto(
                id = id,
                accountId = accountId,
                categoryId = categoryId,
                amount = amount,
                transactionDate = occurredAt,
                comment = comment,
                enteredAt = occurredAt,
                modifiedAt = occurredAt,
                createdAt = occurredAt,
                updatedAt = occurredAt,
                deletedAt = null,
            )
        }
        val transactions = listOf(
            operation("30000000-0000-4000-8000-000000000001", RUB_ACCOUNT, SALARY, "25000.00", 5, "Зарплата"),
            operation("30000000-0000-4000-8000-000000000002", RUB_ACCOUNT, GROCERIES, "1230.50", 3, "Продукты на неделю"),
            operation("30000000-0000-4000-8000-000000000003", RUB_ACCOUNT, COFFEE, "12.30", 1, "Кофе с утра"),
            operation("30000000-0000-4000-8000-000000000004", EUR_ACCOUNT, COFFEE, "8.90", 1, "Кофе в поездке"),
        )
        val adjustment = BalanceAdjustmentDto(
            id = "40000000-0000-4000-8000-000000000001",
            accountId = RUB_ACCOUNT,
            amount = "-20.00",
            occurredAt = recent(1),
            createdAt = recent(1),
        )
        return DemoData(categories, accounts, transactions, listOf(adjustment))
    }
}

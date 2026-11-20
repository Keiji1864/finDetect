package com.ghostteam.findetect.data.local.dao

import androidx.room.*
import kotlinx.datetime.Instant

@Entity(tableName = "forecast_days", indices = [Index(value = ["currency", "day"])])
data class ForecastDay(@PrimaryKey val id: String, val currency: String, val day: String, val stamp: Long,
    val accountIds: String, val fromUtc: Instant, val untilUtc: Instant, val limitMinor: Long?)

@Entity(tableName = "forecast_balances", indices = [Index(value = ["currency", "stamp"])])
data class ForecastBalance(@PrimaryKey val id: String, val currency: String, val stamp: Long, val localDay: String,
    val targetDay: String?, val rule: String?, val accountIds: String, val balanceMinor: Long)

data class DayEvidence(val currency: String, val day: String, val limitMinor: Long?, val spent: Long?, val confirmed: Boolean)

@Dao
interface ForecastHistoryDao {
    @Insert suspend fun insert(row: ForecastDay)
    @Insert suspend fun insert(row: ForecastBalance)

    @Query("SELECT EXISTS(SELECT 1 FROM forecast_days WHERE currency=:currency AND day=:day)")
    suspend fun hasDay(currency: String, day: String): Boolean

    @Query("""SELECT EXISTS(SELECT 1 FROM transactions t JOIN accounts a ON a.id=t.accountId
        JOIN categories c ON c.id=t.categoryId WHERE a.currency=:currency AND c.isIncome=0
        AND t.transactionDate >= :start AND t.transactionDate < :end)""")
    suspend fun alreadySpent(currency: String, start: Instant, end: Instant): Boolean

    @Query("""SELECT d.currency, d.day, d.limitMinor,
        (SELECT SUM(t.amountMinor) FROM transactions t JOIN accounts a ON a.id=t.accountId
         JOIN categories c ON c.id=t.categoryId WHERE a.currency=d.currency AND c.isIncome=0 AND t.deletedAt IS NULL
         AND t.transactionDate >= d.fromUtc AND t.transactionDate < d.untilUtc) AS spent,
        EXISTS(SELECT 1 FROM activity_days a WHERE a.date=d.day AND a.noSpendingConfirmed=1) AS confirmed
        FROM forecast_days d WHERE d.untilUtc <= :now AND d.id=(SELECT f.id FROM forecast_days f
        WHERE f.currency=d.currency AND f.day=d.day ORDER BY f.stamp, f.id LIMIT 1)
        AND NOT EXISTS(SELECT 1 FROM transaction_deletions u JOIN transactions t ON t.id=u.transactionId
        JOIN accounts a ON a.id=t.accountId JOIN categories c ON c.id=t.categoryId
        WHERE u.undoUntil > :now AND a.currency=d.currency AND c.isIncome=0
        AND t.transactionDate >= d.fromUtc AND t.transactionDate < d.untilUtc)
        ORDER BY d.currency, d.day""")
    suspend fun completedDays(now: Instant): List<DayEvidence>

    @Query("SELECT * FROM forecast_balances WHERE currency=:currency ORDER BY stamp DESC, id DESC LIMIT 1")
    suspend fun last(currency: String): ForecastBalance?

    @Query("SELECT * FROM forecast_balances ORDER BY currency, stamp, id")
    suspend fun balances(): List<ForecastBalance>
}

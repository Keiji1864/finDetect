package com.ghostteam.findetect.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.ghostteam.findetect.data.local.model.AccountBalanceRow
import kotlinx.coroutines.flow.Flow

// Aggregate each child table before joining it to accounts, avoiding multiplication of amounts.
private const val BALANCE_SELECT = """
    SELECT a.*,
        COALESCE(m.incomeMinor, 0) AS incomeMinor,
        COALESCE(m.expenseMinor, 0) AS expenseMinor,
        COALESCE(b.adjustmentMinor, 0) AS adjustmentMinor
    FROM accounts AS a
    LEFT JOIN (
        SELECT t.accountId,
            SUM(CASE WHEN c.isIncome = 1 THEN t.amountMinor ELSE 0 END) AS incomeMinor,
            SUM(CASE WHEN c.isIncome = 0 THEN t.amountMinor ELSE 0 END) AS expenseMinor
        FROM transactions AS t
        INNER JOIN categories AS c ON c.id = t.categoryId
        WHERE t.deletedAt IS NULL
        GROUP BY t.accountId
    ) AS m ON m.accountId = a.id
    LEFT JOIN (
        SELECT accountId, SUM(amountMinor) AS adjustmentMinor
        FROM balance_adjustments
        GROUP BY accountId
    ) AS b ON b.accountId = a.id
"""

@Dao
interface BalanceDao {
    @Query(BALANCE_SELECT + " WHERE a.id = :accountId")
    fun observeAccount(accountId: String): Flow<AccountBalanceRow?>

    @Query(BALANCE_SELECT + " WHERE a.deletedAt IS NULL AND a.currency = :currency ORDER BY a.name, a.id")
    fun observeAccounts(currency: String): Flow<List<AccountBalanceRow>>
}

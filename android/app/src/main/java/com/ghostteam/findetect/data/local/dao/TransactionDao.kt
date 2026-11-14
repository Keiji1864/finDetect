package com.ghostteam.findetect.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ghostteam.findetect.data.local.entity.TransactionEntity
import com.ghostteam.findetect.data.local.model.TransactionDetailsRow
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

@Dao
interface TransactionDao {
    @Query(
        """SELECT t.*, a.currency AS currency, c.`key` AS categoryKey,
            c.nameRu AS categoryNameRu, c.nameEn AS categoryNameEn, c.iconId AS categoryIconId,
            c.isIncome AS categoryIsIncome, c.isArchived AS categoryIsArchived
            FROM transactions AS t
            INNER JOIN accounts AS a ON a.id = t.accountId
            INNER JOIN categories AS c ON c.id = t.categoryId
            WHERE t.deletedAt IS NULL
                AND (:currency IS NULL OR a.currency = :currency)
                AND (:accountId IS NULL OR t.accountId = :accountId)
                AND (:categoryId IS NULL OR t.categoryId = :categoryId)
                AND (:isIncome IS NULL OR c.isIncome = :isIncome)
                AND (:fromInclusive IS NULL OR t.transactionDate >= :fromInclusive)
                AND (:toExclusive IS NULL OR t.transactionDate < :toExclusive)
            ORDER BY t.transactionDate DESC, t.id DESC""",
    )
    fun observeDetails(
        currency: String?,
        accountId: String?,
        categoryId: String?,
        isIncome: Boolean?,
        fromInclusive: Instant?,
        toExclusive: Instant?,
    ): Flow<List<TransactionDetailsRow>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: TransactionEntity)

    @Update
    suspend fun update(entity: TransactionEntity): Int

    @Query(
        """SELECT t.* FROM transactions AS t
            INNER JOIN accounts AS a ON a.id = t.accountId
            INNER JOIN categories AS c ON c.id = t.categoryId
            WHERE t.deletedAt IS NULL
                AND (:currency IS NULL OR a.currency = :currency)
                AND (:accountId IS NULL OR t.accountId = :accountId)
                AND (:categoryId IS NULL OR t.categoryId = :categoryId)
                AND (:isIncome IS NULL OR c.isIncome = :isIncome)
                AND (:fromInclusive IS NULL OR t.transactionDate >= :fromInclusive)
                AND (:toExclusive IS NULL OR t.transactionDate < :toExclusive)
            ORDER BY t.transactionDate DESC, t.id DESC""",
    )
    fun observeFiltered(
        currency: String?,
        accountId: String?,
        categoryId: String?,
        isIncome: Boolean?,
        fromInclusive: Instant?,
        toExclusive: Instant?,
    ): Flow<List<TransactionEntity>>

    @Query(
        """SELECT COALESCE(SUM(t.amountMinor), 0) FROM transactions AS t
            INNER JOIN accounts AS a ON a.id = t.accountId
            INNER JOIN categories AS c ON c.id = t.categoryId
            WHERE t.deletedAt IS NULL
                AND (:currency IS NULL OR a.currency = :currency)
                AND (:accountId IS NULL OR t.accountId = :accountId)
                AND (:categoryId IS NULL OR t.categoryId = :categoryId)
                AND (:isIncome IS NULL OR c.isIncome = :isIncome)
                AND (:fromInclusive IS NULL OR t.transactionDate >= :fromInclusive)
                AND (:toExclusive IS NULL OR t.transactionDate < :toExclusive)""",
    )
    fun observeTotalMinor(
        currency: String?,
        accountId: String?,
        categoryId: String?,
        isIncome: Boolean?,
        fromInclusive: Instant?,
        toExclusive: Instant?,
    ): Flow<Long>

    @Query("SELECT * FROM transactions WHERE syncStatus != 'SYNCED' ORDER BY modifiedAt, id")
    suspend fun getPendingChanges(): List<TransactionEntity>
}

package com.ghostteam.findetect.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ghostteam.findetect.data.local.entity.AdjustmentEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

@Dao
interface AdjustmentDao {
    @Query("SELECT * FROM balance_adjustments WHERE id = :id")
    suspend fun getById(id: String): AdjustmentEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: AdjustmentEntity)

    @Query(
        """SELECT b.* FROM balance_adjustments AS b
            INNER JOIN accounts AS a ON a.id = b.accountId
            WHERE (:currency IS NULL OR a.currency = :currency)
                AND (:accountId IS NULL OR b.accountId = :accountId)
                AND (:fromInclusive IS NULL OR b.occurredAt >= :fromInclusive)
                AND (:toExclusive IS NULL OR b.occurredAt < :toExclusive)
            ORDER BY b.occurredAt DESC, b.id DESC""",
    )
    fun observeFiltered(
        currency: String?,
        accountId: String?,
        fromInclusive: Instant?,
        toExclusive: Instant?,
    ): Flow<List<AdjustmentEntity>>

    @Query(
        """SELECT COALESCE(SUM(b.amountMinor), 0) FROM balance_adjustments AS b
            INNER JOIN accounts AS a ON a.id = b.accountId
            WHERE (:currency IS NULL OR a.currency = :currency)
                AND (:accountId IS NULL OR b.accountId = :accountId)
                AND (:fromInclusive IS NULL OR b.occurredAt >= :fromInclusive)
                AND (:toExclusive IS NULL OR b.occurredAt < :toExclusive)""",
    )
    fun observeTotalMinor(
        currency: String?,
        accountId: String?,
        fromInclusive: Instant?,
        toExclusive: Instant?,
    ): Flow<Long>

    @Query(
        """SELECT * FROM balance_adjustments
            WHERE syncStatus = 'PENDING_CREATE' ORDER BY occurredAt, id""",
    )
    suspend fun getPendingChanges(): List<AdjustmentEntity>

    @Query(
        """UPDATE balance_adjustments SET createdAt = :createdAt, syncStatus = 'SYNCED'
            WHERE id = :id AND syncStatus = 'PENDING_CREATE'""",
    )
    suspend fun acknowledgeCreated(id: String, createdAt: Instant): Int
}

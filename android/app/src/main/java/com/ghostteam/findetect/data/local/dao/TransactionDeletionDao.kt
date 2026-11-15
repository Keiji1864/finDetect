package com.ghostteam.findetect.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ghostteam.findetect.data.local.entity.TransactionDeletionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

@Dao
interface TransactionDeletionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(deletion: TransactionDeletionEntity)

    @Query("SELECT * FROM transaction_deletions WHERE transactionId = :id")
    suspend fun getById(id: String): TransactionDeletionEntity?

    @Query("SELECT * FROM transaction_deletions ORDER BY undoUntil, transactionId")
    fun observeAll(): Flow<List<TransactionDeletionEntity>>

    @Query("DELETE FROM transaction_deletions WHERE transactionId = :id")
    suspend fun remove(id: String)

    @Query("DELETE FROM transaction_deletions WHERE undoUntil <= :now")
    suspend fun releaseExpired(now: Instant)
}

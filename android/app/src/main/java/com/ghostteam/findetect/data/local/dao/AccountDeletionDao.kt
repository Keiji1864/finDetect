package com.ghostteam.findetect.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ghostteam.findetect.data.local.entity.AccountDeletionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

@Dao
interface AccountDeletionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(deletion: AccountDeletionEntity)

    @Query("SELECT * FROM account_deletions WHERE accountId = :id")
    suspend fun getById(id: String): AccountDeletionEntity?

    @Query("SELECT * FROM account_deletions ORDER BY undoUntil, accountId")
    fun observeAll(): Flow<List<AccountDeletionEntity>>

    @Query("DELETE FROM account_deletions WHERE accountId = :id")
    suspend fun remove(id: String)

    @Query("DELETE FROM account_deletions WHERE undoUntil <= :now")
    suspend fun releaseExpired(now: Instant)
}

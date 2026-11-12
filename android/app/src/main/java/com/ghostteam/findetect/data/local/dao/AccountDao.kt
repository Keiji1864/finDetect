package com.ghostteam.findetect.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ghostteam.findetect.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query(
        """SELECT * FROM accounts
            WHERE deletedAt IS NULL AND (:currency IS NULL OR currency = :currency)
            ORDER BY name, id""",
    )
    fun observeActive(currency: String?): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: AccountEntity)

    @Update
    suspend fun update(entity: AccountEntity): Int

    @Query(
        """SELECT EXISTS(SELECT 1 FROM transactions WHERE accountId = :id)
            OR EXISTS(SELECT 1 FROM balance_adjustments WHERE accountId = :id)""",
    )
    suspend fun hasMovements(id: String): Boolean

    @Query("SELECT * FROM accounts WHERE syncStatus != 'SYNCED' ORDER BY modifiedAt, id")
    suspend fun getPendingChanges(): List<AccountEntity>
}

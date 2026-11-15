package com.ghostteam.findetect.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ghostteam.findetect.data.local.entity.BalanceConfirmationEntity

@Dao
interface BalanceConfirmationDao {
    @Query("SELECT * FROM balance_confirmations WHERE requestId = :requestId")
    suspend fun getById(requestId: String): BalanceConfirmationEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(confirmation: BalanceConfirmationEntity)
}

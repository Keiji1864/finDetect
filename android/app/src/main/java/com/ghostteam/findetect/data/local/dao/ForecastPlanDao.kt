package com.ghostteam.findetect.data.local.dao

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert

@Entity(tableName = "forecast_plans")
data class ForecastPlanRow(
    @PrimaryKey val currency: String,
    val savingsMinor: Long?,
    val paymentsMinor: Long?,
    val paymentDate: String?,
    val manualDailyMinor: Long?,
    val modifiedAt: String,
)

@Dao
interface ForecastPlanDao {
    @Query("SELECT * FROM forecast_plans WHERE currency = :currency")
    suspend fun current(currency: String): ForecastPlanRow?

    @Upsert suspend fun save(plan: ForecastPlanRow)
}

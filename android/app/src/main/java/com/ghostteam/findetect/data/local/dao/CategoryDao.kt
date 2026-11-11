package com.ghostteam.findetect.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ghostteam.findetect.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query(
        """SELECT * FROM categories
            WHERE isIncome = :isIncome AND isArchived = 0
            ORDER BY `key`, id""",
    )
    fun observeAvailable(isIncome: Boolean): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: String): CategoryEntity?

    @Upsert
    suspend fun upsertAll(entities: List<CategoryEntity>)
}

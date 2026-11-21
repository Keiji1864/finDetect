package com.ghostteam.findetect.domain.repository

import com.ghostteam.findetect.domain.model.Category
import com.ghostteam.findetect.domain.model.CategorySetting
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeAvailable(isIncome: Boolean): Flow<List<Category>>

    fun observeSettings(isIncome: Boolean): Flow<List<CategorySetting>>

    suspend fun getById(id: String): Category?

    suspend fun saveCatalogue(categories: List<Category>)

    suspend fun addCustom(name: String, isIncome: Boolean)

    suspend fun setEnabled(id: String, enabled: Boolean)

    suspend fun removeCustom(id: String)
}


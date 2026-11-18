package com.ghostteam.findetect.domain.repository

import com.ghostteam.findetect.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeAvailable(isIncome: Boolean): Flow<List<Category>>

    suspend fun getById(id: String): Category?

    suspend fun saveCatalogue(categories: List<Category>)
}


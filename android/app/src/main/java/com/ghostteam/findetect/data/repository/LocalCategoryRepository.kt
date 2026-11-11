package com.ghostteam.findetect.data.repository

import androidx.room.withTransaction
import com.ghostteam.findetect.data.local.AppDatabase
import com.ghostteam.findetect.data.local.dao.CategoryDao
import com.ghostteam.findetect.data.mapper.toDomain
import com.ghostteam.findetect.data.mapper.toEntity
import com.ghostteam.findetect.domain.model.Category
import com.ghostteam.findetect.domain.repository.CategoryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalCategoryRepository @Inject constructor(
    private val database: AppDatabase,
    private val categoryDao: CategoryDao,
) : CategoryRepository {
    override fun observeAvailable(isIncome: Boolean): Flow<List<Category>> =
        categoryDao.observeAvailable(isIncome).map { rows -> rows.map { it.toDomain() } }

    override suspend fun getById(id: String): Category? = categoryDao.getById(id)?.toDomain()

    override suspend fun saveCatalogue(categories: List<Category>) = database.withTransaction {
        require(categories.map { it.id }.distinct().size == categories.size) { "Duplicate category IDs" }
        val entities = categories.map { category ->
            require(category.id.isNotBlank() && category.key.isNotBlank() && category.iconId.isNotBlank()) {
                "Category ID, key and iconId are required"
            }
            require(category.names.ru.isNotBlank() && category.names.en.isNotBlank()) {
                "Both category translations are required"
            }
            val existing = categoryDao.getById(category.id)
            if (existing != null) {
                require(existing.isIncome == category.isIncome) { "Category type cannot change for an existing ID" }
                require(existing.key == "legacy:${category.id}" || existing.key == category.key) {
                    "Category key cannot change for an existing ID"
                }
            }
            category.toEntity(legacyEmoji = existing?.legacyEmoji)
        }
        categoryDao.upsertAll(entities)
    }
}

package com.ghostteam.findetect.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.room.withTransaction
import com.ghostteam.findetect.data.local.AppDatabase
import com.ghostteam.findetect.data.local.dao.CategoryDao
import com.ghostteam.findetect.data.local.defaultCategories
import com.ghostteam.findetect.data.mapper.toDomain
import com.ghostteam.findetect.data.mapper.toEntity
import com.ghostteam.findetect.data.session.UserAccessGate
import com.ghostteam.findetect.domain.model.Category
import com.ghostteam.findetect.domain.model.CategoryNames
import com.ghostteam.findetect.domain.model.CategorySetting
import com.ghostteam.findetect.domain.model.FinanceIcon
import com.ghostteam.findetect.domain.repository.CategoryRepository
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class LocalCategoryRepository(
    private val database: AppDatabase,
    private val categoryDao: CategoryDao,
    private val preferences: DataStore<Preferences>,
    private val gate: UserAccessGate,
) : CategoryRepository {
    override fun observeAvailable(isIncome: Boolean): Flow<List<Category>> =
        observeSettings(isIncome).map { settings -> settings.filter { it.enabled }.map { it.category } }
            .distinctUntilChanged()

    override fun observeSettings(isIncome: Boolean): Flow<List<CategorySetting>> {
        val otherKey = if (isIncome) "other_income" else "other_expense"
        val otherId = defaultCategories.first { it.key == otherKey }.id
        return combine(categoryDao.observeAvailable(isIncome), preferences.data) { rows, settings ->
            val hidden = settings[hiddenCategoryIds].orEmpty()
            val removed = settings[removedCategoryIds].orEmpty()
            rows.filter { it.id !in removed }
                .sortedBy { it.id == otherId || it.key == otherKey }
                .map { CategorySetting(it.toDomain(), enabled = it.id !in hidden) }
        }.distinctUntilChanged()
    }

    override suspend fun getById(id: String): Category? = categoryDao.getById(id)?.toDomain()

    override suspend fun saveCatalogue(categories: List<Category>) = gate.use {
        database.withTransaction {
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

    override suspend fun addCustom(name: String, isIncome: Boolean) {
        val title = name.trim()
        require(title.length in 1..Category.MAX_CUSTOM_NAME_LENGTH)
        val id = UUID.randomUUID().toString()
        saveCatalogue(
            listOf(
                Category(
                    id = id,
                    key = Category.customKey(id),
                    names = CategoryNames(title, title),
                    iconId = FinanceIcon.WALLET.key,
                    isIncome = isIncome,
                ),
            ),
        )
    }

    override suspend fun setEnabled(id: String, enabled: Boolean) {
        gate.use {
            preferences.edit { settings ->
                val hidden = settings[hiddenCategoryIds].orEmpty()
                settings[hiddenCategoryIds] = if (enabled) hidden - id else hidden + id
            }
        }
    }

    override suspend fun removeCustom(id: String) {
        gate.use {
            require(categoryDao.getById(id)?.key == Category.customKey(id))
            preferences.edit { settings ->
                settings[hiddenCategoryIds] = settings[hiddenCategoryIds].orEmpty() - id
                settings[removedCategoryIds] = settings[removedCategoryIds].orEmpty() + id
            }
        }
    }

    private companion object {
        val hiddenCategoryIds = stringSetPreferencesKey("hidden_categories")
        val removedCategoryIds = stringSetPreferencesKey("removed_custom_categories")
    }
}

package com.ghostteam.findetect.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghostteam.findetect.domain.model.Category
import com.ghostteam.findetect.domain.model.CategorySetting
import com.ghostteam.findetect.domain.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class CategorySettingsUiState(
    val isIncome: Boolean = false,
    val categories: List<CategorySetting> = emptyList(),
    val name: String = "",
    val loading: Boolean = true,
    val readError: Boolean = false,
    val saving: Boolean = false,
    val writeError: Boolean = false,
    val removalOpen: Boolean = false,
) {
    val canEdit: Boolean get() = !loading && !readError && !saving
    val canAdd: Boolean get() = canEdit && name.trim().length in 1..Category.MAX_CUSTOM_NAME_LENGTH
    val customCategories: List<Category> get() = categories.map { it.category }.filter { it.isCustom }
}

@HiltViewModel
internal class CategorySettingsViewModel @Inject constructor(
    private val repository: CategoryRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(CategorySettingsUiState())
    val state = mutableState.asStateFlow()
    private var observation: Job? = null

    init {
        observeCategories()
    }

    fun selectType(isIncome: Boolean) {
        val current = state.value
        if (current.saving || current.isIncome == isIncome) return
        mutableState.update { it.copy(isIncome = isIncome, categories = emptyList(), writeError = false) }
        observeCategories()
    }

    fun retry() = observeCategories()

    fun changeName(name: String) {
        if (!state.value.saving) mutableState.update { it.copy(name = name, writeError = false) }
    }

    fun add() {
        val current = state.value
        if (!current.canAdd) return
        write {
            repository.addCustom(current.name, current.isIncome)
            mutableState.update { it.copy(name = "") }
        }
    }

    fun setEnabled(id: String, enabled: Boolean) {
        if (state.value.categories.none { it.category.id == id }) return
        write { repository.setEnabled(id, enabled) }
    }

    fun openRemoval() {
        val current = state.value
        if (current.canEdit && current.customCategories.isNotEmpty()) {
            mutableState.update { it.copy(removalOpen = true, writeError = false) }
        }
    }

    fun closeRemoval() {
        if (!state.value.saving) mutableState.update { it.copy(removalOpen = false, writeError = false) }
    }

    fun remove(id: String) {
        if (!state.value.removalOpen || state.value.customCategories.none { it.id == id }) return
        write {
            repository.removeCustom(id)
            mutableState.update { it.copy(removalOpen = false) }
        }
    }

    private fun observeCategories() {
        observation?.cancel()
        val isIncome = state.value.isIncome
        mutableState.update { it.copy(loading = true, readError = false) }
        observation = viewModelScope.launch {
            repository.observeSettings(isIncome)
                .catch { error ->
                    if (error is CancellationException) throw error
                    mutableState.update { it.copy(loading = false, readError = true) }
                }
                .collect { categories ->
                    mutableState.update { it.copy(categories = categories, loading = false) }
                }
        }
    }

    private fun write(action: suspend () -> Unit) {
        if (!state.value.canEdit) return
        mutableState.update { it.copy(saving = true, writeError = false) }
        viewModelScope.launch {
            try {
                action()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                mutableState.update { it.copy(writeError = true) }
            } finally {
                mutableState.update { it.copy(saving = false) }
            }
        }
    }
}

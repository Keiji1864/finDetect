package com.ghostteam.findetect.feature.transaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghostteam.findetect.domain.model.AccountBalance
import com.ghostteam.findetect.domain.model.AddTransactionInput
import com.ghostteam.findetect.domain.model.AddTransactionResult
import com.ghostteam.findetect.domain.model.AppCurrency
import com.ghostteam.findetect.domain.model.Category
import com.ghostteam.findetect.domain.model.TransactionCreationError
import com.ghostteam.findetect.domain.repository.CategoryRepository
import com.ghostteam.findetect.domain.repository.SettingsRepository
import com.ghostteam.findetect.domain.usecase.AddTransactionUseCase
import com.ghostteam.findetect.domain.usecase.ObserveBalancesUseCase
import com.ghostteam.findetect.domain.validation.TransactionAmountParser
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

enum class AddTransactionMode { Switchable, Expense, Income }
enum class TransactionPicker { CATEGORY, ACCOUNT, DATE }

data class AddTransactionUiState(
    val sessionId: String? = null,
    val mode: AddTransactionMode = AddTransactionMode.Switchable,
    val isIncome: Boolean = false,
    val amount: String = "",
    val amountValid: Boolean = false,
    val comment: String = "",
    val date: LocalDate = LocalDate(2000, 1, 1),
    val accountId: String? = null,
    val categoryId: String? = null,
    val accounts: List<AccountBalance> = emptyList(),
    val categories: List<Category> = emptyList(),
    val accountsLoading: Boolean = true,
    val categoriesLoading: Boolean = true,
    val accountsError: Boolean = false,
    val categoriesError: Boolean = false,
    val displayCurrency: AppCurrency? = null,
    val settingsError: Boolean = false,
    val picker: TransactionPicker? = null,
    val pendingAccountId: String? = null,
    val createAccountRequest: String? = null,
    val saving: Boolean = false,
    val saveError: TransactionCreationError? = null,
    val saved: Boolean = false,
    val promptCurrency: AppCurrency? = null,
    val switchingCurrency: Boolean = false,
    val switchError: Boolean = false,
) {
    val selectedAccount: AccountBalance? get() = accounts.firstOrNull { it.account.id == accountId }
    val selectedCategory: Category? get() = categories.firstOrNull { it.id == categoryId }
    val canSave: Boolean get() = amountValid && comment.length <= 200 && selectedAccount != null &&
        selectedCategory != null && !accountsLoading && !categoriesLoading && !accountsError && !categoriesError &&
        displayCurrency != null && !settingsError && !saving && !saved && createAccountRequest == null
}

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val categories: CategoryRepository,
    private val settings: SettingsRepository,
    private val observeBalances: ObserveBalancesUseCase,
    private val addTransaction: AddTransactionUseCase,
    private val amountParser: TransactionAmountParser,
    private val clock: Clock,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private fun today() = clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    private val restoredAmount = savedState.get<String>("amount") ?: ""
    private val mutableState = MutableStateFlow(AddTransactionUiState(
        sessionId = savedState["session"],
        mode = AddTransactionMode.entries.firstOrNull { it.name == savedState.get<String>("mode") } ?: AddTransactionMode.Switchable,
        isIncome = savedState["income"] ?: false,
        amount = restoredAmount,
        amountValid = amountParser.parse(restoredAmount) != null,
        comment = savedState["comment"] ?: "",
        date = savedState.get<String>("date")?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: today(),
        accountId = savedState["account"],
        categoryId = savedState["category"],
        picker = TransactionPicker.entries.firstOrNull { it.name == savedState.get<String>("picker") },
        pendingAccountId = savedState["pendingAccount"],
        createAccountRequest = savedState["createAccount"],
        saved = savedState["saved"] ?: false,
        promptCurrency = AppCurrency.entries.firstOrNull { it.name == savedState.get<String>("promptCurrency") },
    ))
    val state = mutableState.asStateFlow()
    private var observers: List<Job> = emptyList()

    fun startSession(id: String, mode: AddTransactionMode) {
        if (state.value.sessionId != id) {
            mutableState.value = AddTransactionUiState(sessionId = id, mode = mode,
                isIncome = mode == AddTransactionMode.Income, date = today())
            persist()
        }
        retryData()
    }

    fun stopObserving() {
        observers.forEach { it.cancel() }
        observers = emptyList()
    }

    fun retryData() {
        stopObserving()
        mutableState.update { it.copy(accountsLoading = true, categoriesLoading = true,
            accountsError = false, categoriesError = false, settingsError = false, displayCurrency = null) }
        observers = listOf(
            viewModelScope.launch {
                combine(AppCurrency.entries.map { observeBalances(it.name) }) { summaries ->
                    summaries.flatMap { it.accounts }.sortedWith(compareBy({ it.account.currency }, { it.account.name }, { it.account.id }))
                }.catch { error ->
                    if (error is CancellationException) throw error
                    mutableState.update { it.copy(accountsLoading = false, accountsError = true) }
                }.collect { accounts -> mutableState.update { it.copy(accounts = accounts, accountsLoading = false) } }
            },
            viewModelScope.launch {
                state.map { it.isIncome }.distinctUntilChanged().collectLatest { income ->
                    mutableState.update { it.copy(categoriesLoading = true) }
                    categories.observeAvailable(income).catch { error ->
                        if (error is CancellationException) throw error
                        mutableState.update { it.copy(categoriesLoading = false, categoriesError = true) }
                    }.collect { available ->
                        mutableState.update { it.copy(categories = available, categoriesLoading = false, categoriesError = false) }
                    }
                }
            },
            viewModelScope.launch {
                settings.currency.catch { error ->
                    if (error is CancellationException) throw error
                    mutableState.update { it.copy(settingsError = true) }
                }.collect { currency -> mutableState.update { it.copy(displayCurrency = currency, settingsError = false) } }
            },
        )
    }

    private fun edit(change: (AddTransactionUiState) -> AddTransactionUiState) {
        if (state.value.saving || state.value.saved) return
        mutableState.update { change(it).copy(saveError = null) }
        persist()
    }

    fun changeAmount(value: String) = edit { it.copy(amount = value, amountValid = amountParser.parse(value) != null) }
    fun changeComment(value: String) = edit { it.copy(comment = value) }

    fun changeType(income: Boolean) {
        if (state.value.mode != AddTransactionMode.Switchable || state.value.isIncome == income) return
        edit { it.copy(isIncome = income, categoryId = null, categories = emptyList(), categoriesLoading = true, picker = null) }
    }

    fun openPicker(picker: TransactionPicker) = edit { it.copy(picker = picker, pendingAccountId = it.accountId) }
    fun closePicker() = edit { it.copy(picker = null, pendingAccountId = null) }
    fun chooseCategory(id: String) {
        if (state.value.categories.none { it.id == id }) return
        edit { it.copy(categoryId = id, picker = null) }
    }
    fun chooseAccount(id: String) {
        if (state.value.accounts.none { it.account.id == id }) return
        edit { it.copy(pendingAccountId = id) }
    }
    fun confirmAccount() {
        val id = state.value.pendingAccountId ?: return
        if (state.value.accounts.none { it.account.id == id }) return
        edit { it.copy(accountId = id, pendingAccountId = null, picker = null) }
    }
    fun chooseDate(date: LocalDate) = edit { it.copy(date = date, picker = null) }
    fun createAccount() = edit { it.copy(createAccountRequest = UUID.randomUUID().toString(), picker = null) }
    fun cancelAccountCreation() = edit { it.copy(createAccountRequest = null) }
    fun accountCreated(id: String) = edit { it.copy(accountId = id, createAccountRequest = null) }

    fun save() {
        val current = state.value
        if (!current.canSave) return
        val currency = AppCurrency.valueOf(requireNotNull(current.selectedAccount).account.currency)
        val input = AddTransactionInput(current.accountId, current.categoryId, current.isIncome,
            current.amount, current.date, current.comment)
        mutableState.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            when (val result = addTransaction(input)) {
                is AddTransactionResult.Success -> {
                    mutableState.update { it.copy(saving = false, saved = true, picker = null,
                        promptCurrency = currency.takeIf { chosen -> chosen != current.displayCurrency }) }
                    persist()
                }
                is AddTransactionResult.Failure -> {
                    mutableState.update { it.copy(saving = false, saveError = result.error) }
                    // These records may have changed since the picker was opened.
                    if (result.error in staleReferenceErrors) retryData()
                }
            }
        }
    }

    fun dismissCurrencyPrompt() {
        if (state.value.switchingCurrency) return
        mutableState.update { it.copy(promptCurrency = null) }
        persist()
    }

    fun switchCurrency() {
        val currency = state.value.promptCurrency ?: return
        if (state.value.switchingCurrency) return
        mutableState.update { it.copy(switchingCurrency = true, switchError = false) }
        viewModelScope.launch {
            try {
                settings.setCurrency(currency)
                mutableState.update { it.copy(promptCurrency = null) }
                persist()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                mutableState.update { it.copy(switchError = true) }
            } finally {
                mutableState.update { it.copy(switchingCurrency = false) }
            }
        }
    }

    private fun persist() {
        val current = state.value
        savedState["session"] = current.sessionId
        savedState["mode"] = current.mode.name
        savedState["income"] = current.isIncome
        savedState["amount"] = current.amount
        savedState["comment"] = current.comment
        savedState["date"] = current.date.toString()
        savedState["account"] = current.accountId
        savedState["category"] = current.categoryId
        savedState["picker"] = current.picker?.name
        savedState["pendingAccount"] = current.pendingAccountId
        savedState["createAccount"] = current.createAccountRequest
        savedState["saved"] = current.saved
        savedState["promptCurrency"] = current.promptCurrency?.name
    }

    private companion object {
        val staleReferenceErrors = setOf(TransactionCreationError.ACCOUNT_NOT_FOUND, TransactionCreationError.ACCOUNT_DELETED,
            TransactionCreationError.CATEGORY_NOT_FOUND, TransactionCreationError.CATEGORY_ARCHIVED, TransactionCreationError.CATEGORY_TYPE_MISMATCH)
    }
}

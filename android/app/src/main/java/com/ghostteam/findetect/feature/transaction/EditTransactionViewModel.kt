package com.ghostteam.findetect.feature.transaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghostteam.findetect.domain.model.AppCurrency
import com.ghostteam.findetect.domain.model.PendingTransactionDeletion
import com.ghostteam.findetect.domain.model.TransactionDetails
import com.ghostteam.findetect.domain.model.TransactionManagementError
import com.ghostteam.findetect.domain.model.TransactionManagementException
import com.ghostteam.findetect.domain.model.UpdateTransactionResult
import com.ghostteam.findetect.domain.repository.BalanceRepository
import com.ghostteam.findetect.domain.repository.CategoryRepository
import com.ghostteam.findetect.domain.repository.SettingsRepository
import com.ghostteam.findetect.domain.repository.TransactionRepository
import com.ghostteam.findetect.domain.usecase.ObserveBalancesUseCase
import com.ghostteam.findetect.domain.usecase.UpdateTransactionUseCase
import com.ghostteam.findetect.domain.validation.TransactionAmountParser
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class EditTransactionUiState(
    val transactionId: String? = null,
    val original: TransactionDetails? = null,
    val form: AddTransactionUiState = AddTransactionUiState(),
    val loading: Boolean = true,
    val loadError: Boolean = false,
    val unavailable: Boolean = false,
    val changed: Boolean = false,
    val expectedCurrency: String? = null,
    val error: TransactionManagementError? = null,
    val confirmDelete: Boolean = false,
) {
    val currencyChanged: Boolean get() = form.selectedAccount?.account?.currency?.let { it != expectedCurrency } == true
    val canSave: Boolean get() = original != null && !loading && !loadError && !unavailable && !changed &&
        !currencyChanged && form.canSave
    val canDelete: Boolean get() = original != null && !loading && !loadError && !unavailable && !changed && !form.saving && !form.saved
}

@HiltViewModel
class EditTransactionViewModel @Inject constructor(
    private val transactions: TransactionRepository,
    private val categories: CategoryRepository,
    private val balances: BalanceRepository,
    private val observeBalances: ObserveBalancesUseCase,
    private val settings: SettingsRepository,
    private val updateTransaction: UpdateTransactionUseCase,
    private val amountParser: TransactionAmountParser,
    private val clock: Clock,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val restoredAmount = savedState.get<String>("amount").orEmpty()
    private val mutableState = MutableStateFlow(EditTransactionUiState(
        transactionId = savedState["transaction"],
        expectedCurrency = savedState["expectedCurrency"],
        form = AddTransactionUiState(
            amount = restoredAmount, amountValid = amountParser.parse(restoredAmount) != null,
            comment = savedState.get<String>("comment").orEmpty(),
            date = savedState.get<String>("date")?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate(2000, 1, 1),
            accountId = savedState["account"], categoryId = savedState["category"],
            picker = TransactionPicker.entries.firstOrNull { it.name == savedState.get<String>("picker") },
            pendingAccountId = savedState["pendingAccount"], createAccountRequest = savedState["createAccount"],
            saved = savedState["saved"] ?: false,
            promptCurrency = AppCurrency.entries.firstOrNull { it.name == savedState.get<String>("promptCurrency") },
        ),
    ))
    val state = mutableState.asStateFlow()
    private val mutableUndoError = MutableStateFlow(false)
    val undoError = mutableUndoError.asStateFlow()
    val deletions = transactions.observePendingDeletions().catch { error ->
        if (error is CancellationException) throw error
        mutableUndoError.value = true
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private var observers: List<Job> = emptyList()
    private var initialized = savedState.get<Boolean>("initialized") ?: false
    private var expectedVersion = savedState.get<String>("version")
    private var zone = TimeZone.of(savedState.get<String>("zone") ?: TimeZone.currentSystemDefault().id)

    init { if (state.value.transactionId != null && !state.value.form.saved) retryData() }

    fun open(id: String) {
        if (state.value.form.saving || state.value.form.switchingCurrency) return
        stopObserving()
        initialized = false
        expectedVersion = null
        zone = TimeZone.currentSystemDefault()
        mutableState.value = EditTransactionUiState(transactionId = id)
        persist()
        retryData()
    }

    fun dismiss() {
        if (state.value.form.saving || state.value.form.switchingCurrency) return
        stopObserving()
        initialized = false
        expectedVersion = null
        mutableState.value = EditTransactionUiState()
        persist()
    }

    private fun stopObserving() {
        observers.forEach { it.cancel() }
        observers = emptyList()
    }

    fun reload() {
        if (state.value.form.saving) return
        val id = state.value.transactionId ?: return
        open(id)
    }

    fun retryData() {
        val id = state.value.transactionId ?: return
        if (state.value.form.saving || state.value.form.saved) return
        stopObserving()
        mutableState.update { it.copy(loading = true, loadError = false, form = it.form.copy(
            accountsLoading = true, categoriesLoading = true, accountsError = false, categoriesError = false, settingsError = false)) }
        observers = listOf(
            viewModelScope.launch {
                transactions.observeDetailsById(id).catch { error ->
                    if (error is CancellationException) throw error
                    mutableState.update { it.copy(loading = false, loadError = true) }
                }.collect { details ->
                    if (details == null || details.transaction.isDeleted) {
                        mutableState.update { it.copy(loading = false, unavailable = true, confirmDelete = false) }
                    } else if (!initialized) {
                        val transaction = details.transaction
                        val amount = transaction.amount.toPlainString()
                        expectedVersion = transaction.modifiedAt.toString()
                        initialized = true
                        mutableState.update { it.copy(original = details, loading = false, unavailable = false,
                            expectedCurrency = details.currency, form = it.form.copy(
                                mode = if (details.category.isIncome) AddTransactionMode.Income else AddTransactionMode.Expense,
                                isIncome = details.category.isIncome, amount = amount, amountValid = amountParser.parse(amount) != null,
                                accountId = transaction.accountId, categoryId = transaction.categoryId, originalCategory = details.category,
                                date = transaction.transactionDate.toLocalDateTime(zone).date, comment = transaction.comment.orEmpty(),
                            )) }
                        persist()
                    } else {
                        val changed = details.transaction.modifiedAt.toString() != expectedVersion
                        mutableState.update {
                            val original = if (changed) it.original ?: details else details
                            it.copy(
                                original = original, loading = false, unavailable = false, changed = changed,
                                form = it.form.copy(
                                    mode = if (original.category.isIncome) AddTransactionMode.Income else AddTransactionMode.Expense,
                                    isIncome = original.category.isIncome, originalCategory = original.category,
                                ),
                            )
                        }
                    }
                }
            },
            viewModelScope.launch {
                state.map { it.original?.transaction?.accountId }.distinctUntilChanged().collectLatest { originalId ->
                    val active = combine(AppCurrency.entries.map { observeBalances(it.name) }) { summaries -> summaries.flatMap { it.accounts } }
                    combine(active, originalId?.let { balances.observeAccount(it) } ?: flowOf(null)) { accounts, original ->
                        (accounts + listOfNotNull(original)).distinctBy { it.account.id }
                            .sortedWith(compareBy({ it.account.currency }, { it.account.name }, { it.account.id }))
                    }.catch { error ->
                        if (error is CancellationException) throw error
                        mutableState.update { it.copy(form = it.form.copy(accountsLoading = false, accountsError = true)) }
                    }.collect { accounts -> mutableState.update { it.copy(form = it.form.copy(accounts = accounts, accountsLoading = false)) } }
                }
            },
            viewModelScope.launch {
                state.map { it.original?.category }.distinctUntilChanged().collectLatest { original ->
                    if (original == null) return@collectLatest
                    categories.observeAvailable(original.isIncome).catch { error ->
                        if (error is CancellationException) throw error
                        mutableState.update { it.copy(form = it.form.copy(categoriesLoading = false, categoriesError = true)) }
                    }.collect { available -> mutableState.update { it.copy(form = it.form.copy(
                        categories = available, categoriesLoading = false, categoriesError = false)) } }
                }
            },
            viewModelScope.launch {
                settings.currency.catch { error ->
                    if (error is CancellationException) throw error
                    mutableState.update { it.copy(form = it.form.copy(settingsError = true)) }
                }.collect { currency -> mutableState.update { it.copy(form = it.form.copy(displayCurrency = currency, settingsError = false)) } }
            },
        )
    }

    private fun edit(change: (AddTransactionUiState) -> AddTransactionUiState) {
        if (state.value.form.saving || state.value.form.saved) return
        mutableState.update { it.copy(form = change(it.form), error = null) }
        persist()
    }

    fun changeAmount(value: String) = edit { it.copy(amount = value, amountValid = amountParser.parse(value) != null) }
    fun changeComment(value: String) = edit { it.copy(comment = value) }
    fun openPicker(picker: TransactionPicker) = edit { it.copy(picker = picker, pendingAccountId = it.accountId) }
    fun closePicker() = edit { it.copy(picker = null, pendingAccountId = null) }
    fun chooseCategory(id: String) {
        if (state.value.form.categories.none { it.id == id }) return
        edit { it.copy(categoryId = id, picker = null) }
    }
    fun chooseAccount(id: String) {
        if (state.value.form.accounts.none { it.account.id == id }) return
        edit { it.copy(pendingAccountId = id) }
    }
    fun confirmAccount() {
        val account = state.value.form.accounts.firstOrNull { it.account.id == state.value.form.pendingAccountId }?.account ?: return
        edit { it.copy(accountId = account.id, pendingAccountId = null, picker = null) }
        if (!state.value.form.saving && !state.value.form.saved) {
            mutableState.update { it.copy(expectedCurrency = account.currency) }
            persist()
        }
    }
    fun acceptCurrency() {
        if (state.value.form.saving || state.value.form.saved) return
        mutableState.update { it.copy(expectedCurrency = it.form.selectedAccount?.account?.currency, error = null) }
        persist()
    }
    fun chooseDate(date: LocalDate) = edit { it.copy(date = date, picker = null) }
    fun createAccount() = edit { it.copy(createAccountRequest = UUID.randomUUID().toString(), picker = null) }
    fun cancelAccountCreation() = edit { it.copy(createAccountRequest = null) }
    fun accountCreated(id: String) {
        val request = state.value.form.createAccountRequest ?: return
        viewModelScope.launch {
            try {
                val account = balances.observeAccount(id).first()?.account
                    ?: throw TransactionManagementException(TransactionManagementError.ACCOUNT_UNAVAILABLE)
                if (state.value.form.createAccountRequest != request || state.value.form.saving) return@launch
                edit { it.copy(accountId = id, createAccountRequest = null) }
                mutableState.update { it.copy(expectedCurrency = account.currency) }
                persist()
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) {
                if (state.value.form.createAccountRequest == request) {
                    edit { it.copy(createAccountRequest = null) }
                    mutableState.update { it.copy(error = TransactionManagementError.ACCOUNT_UNAVAILABLE) }
                }
            }
        }
    }

    fun save() {
        val current = state.value
        if (!current.canSave) return
        val original = current.original?.transaction ?: return
        val form = current.form
        val currency = AppCurrency.valueOf(requireNotNull(current.expectedCurrency))
        mutableState.update { it.copy(form = it.form.copy(saving = true), error = null) }
        viewModelScope.launch {
            when (val result = updateTransaction(original, form.amount, requireNotNull(form.accountId), currency.name,
                requireNotNull(form.categoryId), form.date, zone, form.comment)) {
                is UpdateTransactionResult.Success -> {
                    stopObserving()
                    mutableState.update { it.copy(form = it.form.copy(saving = false, saved = true, picker = null,
                        promptCurrency = currency.takeIf { chosen -> chosen != form.displayCurrency })) }
                    persist()
                    if (state.value.form.promptCurrency == null) dismiss()
                }
                is UpdateTransactionResult.Failure -> failure(result.error)
            }
        }
    }

    fun requestDelete() { if (state.value.canDelete) mutableState.update { it.copy(confirmDelete = true) } }
    fun cancelDelete() { if (!state.value.form.saving) mutableState.update { it.copy(confirmDelete = false) } }
    fun delete() {
        val current = state.value
        if (!current.confirmDelete || !current.canDelete) return
        val original = current.original?.transaction ?: return
        mutableState.update { it.copy(form = it.form.copy(saving = true), error = null) }
        viewModelScope.launch {
            try {
                transactions.deleteWithUndo(original.id, original.modifiedAt)
                mutableState.update { it.copy(form = it.form.copy(saving = false)) }
                dismiss()
            } catch (error: CancellationException) { throw error }
            catch (error: TransactionManagementException) { failure(error.error) }
            catch (_: Exception) { failure(TransactionManagementError.STORAGE_FAILURE) }
        }
    }

    private fun failure(error: TransactionManagementError) {
        mutableState.update { it.copy(error = error, changed = it.changed || error == TransactionManagementError.CHANGED,
            unavailable = it.unavailable || error == TransactionManagementError.NOT_FOUND,
            confirmDelete = false, form = it.form.copy(saving = false)) }
        if (error == TransactionManagementError.ACCOUNT_UNAVAILABLE || error == TransactionManagementError.CATEGORY_UNAVAILABLE ||
            error == TransactionManagementError.ACCOUNT_CURRENCY_CHANGED) retryData()
    }

    fun dismissCurrencyPrompt() { if (!state.value.form.switchingCurrency) dismiss() }
    fun switchCurrency() {
        val currency = state.value.form.promptCurrency ?: return
        if (state.value.form.switchingCurrency) return
        mutableState.update { it.copy(form = it.form.copy(switchingCurrency = true, switchError = false)) }
        viewModelScope.launch {
            try {
                settings.setCurrency(currency)
                mutableState.update { it.copy(form = it.form.copy(switchingCurrency = false)) }
                dismiss()
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) { mutableState.update { it.copy(form = it.form.copy(switchingCurrency = false, switchError = true)) } }
        }
    }

    fun remainingUndoMillis(deletion: PendingTransactionDeletion): Long = (deletion.undoUntil - clock.now()).inWholeMilliseconds.coerceAtLeast(0)
    fun undo(deletion: PendingTransactionDeletion) {
        viewModelScope.launch {
            try { if (!transactions.undoDelete(deletion)) mutableUndoError.value = true }
            catch (error: CancellationException) { throw error }
            catch (_: Exception) { mutableUndoError.value = true }
        }
    }
    fun clearUndoError() { mutableUndoError.value = false }

    private fun persist() {
        val current = state.value
        val form = current.form
        savedState["transaction"] = current.transactionId
        savedState["initialized"] = initialized
        savedState["version"] = expectedVersion
        savedState["zone"] = zone.id
        savedState["expectedCurrency"] = current.expectedCurrency
        savedState["amount"] = form.amount
        savedState["comment"] = form.comment
        savedState["date"] = form.date.toString()
        savedState["account"] = form.accountId
        savedState["category"] = form.categoryId
        savedState["picker"] = form.picker?.name
        savedState["pendingAccount"] = form.pendingAccountId
        savedState["createAccount"] = form.createAccountRequest
        savedState["saved"] = form.saved
        savedState["promptCurrency"] = form.promptCurrency?.name
    }
}

package com.ghostteam.findetect.feature.accounts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghostteam.findetect.domain.model.*
import com.ghostteam.findetect.domain.repository.AccountRepository
import com.ghostteam.findetect.domain.repository.SettingsRepository
import com.ghostteam.findetect.domain.usecase.AdjustAccountBalanceUseCase
import com.ghostteam.findetect.domain.usecase.ObserveAccountBalanceUseCase
import com.ghostteam.findetect.domain.usecase.UpdateAccountUseCase
import com.ghostteam.findetect.domain.validation.InitialBalanceParser
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock


data class AccountCurrencySwitch(val currency: AppCurrency, val busy: Boolean = false)

@HiltViewModel
class AccountEditorViewModel @Inject constructor(
    private val accounts: AccountRepository,
    private val observeBalance: ObserveAccountBalanceUseCase,
    private val updateAccount: UpdateAccountUseCase,
    private val adjustBalance: AdjustAccountBalanceUseCase,
    private val settings: SettingsRepository,
    private val parser: InitialBalanceParser,
    private val clock: Clock,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val mutableState = MutableStateFlow(AccountEditorState())
    val state = mutableState.asStateFlow()
    private var source: Account? = null
    private var observeJob: Job? = null
    private var accountId: String? = savedState["editor_id"]
    private val mutableDeletions = MutableStateFlow<List<PendingAccountDeletion>>(emptyList())
    val deletions = mutableDeletions.asStateFlow()
    private val mutableUndoError = MutableStateFlow(false)
    val undoError = mutableUndoError.asStateFlow()
    private val mutableCurrencySwitch = MutableStateFlow(savedState.get<String>("editor_pendingCurrency")
        ?.let { AccountCurrencySwitch(AppCurrency.valueOf(it)) })
    val currencySwitch = mutableCurrencySwitch.asStateFlow()

    private var timer: Job? = null
    private var closing = false
    private var balanceRequest: String? = savedState["editor_balanceRequest"]

    init {
        viewModelScope.launch {
            accounts.observePendingDeletions().catch { error ->
                if (error is CancellationException) throw error
                mutableUndoError.value = true
            }.collect { mutableDeletions.value = it }
        }
        accountId?.let { id ->
            mutableState.value = AccountEditorState(open = true, loading = true)
            observe(id)
        }
        if (mutableCurrencySwitch.value != null) retryCurrencySwitch()
    }

    fun open(id: String) {
        if (state.value.open || mutableCurrencySwitch.value != null) return
        accountId = id
        savedState["editor_id"] = id
        mutableState.value = AccountEditorState(open = true, loading = true)
        observe(id)
    }

    private fun Account.draft() = AccountEditDraft(name, currency, iconId, includeInForecast)

    private fun observe(id: String) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            combine(observeBalance(id), accounts.observeHasMovements(id)) { balance, locked -> balance to locked }
                .catch { error ->
                    if (error is CancellationException) throw error
                    change { it.copy(loading = false, readError = true, error = AccountEditorError.LOAD) }
                }.collect { (balance, locked) ->
                    if (balance == null || balance.account.isDeleted) {
                        change { it.copy(balance = null, loading = false, readError = true, error = AccountEditorError.UNAVAILABLE) }
                        return@collect
                    }
                    val account = balance.account
                    val previous = source
                    if (previous != null && account.modifiedAt < previous.modifiedAt) return@collect
                    val current = state.value
                    val restored = previous == null && savedState.get<Boolean>("editor_draftReady") == true
                    val draft = when {
                        restored -> AccountEditDraft(savedState["editor_name"] ?: account.name,
                            savedState["editor_currency"] ?: account.currency, savedState["editor_icon"] ?: account.iconId,
                            savedState["editor_forecast"] ?: account.includeInForecast)
                        previous == null || current.draft == previous.draft() -> account.draft()
                        else -> current.draft
                    }
                    val conflict = !current.busy && draft != account.draft() && when {
                        restored -> savedState.get<String>("editor_base") != account.modifiedAt.toString()
                        previous != null -> previous.modifiedAt != account.modifiedAt
                        else -> false
                    }
                    source = account
                    change { it.copy(balance = balance, loading = false, readError = false, currencyLocked = locked,
                        draft = draft.copy(currency = account.currency), actualBalanceText = if (balanceRequest == null) balance.balance.toPlainString()
                            else if (restored) savedState["editor_actual"] ?: balance.balance.toPlainString() else it.actualBalanceText,
                        error = if (conflict) AccountEditorError.CONFLICT else it.error) }
                    persist()
                    if (restored && !conflict) schedule()
                }
        }
    }

    fun changeDraft(draft: AccountEditDraft) {
        change { it.copy(draft = draft.copy(currency = it.draft.currency)) }
        persist()
        schedule()
    }

    fun changeActual(text: String) {
        if (text == state.value.actualBalanceText) return
        balanceRequest = UUID.randomUUID().toString()
        change { it.copy(actualBalanceText = text) }
        persist()
        schedule()
    }

    private fun schedule() {
        timer?.cancel()
        timer = viewModelScope.launch {
            delay(650)
            flush()
        }
    }

    private fun flush() {
        val current = state.value
        if (current.busy || current.confirmDelete) return
        val base = source
        if (base == null || current.readError || current.error == AccountEditorError.CONFLICT) {
            if (closing) clearSession()
            return
        }
        val draft = current.draft
        val name = draft.name.trim().takeIf { it.length in 1..100 } ?: base.name
        val updated = base.copy(name = name, iconId = draft.iconId,
            includeInForecast = draft.includeInForecast)
        val request = balanceRequest?.takeIf { parser.parse(current.actualBalanceText) != null }
        if (base == updated && request == null) {
            if (closing) clearSession()
            return
        }
        change { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            if (base != updated) {
                when (val result = updateAccount(updated)) {
                    is UpdateAccountResult.Failure -> { failed(result.error.editorError()); return@launch }
                    is UpdateAccountResult.Success -> {
                        source = result.account

                    }
                }
            }
            if (request != null && request == balanceRequest) {
                when (val result = adjustBalance(base.id, current.actualBalanceText, updated.currency, request)) {
                    is AdjustAccountBalanceResult.Failure -> { failed(result.error.editorError(balance = true)); return@launch }
                    is AdjustAccountBalanceResult.Success -> if (request == balanceRequest) balanceRequest = null
                }
            }
            change { it.copy(busy = false) }
            persist()
            if (closing) flush() else if (current.draft != state.value.draft || current.actualBalanceText != state.value.actualBalanceText) schedule()
        }
    }

    private fun failed(error: AccountEditorError) {
        closing = false
        change { it.copy(busy = false, error = error) }
        persist()
    }

    fun requestDelete() {
        if (!state.value.busy && !state.value.loading && !state.value.readError && state.value.balance != null)
            change { it.copy(confirmDelete = true) }
    }
    fun cancelDelete() {
        if (state.value.busy) return
        change { it.copy(confirmDelete = false) }
        schedule()
    }
    fun delete() {
        val base = source ?: return
        if (state.value.busy || !state.value.confirmDelete) return
        change { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                accounts.deleteWithUndo(base.id, base.modifiedAt)
                clearSession()
            } catch (error: CancellationException) { throw error
            } catch (error: AccountManagementException) {
                change { it.copy(busy = false, confirmDelete = false, error = error.error.editorError()) }
            } catch (_: Exception) {
                change { it.copy(busy = false, confirmDelete = false, error = AccountEditorError.DELETE) }
            }
        }
    }

    fun remainingUndoMillis(deletion: PendingAccountDeletion): Long = (deletion.undoUntil - clock.now()).inWholeMilliseconds.coerceAtLeast(0)
    fun undo(deletion: PendingAccountDeletion) {
        viewModelScope.launch {
            try { if (!accounts.undoDelete(deletion)) mutableUndoError.value = true
            } catch (error: CancellationException) { throw error
            } catch (_: Exception) { mutableUndoError.value = true }
        }
    }
    fun clearUndoError() { mutableUndoError.value = false }

    fun retry() {
        if (state.value.busy) return
        val id = accountId ?: return
        if (!state.value.readError && state.value.error in listOf(AccountEditorError.SAVE, AccountEditorError.BALANCE)) {
            flush()
            return
        }
        source = null
        balanceRequest = null
        savedState["editor_draftReady"] = false
        change { it.copy(loading = true, readError = false, error = null) }
        observe(id)
    }

    fun retryCurrencySwitch() {
        val current = mutableCurrencySwitch.value ?: return
        if (current.busy) return
        mutableCurrencySwitch.value = current.copy(busy = true)
        viewModelScope.launch {
            try {
                settings.setCurrency(current.currency)
                dismissCurrencyNotice()
            } catch (error: CancellationException) { throw error
            } catch (_: Exception) { mutableCurrencySwitch.value = current.copy(busy = false) }
        }
    }
    fun dismissCurrencyNotice() { mutableCurrencySwitch.value = null; savedState["editor_pendingCurrency"] = null }

    fun dismiss() {
        closing = true
        timer?.cancel()
        flush()
    }

    private fun change(transform: (AccountEditorState) -> AccountEditorState) {
        mutableState.update { previous ->
            val next = transform(previous)
            next.copy(actualBalanceValid = balanceRequest == null || parser.parse(next.actualBalanceText) != null)
        }
    }

    private fun clearSession() {
        closing = false
        timer?.cancel()
        balanceRequest = null
        observeJob?.cancel()
        observeJob = null
        source = null
        accountId = null
        savedState.keys().filter { it.startsWith("editor_") && it != "editor_pendingCurrency" }.forEach { savedState.remove<Any>(it) }
        mutableState.value = AccountEditorState()
    }

    private fun persist() {
        if (accountId == null) return
        val current = state.value
        savedState["editor_draftReady"] = source != null
        savedState["editor_base"] = source?.modifiedAt?.toString()
        savedState["editor_balanceRequest"] = balanceRequest
        savedState["editor_name"] = current.draft.name
        savedState["editor_currency"] = current.draft.currency
        savedState["editor_icon"] = current.draft.iconId
        savedState["editor_forecast"] = current.draft.includeInForecast
        savedState["editor_actual"] = current.actualBalanceText
    }

    private fun AccountManagementError.editorError(balance: Boolean = false): AccountEditorError = when (this) {
        AccountManagementError.CHANGED -> AccountEditorError.CONFLICT
        AccountManagementError.CURRENCY_LOCKED -> AccountEditorError.CURRENCY_LOCKED
        AccountManagementError.NOT_FOUND -> AccountEditorError.UNAVAILABLE
        else -> if (balance) AccountEditorError.BALANCE else AccountEditorError.SAVE
    }
}

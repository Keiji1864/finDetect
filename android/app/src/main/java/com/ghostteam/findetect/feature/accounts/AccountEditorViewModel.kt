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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

data class AccountCurrencySwitch(val currency: AppCurrency, val busy: Boolean = false)

/** Owns editing separately from account creation, including the draft beneath the balance page. */
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

    init {
        viewModelScope.launch {
            accounts.observePendingDeletions().catch { error ->
                if (error is CancellationException) throw error
                mutableUndoError.value = true
            }.collect { mutableDeletions.value = it }
        }
        accountId?.let { id ->
            val restored = AccountEditorState(
                page = savedState.get<String>("editor_page")?.let { AccountEditorPage.valueOf(it) } ?: AccountEditorPage.EDIT,
                draft = AccountEditDraft(savedState["editor_name"] ?: "", savedState["editor_currency"] ?: "RUB",
                    savedState["editor_icon"] ?: "wallet", savedState["editor_forecast"] ?: true),
                actualBalanceText = savedState["editor_actual"] ?: "",
                pendingIcon = savedState["editor_pendingIcon"] ?: "wallet",
                returnToEdit = savedState["editor_return"] ?: false,
                loading = true,
            )
            mutableState.value = restored
            observe(id)
        }
        if (mutableCurrencySwitch.value != null) retryCurrencySwitch()
    }

    fun open(id: String, balanceOnly: Boolean = false) {
        if (state.value.busy || mutableCurrencySwitch.value != null) return
        clearSession()
        accountId = id
        mutableState.value = AccountEditorState(page = if (balanceOnly) AccountEditorPage.BALANCE else AccountEditorPage.EDIT, loading = true)
        savedState["editor_id"] = id
        savedState["editor_request"] = UUID.randomUUID().toString()
        persist()
        observe(id)
    }

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
                    if (source == null) {
                        val marker = savedState.get<String>("editor_base")?.let(Instant::parse)
                        source = balance.account.copy(modifiedAt = marker ?: balance.account.modifiedAt)
                        if (savedState.get<Boolean>("editor_draftReady") != true) {
                            mutableState.update { it.copy(draft = AccountEditDraft(balance.account.name, balance.account.currency,
                                balance.account.iconId, balance.account.includeInForecast), pendingIcon = balance.account.iconId,
                                actualBalanceText = balance.balance.takeIf { amount -> amount.signum() >= 0 }?.toPlainString().orEmpty()) }
                            savedState["editor_draftReady"] = true
                        }
                        savedState["editor_base"] = requireNotNull(source).modifiedAt.toString()
                    }
                    if (state.value.page == AccountEditorPage.BALANCE && savedState.get<String>("editor_balanceCurrency") == null)
                        savedState["editor_balanceCurrency"] = balance.account.currency
                    change { previous ->
                        val currencyReset = locked && previous.draft.currency != balance.account.currency
                        previous.copy(balance = balance, currencyLocked = locked, loading = false, readError = false,
                            draft = if (currencyReset) previous.draft.copy(currency = balance.account.currency) else previous.draft,
                            error = when {
                                !previous.busy && source?.modifiedAt != balance.account.modifiedAt -> AccountEditorError.CONFLICT
                                currencyReset -> AccountEditorError.CURRENCY_LOCKED
                                previous.error == AccountEditorError.LOAD -> null
                                else -> previous.error
                            })
                    }
                    persist()
                }
        }
    }

    fun changeDraft(draft: AccountEditDraft) {
        if (state.value.busy) return
        val allowed = if (state.value.currencyLocked) draft.copy(currency = source?.currency ?: draft.currency) else draft
        change { it.copy(draft = allowed, error = it.error?.takeIf { error -> error == AccountEditorError.CONFLICT }) }
        persist()
    }

    fun openPage(page: AccountEditorPage) {
        val current = state.value
        if (current.busy || current.loading || current.readError) return
        if (page == AccountEditorPage.CURRENCY && current.currencyLocked) return
        if (page == AccountEditorPage.BALANCE) {
            savedState["editor_balanceCurrency"] = current.balance?.account?.currency
            savedState["editor_request"] = UUID.randomUUID().toString()
        }
        change { it.copy(page = page, pendingIcon = it.draft.iconId,
            returnToEdit = if (page == AccountEditorPage.BALANCE) current.page == AccountEditorPage.EDIT else it.returnToEdit,
            actualBalanceText = if (page == AccountEditorPage.BALANCE)
                it.balance?.balance?.takeIf { amount -> amount.signum() >= 0 }?.toPlainString().orEmpty() else it.actualBalanceText,
            error = null) }
        persist()
    }

    fun selectIcon(icon: String) { if (!state.value.busy) { change { it.copy(pendingIcon = icon) }; persist() } }
    fun confirmIcon() {
        if (state.value.busy) return
        changeDraft(state.value.draft.copy(iconId = state.value.pendingIcon))
        dismiss()
    }

    fun changeActual(text: String) {
        if (state.value.busy || text == state.value.actualBalanceText) return
        savedState["editor_request"] = UUID.randomUUID().toString()
        change { it.copy(actualBalanceText = text, error = null) }
        persist()
    }

    fun save() {
        val current = state.value
        val base = source ?: return
        if (!current.canSave) return
        change { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            when (val result = updateAccount(base.copy(name = current.draft.name, currency = current.draft.currency,
                iconId = current.draft.iconId, includeInForecast = current.draft.includeInForecast))) {
                is UpdateAccountResult.Failure -> change { it.copy(busy = false, error = result.error.editorError()) }
                is UpdateAccountResult.Success -> {
                    clearSession()
                    if (base.currency != result.account.currency) {
                        val currency = AppCurrency.valueOf(result.account.currency)
                        savedState["editor_pendingCurrency"] = currency.name
                        mutableCurrencySwitch.value = AccountCurrencySwitch(currency)
                        retryCurrencySwitch()
                    }
                }
            }
        }
    }

    fun confirmBalance() {
        val current = state.value
        val account = current.balance?.account ?: return
        if (!current.canConfirmBalance) return
        val currency = savedState.get<String>("editor_balanceCurrency") ?: return
        val request = savedState.get<String>("editor_request") ?: UUID.randomUUID().toString().also { savedState["editor_request"] = it }
        change { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            when (val result = adjustBalance(account.id, current.actualBalanceText, currency, request)) {
                is AdjustAccountBalanceResult.Failure -> change { it.copy(busy = false, error = result.error.editorError(balance = true)) }
                is AdjustAccountBalanceResult.Success -> {
                    change { it.copy(busy = false) }
                    dismiss()
                }
            }
        }
    }

    fun requestDelete() {
        if (!state.value.busy && !state.value.loading && !state.value.readError && state.value.balance != null)
            change { it.copy(confirmDelete = true) }
    }
    fun cancelDelete() { if (!state.value.busy) change { it.copy(confirmDelete = false) } }
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
        source = null
        savedState["editor_base"] = null
        if (state.value.page == AccountEditorPage.BALANCE) {
            savedState["editor_balanceCurrency"] = null
            savedState["editor_request"] = UUID.randomUUID().toString()
        }
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
        val current = state.value
        if (current.busy) return
        when (current.page) {
            AccountEditorPage.ICON, AccountEditorPage.CURRENCY -> change { it.copy(page = AccountEditorPage.EDIT, error = null) }
            AccountEditorPage.BALANCE -> if (current.returnToEdit) change { it.copy(page = AccountEditorPage.EDIT, returnToEdit = false, error = null) } else clearSession()
            else -> clearSession()
        }
        persist()
    }

    private fun change(transform: (AccountEditorState) -> AccountEditorState) {
        mutableState.update { previous ->
            val transformed = transform(previous)
            val stale = source != null && transformed.balance != null && source?.modifiedAt != transformed.balance.account.modifiedAt
            val balanceCurrency = savedState.get<String>("editor_balanceCurrency")
            val currencyChanged = transformed.page == AccountEditorPage.BALANCE && balanceCurrency != null &&
                transformed.balance != null && balanceCurrency != transformed.balance.account.currency
            val next = if (!transformed.busy && (currencyChanged || stale && transformed.page == AccountEditorPage.EDIT))
                transformed.copy(error = AccountEditorError.CONFLICT) else transformed
            next.copy(actualBalanceValid = parser.parse(next.actualBalanceText) != null,
                canConfirmBalance = !next.busy && !next.loading && !next.readError && next.balance != null &&
                    next.error != AccountEditorError.CONFLICT &&
                    savedState.get<String>("editor_balanceCurrency") == next.balance.account.currency &&
                    parser.parse(next.actualBalanceText) != null,
                canSave = !next.busy && !next.loading && !next.readError && next.balance != null &&
                    !stale &&
                    next.error != AccountEditorError.CONFLICT && next.draft.name.trim().length in 1..100 &&
                    next.draft.iconId.isNotBlank() && AppCurrency.entries.any { it.name == next.draft.currency })
        }
    }

    private fun clearSession() {
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
        savedState["editor_page"] = current.page?.name
        savedState["editor_name"] = current.draft.name
        savedState["editor_currency"] = current.draft.currency
        savedState["editor_icon"] = current.draft.iconId
        savedState["editor_forecast"] = current.draft.includeInForecast
        savedState["editor_actual"] = current.actualBalanceText
        savedState["editor_pendingIcon"] = current.pendingIcon
        savedState["editor_return"] = current.returnToEdit
    }

    private fun AccountManagementError.editorError(balance: Boolean = false): AccountEditorError = when (this) {
        AccountManagementError.CHANGED -> AccountEditorError.CONFLICT
        AccountManagementError.CURRENCY_LOCKED -> AccountEditorError.CURRENCY_LOCKED
        AccountManagementError.NOT_FOUND -> AccountEditorError.UNAVAILABLE
        else -> if (balance) AccountEditorError.BALANCE else AccountEditorError.SAVE
    }
}

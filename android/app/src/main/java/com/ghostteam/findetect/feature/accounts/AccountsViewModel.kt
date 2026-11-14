package com.ghostteam.findetect.feature.accounts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghostteam.findetect.domain.model.AccountCreationError
import com.ghostteam.findetect.domain.model.AppCurrency
import com.ghostteam.findetect.domain.model.BalanceSummary
import com.ghostteam.findetect.domain.model.CreateAccountInput
import com.ghostteam.findetect.domain.model.CreateAccountResult
import com.ghostteam.findetect.domain.repository.SettingsRepository
import com.ghostteam.findetect.domain.usecase.CreateAccountUseCase
import com.ghostteam.findetect.domain.usecase.ObserveBalancesUseCase
import com.ghostteam.findetect.domain.validation.InitialBalanceParser
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AccountPicker { CURRENCY, ICON }

data class AccountsUiState(
    val currency: AppCurrency = AppCurrency.RUB,
    val currencyReady: Boolean = false,
    val loading: Boolean = true,
    val summary: BalanceSummary? = null,
    val readError: Boolean = false,
    val formOpen: Boolean = false,
    val draft: CreateAccountInput = CreateAccountInput(),
    val validationError: AccountCreationError? = AccountCreationError.INVALID_NAME,
    val saveError: AccountCreationError? = null,
    val isSaving: Boolean = false,
    val picker: AccountPicker? = null,
    val pendingIcon: String = "wallet",
    val pendingCurrency: AppCurrency? = null,
    val switchingCurrency: Boolean = false,
    val createdAccountId: String? = null,
    val creationRequestId: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val observeBalances: ObserveBalancesUseCase,
    private val createAccount: CreateAccountUseCase,
    private val parser: InitialBalanceParser,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val restoredDraft = CreateAccountInput(
        name = savedState["name"] ?: "",
        currency = savedState["currency"] ?: AppCurrency.RUB.name,
        initialBalanceText = savedState["balance"] ?: "0",
        iconId = savedState["icon"] ?: "wallet",
        includeInForecast = savedState["forecast"] ?: true,
    )
    private val mutableState = MutableStateFlow(AccountsUiState(
        draft = restoredDraft,
        validationError = restoredDraft.validationError(parser),
        formOpen = savedState["formOpen"] ?: false,
        picker = AccountPicker.entries.firstOrNull { it.name == savedState.get<String>("picker") },
        pendingIcon = savedState["pendingIcon"] ?: restoredDraft.iconId,
        pendingCurrency = AppCurrency.entries.firstOrNull { it.name == savedState.get<String>("pendingCurrency") },
        createdAccountId = savedState["createdAccountId"],
        creationRequestId = savedState["creationRequest"],
    ))
    val state = mutableState.asStateFlow()
    private val reload = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            reload.flatMapLatest {
                settings.currency.flatMapLatest { currency ->
                    mutableState.update {
                        it.copy(currency = currency, currencyReady = true, loading = true, readError = false,
                            summary = it.summary?.takeIf { summary -> summary.currency == currency.name })
                    }
                    observeBalances(currency.name).catch { error ->
                        if (error is CancellationException) throw error
                        mutableState.update { it.copy(loading = false, readError = true) }
                    }
                }.catch { error ->
                    if (error is CancellationException) throw error
                    mutableState.update { it.copy(loading = false, readError = true, currencyReady = false) }
                }
            }.collect { summary ->
                mutableState.update { it.copy(summary = summary, loading = false, readError = false) }
            }
        }
    }

    fun retryRead() { reload.value += 1 }

    fun openForm() {
        if (!state.value.currencyReady || state.value.isSaving || state.value.switchingCurrency) return
        val draft = CreateAccountInput(currency = state.value.currency.name)
        mutableState.update { it.copy(formOpen = true, draft = draft, validationError = draft.validationError(parser),
            saveError = null, picker = null, pendingIcon = draft.iconId, createdAccountId = null) }
        savedState["createdAccountId"] = null
        persistDraft()
    }

    /** Opens the shared creation form once for a caller, including after Activity recreation. */
    fun beginCreation(requestId: String) {
        if (!state.value.currencyReady || state.value.isSaving || state.value.switchingCurrency ||
            savedState.get<String>("creationRequest") == requestId) return
        openForm()
        savedState["creationRequest"] = requestId
        mutableState.update { it.copy(creationRequestId = requestId) }
    }

    fun closeForm() {
        if (state.value.isSaving) return
        mutableState.update { it.copy(formOpen = false, picker = null) }
        persistDraft()
    }

    fun changeDraft(draft: CreateAccountInput) {
        if (state.value.isSaving) return
        mutableState.update { it.copy(draft = draft, validationError = draft.validationError(parser), saveError = null) }
        persistDraft()
    }

    fun openPicker(picker: AccountPicker) {
        if (state.value.isSaving) return
        mutableState.update { it.copy(picker = picker, pendingIcon = it.draft.iconId) }
        persistDraft()
    }

    fun closePicker() {
        mutableState.update { it.copy(picker = null) }
        persistDraft()
    }

    fun selectIcon(key: String) {
        mutableState.update { it.copy(pendingIcon = key) }
        persistDraft()
    }

    fun confirmIcon() {
        changeDraft(state.value.draft.copy(iconId = state.value.pendingIcon))
        closePicker()
    }

    fun selectCurrency(currency: AppCurrency) {
        changeDraft(state.value.draft.copy(currency = currency.name))
        closePicker()
    }

    fun save() {
        val current = state.value
        if (!current.formOpen || current.isSaving || current.validationError != null) return
        mutableState.update { it.copy(isSaving = true, saveError = null) }
        viewModelScope.launch {
            when (val result = createAccount(current.draft)) {
                is CreateAccountResult.Failure -> mutableState.update { it.copy(isSaving = false, saveError = result.error) }
                is CreateAccountResult.Success -> {
                    val currency = AppCurrency.valueOf(result.account.currency)
                    savedState["pendingCurrency"] = currency.name
                    savedState["createdAccountId"] = result.account.id
                    mutableState.update { it.copy(formOpen = false, picker = null, isSaving = false,
                        pendingCurrency = currency, createdAccountId = result.account.id) }
                    persistDraft()
                    retryCurrencySwitch()
                }
            }
        }
    }

    fun retryCurrencySwitch() {
        val currency = state.value.pendingCurrency ?: return
        if (state.value.switchingCurrency) return
        mutableState.update { it.copy(switchingCurrency = true) }
        viewModelScope.launch {
            try {
                settings.setCurrency(currency)
                savedState["pendingCurrency"] = null
                mutableState.update { it.copy(pendingCurrency = null) }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // The account is already saved. Retrying here changes only the display setting.
            } finally {
                mutableState.update { it.copy(switchingCurrency = false) }
            }
        }
    }

    private fun persistDraft() {
        val current = state.value
        savedState["formOpen"] = current.formOpen
        savedState["name"] = current.draft.name
        savedState["currency"] = current.draft.currency
        savedState["balance"] = current.draft.initialBalanceText
        savedState["icon"] = current.draft.iconId
        savedState["forecast"] = current.draft.includeInForecast
        savedState["picker"] = current.picker?.name
        savedState["pendingIcon"] = current.pendingIcon
    }
}

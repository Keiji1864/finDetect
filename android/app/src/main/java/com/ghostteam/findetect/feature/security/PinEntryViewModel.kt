package com.ghostteam.findetect.feature.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghostteam.findetect.core.security.AppLockManager
import com.ghostteam.findetect.domain.repository.PinRepository
import com.ghostteam.findetect.domain.repository.PinResult
import com.ghostteam.findetect.domain.repository.PinStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PinStage { MENU, CREATE, REPEAT, CURRENT_FOR_CHANGE, CURRENT_FOR_DISABLE, UNLOCK }
enum class PinError { NONE, INCORRECT, MISMATCH, THROTTLED, STORAGE, SAVE }
data class PinUiState(
    val status: PinStatus = PinStatus.LOADING, val stage: PinStage = PinStage.MENU,
    val filled: Int = 0, val working: Boolean = false, val completed: Boolean = false,
    val error: PinError = PinError.NONE, val retrySeconds: Int = 0,
)

/** Partial PINs stay in memory, never in SavedStateHandle, Compose saved state, logs or preferences. */
@HiltViewModel
class PinEntryViewModel @Inject constructor(private val repository: PinRepository, private val lock: AppLockManager) : ViewModel() {
    private val mutableState = MutableStateFlow(PinUiState(status = repository.status.value))
    val state = mutableState.asStateFlow()
    private var input = ""
    private var firstPin = ""
    private var oldPin = ""
    private var operation: Job? = null
    private var session = 0L

    init { viewModelScope.launch { repository.status.collect { status -> mutableState.update { it.copy(status = status) } } } }

    fun begin(stage: PinStage) {
        if (state.value.working) return
        if (stage == PinStage.CREATE && state.value.status != PinStatus.DISABLED) return
        if (stage != PinStage.CREATE && state.value.status != PinStatus.ENABLED) return
        if (stage == PinStage.CREATE) lock.allowAccess() // The current session already has access with PIN disabled.
        cancel()
        mutableState.update { it.copy(stage = stage) }
    }

    fun digit(digit: Char) {
        if (state.value.working || state.value.completed || state.value.stage == PinStage.MENU || input.length == 4 || digit !in '0'..'9') return
        input += digit
        mutableState.update { it.copy(filled = input.length, error = PinError.NONE) }
        if (input.length == 4) {
            val entered = input
            val currentSession = session
            mutableState.update { it.copy(working = true) }
            operation = viewModelScope.launch { submit(entered, currentSession) }
        }
    }

    fun delete() {
        if (state.value.working || state.value.completed) return
        input = input.dropLast(1)
        mutableState.update { it.copy(filled = input.length, error = PinError.NONE) }
    }

    fun cancel() {
        session += 1
        operation?.cancel()
        operation = null
        input = ""
        firstPin = ""
        oldPin = ""
        mutableState.update { PinUiState(status = it.status) }
    }

    fun retryStorage() { viewModelScope.launch { repository.refresh() } }

    private suspend fun submit(entered: String, currentSession: Long) {
        try {
            when (state.value.stage) {
                PinStage.CREATE -> { firstPin = entered; advance(PinStage.REPEAT) }
                PinStage.REPEAT -> {
                    if (firstPin != entered) {
                        firstPin = ""
                        advance(PinStage.CREATE)
                        mutableState.update { it.copy(error = PinError.MISMATCH) }
                    } else {
                        val result = if (oldPin.isEmpty()) repository.create(entered) else repository.change(oldPin, entered)
                        handleResult(result, save = true)
                    }
                }
                PinStage.CURRENT_FOR_CHANGE -> {
                    when (val result = repository.verify(entered)) {
                        PinResult.Success -> { oldPin = entered; advance(PinStage.CREATE) }
                        else -> handleResult(result)
                    }
                }
                PinStage.CURRENT_FOR_DISABLE -> handleResult(repository.disable(entered), save = true)
                PinStage.UNLOCK -> handleResult(repository.verify(entered))
                PinStage.MENU -> Unit
            }
        } catch (error: CancellationException) { throw error }
        catch (_: Exception) { if (session == currentSession) mutableState.update { it.copy(error = PinError.SAVE) } }
        finally {
            if (session == currentSession) {
                input = ""
                mutableState.update { it.copy(filled = 0, working = false) }
            }
        }
    }

    private fun advance(stage: PinStage) {
        input = ""
        mutableState.update { it.copy(stage = stage, filled = 0, error = PinError.NONE) }
    }

    private fun handleResult(result: PinResult, save: Boolean = false) {
        when (result) {
            PinResult.Success -> {
                firstPin = ""
                oldPin = ""
                lock.allowAccess()
                mutableState.update { it.copy(completed = true) }
            }
            PinResult.Incorrect -> mutableState.update { it.copy(error = PinError.INCORRECT) }
            is PinResult.Throttled -> mutableState.update { it.copy(error = PinError.THROTTLED, retrySeconds = result.seconds) }
            PinResult.StorageError -> mutableState.update { it.copy(error = if (save) PinError.SAVE else PinError.STORAGE) }
        }
    }

    override fun onCleared() { input = ""; firstPin = ""; oldPin = "" }
}

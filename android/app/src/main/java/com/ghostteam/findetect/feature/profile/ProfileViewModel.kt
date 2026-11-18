package com.ghostteam.findetect.feature.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghostteam.findetect.domain.model.*
import com.ghostteam.findetect.domain.repository.AuthRepository
import com.ghostteam.findetect.domain.repository.ProfileRepository
import com.ghostteam.findetect.domain.usecase.ObserveXpProgressUseCase
import com.ghostteam.findetect.feature.gamification.XpProgressStateHolder
import com.ghostteam.findetect.feature.gamification.XpProgressUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ProfileAction { LOAD, SAVE, DELETE }

data class ProfileUiState(
    val user: SessionUser? = null,
    val draftName: String = "",
    val refreshing: Boolean = false,
    val saving: Boolean = false,
    val deleting: Boolean = false,
    val confirmDelete: Boolean = false,
    val nameRejected: Boolean = false,
    val saved: Boolean = false,
    val error: ProfileFailure? = null,
    val errorAction: ProfileAction? = null,
    val xp: XpProgressUiState = XpProgressUiState(),
) {
    val busy: Boolean get() = saving || deleting
    val dirty: Boolean get() = user != null && draftName.trim() != user.name.trim()
    val validName: Boolean get() = UserNameValidation.error(draftName) == null
    val canSave: Boolean get() = dirty && validName && !busy
}

/** Draft state belongs to this navigation entry and login; network refreshes do not overwrite edits. */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profile: ProfileRepository,
    private val auth: AuthRepository,
    private val owner: SessionIdentity,
    private val savedState: SavedStateHandle,
    observeXp: ObserveXpProgressUseCase,
) : ViewModel() {
    private val xp = XpProgressStateHolder(viewModelScope, observeXp)
    private val initialUser = checkNotNull((auth.state.value as? SessionState.Active)?.takeIf { it.identity == owner }?.user)
    private val initialDraft = savedState.get<String>(DRAFT).takeIf { savedState.get<String>(OWNER) == owner.sessionId } ?: initialUser.name
    private val mutableState = MutableStateFlow(ProfileUiState(user = initialUser, draftName = initialDraft))
    val state = mutableState.asStateFlow()

    init {
        savedState[OWNER] = owner.sessionId
        viewModelScope.launch { xp.state.collect { progress -> mutableState.update { it.copy(xp = progress) } } }
        viewModelScope.launch {
            auth.state.collect { session ->
                val active = (session as? SessionState.Active)?.takeIf { it.identity == owner } ?: return@collect
                val previous = state.value
                val draft = if (!previous.dirty) active.user.name else previous.draftName
                val confirmedDraft = draft.trim() == active.user.name.trim()
                savedState[DRAFT] = draft
                mutableState.update { it.copy(user = active.user, draftName = draft,
                    nameRejected = it.nameRejected && !confirmedDraft,
                    error = if (confirmedDraft && it.errorAction == ProfileAction.SAVE) null else it.error,
                    errorAction = if (confirmedDraft && it.errorAction == ProfileAction.SAVE) null else it.errorAction) }
            }
        }
        refresh()
    }

    private fun current() = (auth.state.value as? SessionState.Active)?.identity == owner

    fun retryXp() { if (current()) xp.retry() }

    fun editName(value: String) {
        if (!current() || state.value.busy) return
        savedState[DRAFT] = value
        mutableState.update { it.copy(draftName = value, saved = false, nameRejected = false,
            error = if (it.errorAction == ProfileAction.SAVE) null else it.error,
            errorAction = if (it.errorAction == ProfileAction.SAVE) null else it.errorAction) }
    }

    fun refresh() {
        if (!current() || state.value.refreshing || state.value.busy) return
        mutableState.update { it.copy(refreshing = true,
            error = if (it.errorAction == ProfileAction.LOAD) null else it.error,
            errorAction = if (it.errorAction == ProfileAction.LOAD) null else it.errorAction) }
        viewModelScope.launch {
            try { profile.refresh(owner) }
            catch (error: CancellationException) { throw error }
            catch (error: Exception) { fail(error, ProfileAction.LOAD) }
            finally { if (current()) mutableState.update { it.copy(refreshing = false) } }
        }
    }

    fun save() {
        if (!current() || state.value.busy || !state.value.dirty) return
        if (!state.value.validName) { mutableState.update { it.copy(nameRejected = true) }; return }
        val name = state.value.draftName.trim()
        mutableState.update { it.copy(saving = true, error = null, errorAction = null, saved = false, nameRejected = false) }
        viewModelScope.launch {
            try {
                profile.saveName(owner, name)
                if (current()) {
                    savedState[DRAFT] = name
                    mutableState.update { it.copy(draftName = name, saved = true, error = null, errorAction = null) }
                }
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { fail(error, ProfileAction.SAVE) }
            finally { if (current()) mutableState.update { it.copy(saving = false) } }
        }
    }

    fun askDelete() {
        if (current() && !state.value.busy) mutableState.update { it.copy(confirmDelete = true, saved = false) }
    }
    fun dismissDelete() { mutableState.update { it.copy(confirmDelete = false) } }
    fun confirmDelete() {
        if (!current() || state.value.busy || !state.value.confirmDelete) return
        mutableState.update { it.copy(confirmDelete = false, deleting = true, saved = false, error = null, errorAction = null) }
        viewModelScope.launch {
            try { profile.deleteAccount(owner) }
            catch (error: CancellationException) { throw error }
            catch (error: Exception) { fail(error, ProfileAction.DELETE) }
            finally { if (current()) mutableState.update { it.copy(deleting = false) } }
        }
    }

    private fun fail(error: Exception, action: ProfileAction) {
        if (!current()) return
        val reason = (error as? ProfileException)?.reason ?: ProfileFailure.UNAVAILABLE
        mutableState.update { it.copy(error = reason, errorAction = action,
            nameRejected = action == ProfileAction.SAVE && reason == ProfileFailure.INVALID_NAME) }
    }

    private companion object { const val DRAFT = "profile_name_draft"; const val OWNER = "profile_draft_owner" }
}

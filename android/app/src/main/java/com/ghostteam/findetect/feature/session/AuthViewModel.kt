package com.ghostteam.findetect.feature.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghostteam.findetect.domain.model.AuthException
import com.ghostteam.findetect.domain.model.AuthFailure
import com.ghostteam.findetect.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(val working: Boolean = false, val error: AuthFailure? = null)

@HiltViewModel
class AuthViewModel @Inject constructor(private val auth: AuthRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(AuthUiState())
    val state = mutableState.asStateFlow()
    fun login(email: String, password: String) = submit { auth.login(email, password) }
    fun register(name: String, email: String, password: String) = submit { auth.register(name, email, password) }

    private fun submit(action: suspend () -> Unit) {
        if (state.value.working) return
        mutableState.value = AuthUiState(working = true)
        viewModelScope.launch {
            try { action(); mutableState.value = AuthUiState() }
            catch (error: CancellationException) { throw error }
            catch (error: AuthException) { mutableState.value = AuthUiState(error = error.reason) }
            catch (_: Exception) { mutableState.value = AuthUiState(error = AuthFailure.UNAVAILABLE) }
        }
    }
}

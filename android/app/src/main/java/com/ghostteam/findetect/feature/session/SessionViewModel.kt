package com.ghostteam.findetect.feature.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghostteam.findetect.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class SessionViewModel @Inject constructor(private val auth: AuthRepository) : ViewModel() {
    val state = auth.state
    fun retry() { viewModelScope.launch { auth.restore() } }
    fun refreshUser() { viewModelScope.launch { auth.refreshUser() } }
}

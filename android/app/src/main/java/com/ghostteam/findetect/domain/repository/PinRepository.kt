package com.ghostteam.findetect.domain.repository

import kotlinx.coroutines.flow.StateFlow

enum class PinStatus { LOADING, DISABLED, ENABLED, ERROR }

sealed interface PinResult {
    data object Success : PinResult
    data object Incorrect : PinResult
    data object StorageError : PinResult
    data class Throttled(val seconds: Int) : PinResult
}

interface PinRepository {
    val status: StateFlow<PinStatus>
    suspend fun refresh()
    suspend fun create(pin: String): PinResult
    suspend fun verify(pin: String): PinResult
    suspend fun change(oldPin: String, newPin: String): PinResult
    suspend fun disable(pin: String): PinResult
}

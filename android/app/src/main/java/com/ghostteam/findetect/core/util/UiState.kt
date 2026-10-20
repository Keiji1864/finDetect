package com.ghostteam.findetect.core.util

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Content<T>(val data: T) : UiState<T>
    data object Empty : UiState<Nothing>
    data class Error(val messageRes: Int, val canRetry: Boolean = true) : UiState<Nothing>
}
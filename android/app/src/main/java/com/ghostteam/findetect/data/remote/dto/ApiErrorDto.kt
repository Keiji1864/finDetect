package com.ghostteam.findetect.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ApiErrorDto(
    val code: String,
    val message: String,
    val details: List<ApiFieldErrorDto> = emptyList(),
)

@Serializable
data class ApiFieldErrorDto(val field: String, val message: String)

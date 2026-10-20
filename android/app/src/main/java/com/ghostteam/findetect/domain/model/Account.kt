package com.ghostteam.findetect.domain.model

data class Account(
    val id: String,
    val name: String,
    val emoji: String,
    val currency: String,
    val initialBalance: Double,
    val includeInForecast: Boolean = true,
    val isDeleted: Boolean = false,
)
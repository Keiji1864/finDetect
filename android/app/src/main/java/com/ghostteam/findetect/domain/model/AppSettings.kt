package com.ghostteam.findetect.domain.model

enum class AppTheme { LIGHT, DARK, SYSTEM }
enum class AppLanguage { RU, EN }
enum class AppCurrency(val symbol: String) {
    RUB("₽"), USD("$"), EUR("€"), CNY("¥")
}

data class AppSettings(
    val currency: AppCurrency = AppCurrency.RUB,
    val theme: AppTheme = AppTheme.SYSTEM,
    val language: AppLanguage = AppLanguage.RU,
    val gameElementsEnabled: Boolean = true,
    val forecastTipsEnabled: Boolean = true,
)

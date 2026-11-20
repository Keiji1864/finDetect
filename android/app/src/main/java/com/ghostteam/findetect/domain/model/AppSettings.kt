package com.ghostteam.findetect.domain.model

enum class AppTheme { LIGHT, DARK, SYSTEM }
enum class AppLanguage { RU, EN }
enum class AppCurrency(val symbol: String) {
    RUB("₽"), USD("$"), EUR("€"), CNY("¥")
}

data class AppSettings(
    val currency: AppCurrency = AppCurrency.RUB,
    val theme: AppTheme = AppTheme.SYSTEM,
    val gameElementsEnabled: Boolean = true,
    val forecastTipsEnabled: Boolean = true,
    val dailyReminder: DailyReminderSettings = DailyReminderSettings(),
    val incomeSchedule: IncomeSchedule? = null,
)

data class DailyReminderSettings(val enabled: Boolean = false, val hour: Int = 20) {
    init { require(hour in 18..22) }
}

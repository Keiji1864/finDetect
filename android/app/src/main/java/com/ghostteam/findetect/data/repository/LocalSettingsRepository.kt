package com.ghostteam.findetect.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ghostteam.findetect.domain.model.AppCurrency
import com.ghostteam.findetect.domain.model.AppSettings
import com.ghostteam.findetect.domain.model.DailyReminderSettings
import com.ghostteam.findetect.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

@Singleton
class LocalSettingsRepository @Inject constructor(
    @ApplicationContext context: Context,
) : SettingsRepository {
    private val store = context.settingsDataStore

    override val settings = store.data.map { preferences ->
        AppSettings(
            currency = AppCurrency.entries.firstOrNull { it.name == preferences[currencyKey] } ?: AppCurrency.RUB,
            gameElementsEnabled = preferences[gameKey] ?: true,
            dailyReminder = DailyReminderSettings(
                enabled = preferences[reminderEnabledKey] ?: false,
                hour = (preferences[reminderHourKey] ?: 20).coerceIn(18, 22),
            ),
        )
    }.distinctUntilChanged()

    override val currency = settings.map { it.currency }.distinctUntilChanged()

    override suspend fun setCurrency(currency: AppCurrency) {
        store.edit { it[currencyKey] = currency.name }
    }

    override suspend fun setGameElementsEnabled(enabled: Boolean) {
        store.edit { it[gameKey] = enabled }
    }

    override suspend fun setDailyReminderEnabled(enabled: Boolean) {
        store.edit { it[reminderEnabledKey] = enabled }
    }

    override suspend fun setDailyReminderHour(hour: Int) {
        require(hour in 18..22)
        store.edit { it[reminderHourKey] = hour }
    }

    private companion object {
        val currencyKey = stringPreferencesKey("display_currency")
        val gameKey = booleanPreferencesKey("game_elements_enabled")
        val reminderEnabledKey = booleanPreferencesKey("daily_reminder_enabled")
        val reminderHourKey = intPreferencesKey("daily_reminder_hour")
    }
}

package com.ghostteam.findetect.data.repository

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.ghostteam.findetect.data.session.DevicePreferences
import com.ghostteam.findetect.data.session.UserAccessGate
import com.ghostteam.findetect.domain.model.AppCurrency
import com.ghostteam.findetect.domain.model.AppSettings
import com.ghostteam.findetect.domain.model.DailyReminderSettings
import com.ghostteam.findetect.domain.repository.SettingsRepository
import com.ghostteam.findetect.data.local.dao.*
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class LocalSettingsRepository(
    private val device: DevicePreferences,
    private val store: DataStore<Preferences>,
    private val dao: PersonalSettingsDao,
    private val gate: UserAccessGate,
) : SettingsRepository {
    override val settings = combine(device.store.data, dao.observe()) { devicePreferences, preferences ->
        AppSettings(
            currency = AppCurrency.entries.firstOrNull { it.name == devicePreferences[currencyKey] } ?: AppCurrency.RUB,
            gameElementsEnabled = preferences.gameEnabled,
            dailyReminder = DailyReminderSettings(
                enabled = preferences.reminderEnabled,
                hour = preferences.reminderHour,
            ),
        )
    }.distinctUntilChanged()

    internal suspend fun initialize() {
        val old = store.data.first()
        dao.seed(PersonalSettings(gameEnabled = old[gameKey] ?: true, reminderEnabled = old[reminderEnabledKey] ?: false,
            reminderHour = (old[reminderHourKey] ?: 20).coerceIn(18,22), modifiedAt = "1970-01-01T00:00:00.000000000Z"))
    }

    override val currency = settings.map { it.currency }.distinctUntilChanged()

    override suspend fun setCurrency(currency: AppCurrency) = gate.use {
        device.store.edit { it[currencyKey] = currency.name }
        Unit
    }

    override suspend fun setGameElementsEnabled(enabled: Boolean) = gate.use {
        dao.game(enabled, Clock.System.now().toString())
        Unit
    }

    override suspend fun setDailyReminderEnabled(enabled: Boolean) = gate.use {
        dao.reminder(enabled, Clock.System.now().toString())
        Unit
    }

    override suspend fun setDailyReminderHour(hour: Int) = gate.use {
        require(hour in 18..22)
        dao.hour(hour, Clock.System.now().toString())
        Unit
    }

    private companion object {
        val currencyKey = stringPreferencesKey("display_currency")
        val gameKey = booleanPreferencesKey("game_elements_enabled")
        val reminderEnabledKey = booleanPreferencesKey("daily_reminder_enabled")
        val reminderHourKey = intPreferencesKey("daily_reminder_hour")
    }
}

package com.ghostteam.findetect.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ghostteam.findetect.domain.model.AppCurrency
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

    override val currency = store.data.map { preferences ->
        AppCurrency.entries.firstOrNull { it.name == preferences[currencyKey] } ?: AppCurrency.RUB
    }.distinctUntilChanged()

    override suspend fun setCurrency(currency: AppCurrency) {
        store.edit { it[currencyKey] = currency.name }
    }

    private companion object {
        val currencyKey = stringPreferencesKey("display_currency")
    }
}

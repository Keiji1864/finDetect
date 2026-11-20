package com.ghostteam.findetect.core.util

import android.content.Context
import android.content.res.Configuration
import com.ghostteam.findetect.domain.model.AppLanguage
import java.util.Locale

object ApplicationLanguage {
    private const val FILE = "app_language"
    private const val KEY = "language"

    fun read(context: Context): AppLanguage {
        val stored = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, null)
        return AppLanguage.entries.firstOrNull { it.name == stored } ?: AppLanguage.RU
    }

    fun save(context: Context, language: AppLanguage) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(KEY, language.name).apply()
    }

    fun configuration(context: Context): Configuration {
        val locale = Locale.forLanguageTag(read(context).name.lowercase(Locale.ROOT))
        return Configuration().apply { setLocale(locale) }
    }

    fun localized(context: Context): Context = context.createConfigurationContext(configuration(context))
}

package com.luntikius.wallet.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface SettingsRepository {
    val themeMode: StateFlow<AppThemeMode>
    val showEducations: StateFlow<Boolean>

    fun setThemeMode(themeMode: AppThemeMode)

    fun setShowEducations(enabled: Boolean)
}

class SharedPreferencesSettingsRepository(context: Context) : SettingsRepository {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(readThemeMode())
    override val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _showEducations = MutableStateFlow(preferences.getBoolean(KEY_SHOW_EDUCATIONS, false))
    override val showEducations: StateFlow<Boolean> = _showEducations.asStateFlow()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            KEY_THEME_MODE -> _themeMode.value = readThemeMode()
            KEY_SHOW_EDUCATIONS -> _showEducations.value = preferences.getBoolean(KEY_SHOW_EDUCATIONS, false)
        }
    }

    init {
        preferences.registerOnSharedPreferenceChangeListener(listener)
    }

    override fun setThemeMode(themeMode: AppThemeMode) {
        preferences.edit()
            .putString(KEY_THEME_MODE, themeMode.name)
            .apply()
        _themeMode.value = themeMode
    }

    override fun setShowEducations(enabled: Boolean) {
        preferences.edit()
            .putBoolean(KEY_SHOW_EDUCATIONS, enabled)
            .apply()
        _showEducations.value = enabled
    }

    private fun readThemeMode(): AppThemeMode {
        val savedValue = preferences.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name)
        return AppThemeMode.entries.firstOrNull { themeMode -> themeMode.name == savedValue } ?: AppThemeMode.SYSTEM
    }

    private companion object {
        const val PREFERENCES_NAME = "wallet_settings"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_SHOW_EDUCATIONS = "show_educations"
    }
}

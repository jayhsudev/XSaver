package org.jayhsu.xsaver.ui.viewmodel

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.core.os.LocaleListCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

// DataStore extension
import androidx.datastore.preferences.preferencesDataStore
import android.content.Context

private val Context.dataStore by preferencesDataStore(name = "settings")

enum class ThemeMode { LIGHT, DARK, SYSTEM }

// History screen related enums (persisted settings)
enum class HistorySortBy { DownloadTime, FileSize }
enum class HistoryViewMode { List, Grid }

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {
    private val appContext = getApplication<Application>().applicationContext

    private val THEME_KEY = intPreferencesKey("theme_mode")
    private val LOCALE_KEY = stringPreferencesKey("locale")
    private val HISTORY_SORT_KEY = intPreferencesKey("history_sort_by") // 0 time, 1 size
    private val HISTORY_VIEW_MODE_KEY = intPreferencesKey("history_view_mode") // 0 list, 1 grid

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode

    private val _language = MutableStateFlow("zh") // zh or en
    val language: StateFlow<String> = _language

    private val _historySortBy = MutableStateFlow(HistorySortBy.DownloadTime)
    val historySortBy: StateFlow<HistorySortBy> = _historySortBy

    private val _historyViewMode = MutableStateFlow(HistoryViewMode.List)
    val historyViewMode: StateFlow<HistoryViewMode> = _historyViewMode

    init {
        viewModelScope.launch {
            appContext.dataStore.data.map { prefs ->
                when (prefs[THEME_KEY] ?: 2) { // 0 light, 1 dark, 2 system
                    0 -> ThemeMode.LIGHT
                    1 -> ThemeMode.DARK
                    else -> ThemeMode.SYSTEM
                }
            }.collect { mode ->
                _themeMode.value = mode
                applyTheme(mode)
            }
        }
        viewModelScope.launch {
            appContext.dataStore.data.map { prefs -> prefs[LOCALE_KEY] ?: "zh" }
                .collect { lang ->
                    _language.value = lang
                    applyLocale(lang)
                }
        }
        viewModelScope.launch {
            appContext.dataStore.data.map { prefs ->
                when (prefs[HISTORY_SORT_KEY] ?: 0) {
                    0 -> HistorySortBy.DownloadTime
                    else -> HistorySortBy.FileSize
                }
            }.collect { _historySortBy.value = it }
        }
        viewModelScope.launch {
            appContext.dataStore.data.map { prefs ->
                when (prefs[HISTORY_VIEW_MODE_KEY] ?: 0) {
                    0 -> HistoryViewMode.List
                    else -> HistoryViewMode.Grid
                }
            }.collect { _historyViewMode.value = it }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            appContext.dataStore.edit { prefs ->
                prefs[THEME_KEY] = when (mode) { ThemeMode.LIGHT -> 0; ThemeMode.DARK -> 1; ThemeMode.SYSTEM -> 2 }
            }
        }
    }

    fun setLanguage(lang: String) { // "zh" or "en"
        viewModelScope.launch {
            appContext.dataStore.edit { prefs ->
                prefs[LOCALE_KEY] = lang
            }
        }
    }

    fun setHistorySortBy(sort: HistorySortBy) {
        viewModelScope.launch {
            appContext.dataStore.edit { prefs ->
                prefs[HISTORY_SORT_KEY] = when (sort) {
                    HistorySortBy.DownloadTime -> 0
                    HistorySortBy.FileSize -> 1
                }
            }
        }
    }

    fun toggleHistoryViewMode() {
        val newMode = if (_historyViewMode.value == HistoryViewMode.List) HistoryViewMode.Grid else HistoryViewMode.List
        setHistoryViewMode(newMode)
    }

    fun setHistoryViewMode(mode: HistoryViewMode) {
        viewModelScope.launch {
            appContext.dataStore.edit { prefs ->
                prefs[HISTORY_VIEW_MODE_KEY] = when (mode) {
                    HistoryViewMode.List -> 0
                    HistoryViewMode.Grid -> 1
                }
            }
        }
    }

    private fun applyTheme(mode: ThemeMode) {
        val nightMode = when (mode) {
            ThemeMode.LIGHT -> MODE_NIGHT_NO
            ThemeMode.DARK -> MODE_NIGHT_YES
            ThemeMode.SYSTEM -> MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    private fun applyLocale(lang: String) {
        val locales = when (lang) {
            "en" -> LocaleListCompat.forLanguageTags("en")
            else -> LocaleListCompat.forLanguageTags("zh")
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }
}

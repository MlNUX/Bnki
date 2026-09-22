package com.example.bnki.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Theme-Modus der App. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Einfache, persistente App-Einstellungen (SharedPreferences) mit reaktivem State.
 */
class AppSettings private constructor(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("bnki_settings", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _devMode = MutableStateFlow(prefs.getBoolean(KEY_DEV, false))
    /** Dev-Modus: Lern-Sessions enthalten immer ALLE Karten (ohne Fälligkeit/Limit). */
    val devMode: StateFlow<Boolean> = _devMode.asStateFlow()

    private val _canvasGrid = MutableStateFlow(prefs.getBoolean(KEY_GRID, false))
    /** Kariertes Raster auf der Schreib-Zeichenfläche anzeigen. */
    val canvasGrid: StateFlow<Boolean> = _canvasGrid.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString(KEY_THEME, mode.name).apply()
    }

    fun setDevMode(enabled: Boolean) {
        _devMode.value = enabled
        prefs.edit().putBoolean(KEY_DEV, enabled).apply()
    }

    fun setCanvasGrid(enabled: Boolean) {
        _canvasGrid.value = enabled
        prefs.edit().putBoolean(KEY_GRID, enabled).apply()
    }

    private fun loadThemeMode(): ThemeMode =
        prefs.getString(KEY_THEME, null)
            ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.SYSTEM

    companion object {
        private const val KEY_THEME = "theme_mode"
        private const val KEY_DEV = "dev_mode"
        private const val KEY_GRID = "canvas_grid"

        @Volatile
        private var INSTANCE: AppSettings? = null

        fun get(context: Context): AppSettings =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppSettings(context).also { INSTANCE = it }
            }
    }
}

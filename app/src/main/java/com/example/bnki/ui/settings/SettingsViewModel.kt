package com.example.bnki.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.bnki.data.AppSettings
import com.example.bnki.data.ThemeMode
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val settings = AppSettings.get(app)

    val themeMode: StateFlow<ThemeMode> = settings.themeMode
    val devMode: StateFlow<Boolean> = settings.devMode
    val canvasGrid: StateFlow<Boolean> = settings.canvasGrid

    fun setThemeMode(mode: ThemeMode) = settings.setThemeMode(mode)
    fun setDevMode(enabled: Boolean) = settings.setDevMode(enabled)
    fun setCanvasGrid(enabled: Boolean) = settings.setCanvasGrid(enabled)
}

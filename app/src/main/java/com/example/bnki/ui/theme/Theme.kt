package com.example.bnki.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Purple500 = Color(0xFF6200EE)
private val Purple700 = Color(0xFF3700B3)
private val Purple200 = Color(0xFFBB86FC)
private val Teal200 = Color(0xFF03DAC5)
private val Teal700 = Color(0xFF018786)

private val LightColors = lightColorScheme(
    primary = Purple500,
    onPrimary = Color.White,
    primaryContainer = Purple200,
    secondary = Teal700,
    onSecondary = Color.White,
    tertiary = Teal200,
)

private val DarkColors = darkColorScheme(
    primary = Purple200,
    onPrimary = Color.Black,
    primaryContainer = Purple700,
    secondary = Teal200,
    onSecondary = Color.Black,
    tertiary = Teal200,
)

@Composable
fun BnkiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        content = content,
    )
}

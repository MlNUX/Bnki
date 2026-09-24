package com.example.bnki.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Warmes Bernstein-/Orange-Schema. Systemfarben bleiben aus, damit das Design
// auf jedem Gerät konsistent ohne Blautöne erscheint.
private val LightColors = lightColorScheme(
    primary = Color(0xFF9A3F00),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBC8),
    onPrimaryContainer = Color(0xFF331100),
    secondary = Color(0xFF77574A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDBCB),
    onSecondaryContainer = Color(0xFF2C160D),
    tertiary = Color(0xFF6A5F00),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE96B),
    onTertiaryContainer = Color(0xFF211B00),
    background = Color(0xFFFFF8F5),
    onBackground = Color(0xFF231A16),
    surface = Color(0xFFFFF8F5),
    onSurface = Color(0xFF231A16),
    surfaceVariant = Color(0xFFF4DDD4),
    onSurfaceVariant = Color(0xFF55433C),
    outline = Color(0xFF89736A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB68A),
    onPrimary = Color(0xFF542000),
    primaryContainer = Color(0xFF783000),
    onPrimaryContainer = Color(0xFFFFDBC8),
    secondary = Color(0xFFE7BDB0),
    onSecondary = Color(0xFF442A20),
    secondaryContainer = Color(0xFF5D4035),
    onSecondaryContainer = Color(0xFFFFDBCB),
    tertiary = Color(0xFFFFDD43),
    onTertiary = Color(0xFF363000),
    tertiaryContainer = Color(0xFF504700),
    onTertiaryContainer = Color(0xFFFFE96B),
    background = Color(0xFF1A1210),
    onBackground = Color(0xFFF0DFD8),
    surface = Color(0xFF1A1210),
    onSurface = Color(0xFFF0DFD8),
    surfaceVariant = Color(0xFF53433D),
    onSurfaceVariant = Color(0xFFD8C2B9),
    outline = Color(0xFFA38B81),
)

@Composable
fun BnkiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colors,
        content = content,
    )
}

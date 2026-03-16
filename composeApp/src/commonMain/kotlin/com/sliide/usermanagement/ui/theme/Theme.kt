package com.sliide.usermanagement.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val primaryLight = Color(0xFF1A6EE6)
private val secondaryLight = Color(0xFF7B5EA7)
private val tertiaryLight = Color(0xFF1A9E6E)
private val backgroundLight = Color(0xFFF8F9FF)
private val surfaceLight = Color(0xFFFFFFFF)

private val primaryDark = Color(0xFF7BB3F5)
private val secondaryDark = Color(0xFFCFB3F5)
private val tertiaryDark = Color(0xFF7DDBB3)
private val backgroundDark = Color(0xFF0E1117)
private val surfaceDark = Color(0xFF1A1E2A)

private val LightColorScheme = lightColorScheme(
    primary = primaryLight,
    secondary = secondaryLight,
    tertiary = tertiaryLight,
    background = backgroundLight,
    surface = surfaceLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = primaryDark,
    secondary = secondaryDark,
    tertiary = tertiaryDark,
    background = backgroundDark,
    surface = surfaceDark,
)

@Composable
fun UserManagementTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

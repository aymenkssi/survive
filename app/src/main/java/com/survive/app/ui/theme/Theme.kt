package com.survive.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palette « terrain » : vert forêt, sable, rouge d'alerte. Fort contraste pour la lecture en plein soleil.
private val Dark = darkColorScheme(
    primary = Color(0xFF8FCB9B),
    onPrimary = Color(0xFF0E2614),
    secondary = Color(0xFFE6C37A),
    background = Color(0xFF1B1F1A),
    surface = Color(0xFF232823),
    surfaceVariant = Color(0xFF2E352E),
    onBackground = Color(0xFFE8ECE4),
    onSurface = Color(0xFFE8ECE4),
    error = Color(0xFFFF8A80),
)

private val Light = lightColorScheme(
    primary = Color(0xFF2E5A3A),
    onPrimary = Color.White,
    secondary = Color(0xFF8A6A1F),
    background = Color(0xFFF7F5EE),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE9E6DA),
    onBackground = Color(0xFF1B1F1A),
    onSurface = Color(0xFF1B1F1A),
    error = Color(0xFFB3261E),
)

@Composable
fun SurviveTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
}

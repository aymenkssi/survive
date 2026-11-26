package com.survive.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.survive.app.data.ThemeMode

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
    onSurfaceVariant = Color(0xFFC9D0C5),
    primaryContainer = Color(0xFF2F4F37),
    onPrimaryContainer = Color(0xFFB5E8BF),
    secondaryContainer = Color(0xFF4A3F22),
    onSecondaryContainer = Color(0xFFF2DFA9),
    surfaceContainerLowest = Color(0xFF151915),
    surfaceContainerLow = Color(0xFF1F241F),
    surfaceContainer = Color(0xFF252B25),
    surfaceContainerHigh = Color(0xFF2B322B),
    surfaceContainerHighest = Color(0xFF323A32),
    outline = Color(0xFF8A9486),
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
    primaryContainer = Color(0xFFC4E8CB),
    onPrimaryContainer = Color(0xFF0E2614),
    secondaryContainer = Color(0xFFF0E0B0),
    onSecondaryContainer = Color(0xFF2B2000),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF4F2EA),
    surfaceContainer = Color(0xFFEFEDE4),
    surfaceContainerHigh = Color(0xFFE9E7DE),
    surfaceContainerHighest = Color(0xFFE3E1D8),
    error = Color(0xFFB3261E),
)

/**
 * Mode nuit rouge : uniquement du rouge sur noir, faible luminosité.
 * La lumière rouge préserve l'adaptation de l'œil à l'obscurité et se voit peu de loin.
 */
private val NightRed = darkColorScheme(
    primary = Color(0xFFD32F2F),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF3A0000),
    onPrimaryContainer = Color(0xFFE05050),
    secondary = Color(0xFFB71C1C),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF2A0000),
    onSecondaryContainer = Color(0xFFD04040),
    tertiary = Color(0xFFB71C1C),
    background = Color.Black,
    onBackground = Color(0xFFC62828),
    surface = Color(0xFF0D0000),
    onSurface = Color(0xFFC62828),
    surfaceVariant = Color(0xFF1A0000),
    onSurfaceVariant = Color(0xFFA52020),
    surfaceContainer = Color(0xFF120000),
    surfaceContainerHigh = Color(0xFF160000),
    surfaceContainerHighest = Color(0xFF1C0000),
    surfaceContainerLow = Color(0xFF0D0000),
    surfaceContainerLowest = Color.Black,
    outline = Color(0xFF7F1010),
    outlineVariant = Color(0xFF4A0808),
    error = Color(0xFFFF5252),
)

/** Vrai quand le mode nuit rouge est actif : les écrans évitent alors toute autre couleur. */
val LocalNightRed = staticCompositionLocalOf { false }

@Composable
fun SurviveTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    largeText: Boolean = false,
    content: @Composable () -> Unit,
) {
    val scheme = when (mode) {
        ThemeMode.SYSTEM -> if (isSystemInDarkTheme()) Dark else Light
        ThemeMode.LIGHT -> Light
        ThemeMode.DARK -> Dark
        ThemeMode.NIGHT_RED -> NightRed
    }
    val density = LocalDensity.current
    val scaled = if (largeText) Density(density.density, density.fontScale * 1.3f) else density
    CompositionLocalProvider(
        LocalNightRed provides (mode == ThemeMode.NIGHT_RED),
        LocalDensity provides scaled,
    ) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

/** Couleur d'accent, remplacée par le rouge du thème en mode nuit. */
@Composable
fun accent(color: Color): Color = if (LocalNightRed.current) MaterialTheme.colorScheme.primary else color

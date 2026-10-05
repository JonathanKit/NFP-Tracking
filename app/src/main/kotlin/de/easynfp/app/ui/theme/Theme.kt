package de.easynfp.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF2F6F68),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB8EDE4),
    onPrimaryContainer = Color(0xFF00201D),
    secondary = Color(0xFF4A6360),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCE8E3),
    onSecondaryContainer = Color(0xFF051F1D),
    tertiary = Color(0xFF9C4555),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD9DE),
    onTertiaryContainer = Color(0xFF3F0414),
    background = Color(0xFFF8FAF9),
    onBackground = Color(0xFF191C1C),
    surface = Color(0xFFF8FAF9),
    onSurface = Color(0xFF191C1C),
    surfaceVariant = Color(0xFFDAE5E2),
    onSurfaceVariant = Color(0xFF3F4947),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F5F4),
    surfaceContainer = Color(0xFFECEFEE),
    surfaceContainerHigh = Color(0xFFE6E9E8),
    surfaceContainerHighest = Color(0xFFE1E3E2),
    outline = Color(0xFF6F7977),
    outlineVariant = Color(0xFFBEC9C6),
    error = Color(0xFFBA1A1A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9CD1C8),
    onPrimary = Color(0xFF003732),
    primaryContainer = Color(0xFF145049),
    onPrimaryContainer = Color(0xFFB8EDE4),
    secondary = Color(0xFFB1CCC7),
    onSecondary = Color(0xFF1C3532),
    secondaryContainer = Color(0xFF334B48),
    onSecondaryContainer = Color(0xFFCCE8E3),
    tertiary = Color(0xFFFFB2BE),
    onTertiary = Color(0xFF5E1128),
    tertiaryContainer = Color(0xFF7D2D3E),
    onTertiaryContainer = Color(0xFFFFD9DE),
    background = Color(0xFF101414),
    onBackground = Color(0xFFE0E3E2),
    surface = Color(0xFF101414),
    onSurface = Color(0xFFE0E3E2),
    surfaceVariant = Color(0xFF3F4947),
    onSurfaceVariant = Color(0xFFBEC9C6),
    surfaceContainerLowest = Color(0xFF0B0F0E),
    surfaceContainerLow = Color(0xFF191C1C),
    surfaceContainer = Color(0xFF1D2020),
    surfaceContainerHigh = Color(0xFF272B2A),
    surfaceContainerHighest = Color(0xFF323535),
    outline = Color(0xFF899390),
    outlineVariant = Color(0xFF3F4947),
    error = Color(0xFFFFB4AB),
)

/**
 * Colors of the cycle phases. N-05: phases are never shown by color alone; every use also has a
 * pattern (hatching for fertile days) or a text label.
 */
@Immutable
data class PhaseColors(
    val infertile: Color,
    val onInfertile: Color,
    val fertile: Color,
    val onFertile: Color,
    val fertileHatch: Color,
    val bleeding: Color,
    val coverline: Color,
    val temperatureLine: Color,
    val excluded: Color,
    val reference: Color,
    val higher: Color,
    val disturbance: Color,
    val grid: Color,
    val gridStrong: Color,
    val today: Color,
)

private val LightPhaseColors = PhaseColors(
    infertile = Color(0xFFD3EEE7),
    onInfertile = Color(0xFF1F4D47),
    fertile = Color(0xFFFBDDE1),
    onFertile = Color(0xFF7D2D3E),
    fertileHatch = Color(0xFFE6A3AE),
    bleeding = Color(0xFFC62E47),
    coverline = Color(0xFF2F6F68),
    temperatureLine = Color(0xFF233130),
    excluded = Color(0xFF8A9592),
    reference = Color(0xFF2F6F68),
    higher = Color(0xFF9C4555),
    disturbance = Color(0xFFB26A00),
    grid = Color(0xFFE3E8E6),
    gridStrong = Color(0xFFC8D1CE),
    today = Color(0x332F6F68),
)

private val DarkPhaseColors = PhaseColors(
    infertile = Color(0xFF1E3D38),
    onInfertile = Color(0xFFB8EDE4),
    fertile = Color(0xFF4A2730),
    onFertile = Color(0xFFFFD9DE),
    fertileHatch = Color(0xFF8C4A58),
    bleeding = Color(0xFFFF8A9C),
    coverline = Color(0xFF9CD1C8),
    temperatureLine = Color(0xFFE0E3E2),
    excluded = Color(0xFF7A8582),
    reference = Color(0xFF9CD1C8),
    higher = Color(0xFFFFB2BE),
    disturbance = Color(0xFFFFB960),
    grid = Color(0xFF232828),
    gridStrong = Color(0xFF3A4140),
    today = Color(0x339CD1C8),
)

val LocalPhaseColors = staticCompositionLocalOf { LightPhaseColors }

@Composable
fun EasyNfpTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors: ColorScheme = if (darkTheme) DarkColors else LightColors
    CompositionLocalProvider(LocalPhaseColors provides if (darkTheme) DarkPhaseColors else LightPhaseColors) {
        MaterialTheme(colorScheme = colors, typography = Typography(), content = content)
    }
}

/** F-22: dark, dimmed colors for the quick entry, independent of the system setting. */
@Composable
fun QuickEntryTheme(content: @Composable () -> Unit) {
    val colors = DarkColors.copy(
        background = Color.Black,
        surface = Color.Black,
        onBackground = Color(0xFFB0B8B6),
        onSurface = Color(0xFFB0B8B6),
        primary = Color(0xFF6E9E97),
        onPrimary = Color.Black,
    )
    CompositionLocalProvider(LocalPhaseColors provides DarkPhaseColors) {
        MaterialTheme(colorScheme = colors, typography = Typography(), content = content)
    }
}

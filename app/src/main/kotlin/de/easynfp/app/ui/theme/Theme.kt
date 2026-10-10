package de.easynfp.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Warm and cheerful: peach as primary color, lavender and mint as companions.
private val LightColors = lightColorScheme(
    primary = Color(0xFF9A4530),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBD1),
    onPrimaryContainer = Color(0xFF3B0A02),
    secondary = Color(0xFF6750A4),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE9DDFF),
    onSecondaryContainer = Color(0xFF22005D),
    tertiary = Color(0xFF00695C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFA6F2E0),
    onTertiaryContainer = Color(0xFF00201B),
    background = Color(0xFFFFF8F5),
    onBackground = Color(0xFF201A1D),
    surface = Color(0xFFFFF8F5),
    onSurface = Color(0xFF201A1D),
    surfaceVariant = Color(0xFFF1DEE4),
    onSurfaceVariant = Color(0xFF51434A),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFCF0F2),
    surfaceContainer = Color(0xFFF7EAED),
    surfaceContainerHigh = Color(0xFFF1E4E7),
    surfaceContainerHighest = Color(0xFFEBDFE2),
    outline = Color(0xFF84737A),
    outlineVariant = Color(0xFFD6C2C9),
    error = Color(0xFFBA1A1A),
)

// Dark, but not gloomy: violet-tinted night surfaces with bright pastel accents.
private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB59F),
    onPrimary = Color(0xFF5C1A0A),
    primaryContainer = Color(0xFF7B2F1C),
    onPrimaryContainer = Color(0xFFFFDBD1),
    secondary = Color(0xFFCFBCFF),
    onSecondary = Color(0xFF381E72),
    secondaryContainer = Color(0xFF4A3A78),
    onSecondaryContainer = Color(0xFFE9DDFF),
    tertiary = Color(0xFF7FE0C8),
    onTertiary = Color(0xFF00382F),
    tertiaryContainer = Color(0xFF00544A),
    onTertiaryContainer = Color(0xFFA6F2E0),
    background = Color(0xFF151219),
    onBackground = Color(0xFFEDE5EE),
    surface = Color(0xFF151219),
    onSurface = Color(0xFFEDE5EE),
    surfaceVariant = Color(0xFF4A4152),
    onSurfaceVariant = Color(0xFFCDC1D2),
    surfaceContainerLowest = Color(0xFF0F0D13),
    surfaceContainerLow = Color(0xFF1D1A22),
    surfaceContainer = Color(0xFF221E28),
    surfaceContainerHigh = Color(0xFF2C2833),
    surfaceContainerHighest = Color(0xFF37323E),
    outline = Color(0xFF968B9C),
    outlineVariant = Color(0xFF4A4152),
    error = Color(0xFFFFB4AB),
)

/** Soft diagonal glow for highlight cards, e.g. the status on "Heute". */
val ColorScheme.highlightBrush: Brush
    get() = Brush.linearGradient(
        listOf(
            primaryContainer.copy(alpha = HIGHLIGHT_ALPHA),
            secondaryContainer.copy(alpha = HIGHLIGHT_ALPHA),
            tertiaryContainer.copy(alpha = HIGHLIGHT_ALPHA),
        ),
    )

/** Colorful fill for chart bars. */
val ColorScheme.barBrush: Brush get() = Brush.verticalGradient(listOf(primary, secondary))

private const val HIGHLIGHT_ALPHA = 0.55f

/** Rounder corners than the Material default feel softer and friendlier. */
private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
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
    /** Border of the today column, so evaluated days and the forecast are not confused (F-47). */
    val todayOutline: Color,
    /** Subtle background of infertile days over the whole chart height. */
    val infertileColumn: Color,
    /** Background of the column of the first higher measurement. */
    val firstHigherColumn: Color,
)

/** Light phase colors, also used for printing (F-52). */
val PrintPhaseColors = PhaseColors(
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
    todayOutline = Color(0xFF2F6F68),
    infertileColumn = Color(0x80D3EEE7),
    firstHigherColumn = Color(0x2E9C4555),
)

private val DarkPhaseColors = PhaseColors(
    infertile = Color(0xFF1B4A42),
    onInfertile = Color(0xFFA6F2E0),
    fertile = Color(0xFF5A2342),
    onFertile = Color(0xFFFFD8E8),
    fertileHatch = Color(0xFFA6527C),
    bleeding = Color(0xFFFF7E96),
    coverline = Color(0xFF7FE0C8),
    temperatureLine = Color(0xFFEDE5EE),
    excluded = Color(0xFF857A8A),
    reference = Color(0xFF7FE0C8),
    higher = Color(0xFFFFA8CB),
    disturbance = Color(0xFFFFC266),
    grid = Color(0xFF2A2530),
    gridStrong = Color(0xFF443D4B),
    today = Color(0x40FFB59F),
    todayOutline = Color(0xFFFFB59F),
    infertileColumn = Color(0x801B4A42),
    firstHigherColumn = Color(0x38FFA8CB),
)

val LocalPhaseColors = staticCompositionLocalOf { PrintPhaseColors }

@Composable
fun EasyNfpTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors: ColorScheme = if (darkTheme) DarkColors else LightColors
    CompositionLocalProvider(LocalPhaseColors provides if (darkTheme) DarkPhaseColors else PrintPhaseColors) {
        MaterialTheme(colorScheme = colors, typography = Typography(), shapes = AppShapes, content = content)
    }
}

/** F-22: dark, dimmed colors for the quick entry, independent of the system setting. */
@Composable
fun QuickEntryTheme(content: @Composable () -> Unit) {
    val colors = DarkColors.copy(
        background = Color.Black,
        surface = Color.Black,
        onBackground = Color(0xFFB8AFBA),
        onSurface = Color(0xFFB8AFBA),
        primary = Color(0xFFB98576),
        onPrimary = Color.Black,
    )
    CompositionLocalProvider(LocalPhaseColors provides DarkPhaseColors) {
        MaterialTheme(colorScheme = colors, typography = Typography(), shapes = AppShapes, content = content)
    }
}

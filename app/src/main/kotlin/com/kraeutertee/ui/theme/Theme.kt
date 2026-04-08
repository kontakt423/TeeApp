package com.kraeutertee.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Colors ─────────────────────────────────────────────────────────────────────
val GreenDark      = Color(0xFF1B5E20)
val GreenPrimary   = Color(0xFF2E7D32)
val GreenLight     = Color(0xFF4CAF50)
val GreenPale      = Color(0xFFE8F5E9)
val AmberPrimary   = Color(0xFFF57C00)
val AmberLight     = Color(0xFFFFCC02)
val BrownEarth     = Color(0xFF5D4037)
val BrownLight     = Color(0xFF8D6E63)
val CreamBg        = Color(0xFFFAF8F2)
val CreamSurface   = Color(0xFFF5F2EA)
val DarkBg         = Color(0xFF1A1F1A)
val DarkSurface    = Color(0xFF252B25)

private val LightColorScheme = lightColorScheme(
    primary          = GreenPrimary,
    onPrimary        = Color.White,
    primaryContainer = GreenPale,
    onPrimaryContainer = GreenDark,
    secondary        = AmberPrimary,
    onSecondary      = Color.White,
    secondaryContainer = Color(0xFFFFF3E0),
    onSecondaryContainer = Color(0xFF7F4800),
    tertiary         = BrownEarth,
    onTertiary       = Color.White,
    tertiaryContainer = Color(0xFFEFEBE9),
    background       = CreamBg,
    onBackground     = Color(0xFF1A1C1A),
    surface          = CreamSurface,
    onSurface        = Color(0xFF1A1C1A),
    surfaceVariant   = Color(0xFFDCE8DC),
    onSurfaceVariant = Color(0xFF404942),
    outline          = Color(0xFF707973),
    error            = Color(0xFFBA1A1A)
)

private val DarkColorScheme = darkColorScheme(
    primary          = GreenLight,
    onPrimary        = Color(0xFF003909),
    primaryContainer = GreenDark,
    onPrimaryContainer = Color(0xFFAAF0A4),
    secondary        = AmberLight,
    onSecondary      = Color(0xFF412D00),
    secondaryContainer = Color(0xFF5D4200),
    onSecondaryContainer = Color(0xFFFFDEA8),
    tertiary         = BrownLight,
    onTertiary       = Color(0xFF3E2723),
    background       = DarkBg,
    onBackground     = Color(0xFFE2E3DC),
    surface          = DarkSurface,
    onSurface        = Color(0xFFE2E3DC),
    surfaceVariant   = Color(0xFF404942),
    onSurfaceVariant = Color(0xFFC0C9C1),
    outline          = Color(0xFF8A938C)
)

@Composable
fun KraeuterTeeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography(),
        content     = content
    )
}

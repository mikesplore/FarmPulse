package co.farmpulse.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = ForestGreen,
    onPrimary = Color.White,
    background = BackgroundOffWhite,
    surface = SurfaceWhite,
    onSurface = OnSurfaceCharcoal,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = SecondaryText,
    outline = BorderGrey
)

// The design reference emphasizes a light, airy "farmer-friendly" UI,
// but we keep a dark variant for system compatibility.
private val DarkColors = darkColorScheme(
    primary = LightGreen,
    onPrimary = Color.White,
    background = Color(0xFF111310), // Matches mockup .phone-dark
    surface = Color(0xFF1E1F1B),
    onSurface = Color(0xFFE8E6DF),
    surfaceVariant = Color(0xFF252620),
    onSurfaceVariant = Color(0xFF7A7A70),
    outline = Color(0xFF2E2F2A)
)

@Composable
fun FarmPulseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = FarmPulseTypography,
        content = content
    )
}

package co.farmpulse.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = ForestGreen,
    onPrimary = SurfaceWhite,
    background = BackgroundOffWhite,
    surface = SurfaceWhite,
    onSurface = OnSurfaceCharcoal
)

private val DarkColors = darkColorScheme(
    primary = LightGreen,
    onPrimary = SurfaceWhite,
    background = OnSurfaceCharcoal,
    surface = OnSurfaceCharcoal,
    onSurface = SurfaceWhite
)

@Composable
fun FarmPulseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}


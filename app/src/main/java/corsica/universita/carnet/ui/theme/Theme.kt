package corsica.universita.carnet.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = DarkSeaBlue, onPrimary = DarkBackground,
    secondary = DarkMaquisGreen, onSecondary = DarkBackground,
    background = DarkBackground, onBackground = DarkText,
    surface = DarkSurface, onSurface = DarkText,
    surfaceVariant = DarkSurfaceVariant, onSurfaceVariant = DarkTextMuted,
    error = DarkDanger, onError = DarkBackground,
)
private val LightColorScheme = lightColorScheme(
    primary = SeaBlue, onPrimary = LightSurface,
    secondary = MaquisGreen, onSecondary = LightSurface,
    background = LightBackground, onBackground = LightText,
    surface = LightSurface, onSurface = LightText,
    surfaceVariant = LightSurfaceVariant, onSurfaceVariant = LightTextMuted,
    error = LightDanger, onError = LightSurface,
)

@Composable
fun CorsicaSoundTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}

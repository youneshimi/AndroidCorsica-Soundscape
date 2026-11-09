package corsica.universita.carnet.ui.theme

import androidx.compose.ui.graphics.Color

val SeaBlue = Color(0xFF1B6B93)
val SeaBlueHover = Color(0xFF155674)
val MaquisGreen = Color(0xFF4E9F3D)
val LightBackground = Color(0xFFF5F8F8)
val LightSurface = Color(0xFFFAFAFA)
val LightSurfaceVariant = Color(0xFFE7EEF0)
val LightText = Color(0xFF2C3333)
val LightTextMuted = Color(0xFF647477)
val LightDanger = Color(0xFFD84C54)
val DarkBackground = Color(0xFF162225)
val DarkSurface = Color(0xFF203035)
val DarkSurfaceVariant = Color(0xFF31464B)
val DarkSeaBlue = Color(0xFF70B8DF)
val DarkMaquisGreen = Color(0xFF89C879)
val DarkText = Color(0xFFF2F6F5)
val DarkTextMuted = Color(0xFFB0C3C6)
val DarkDanger = Color(0xFFFF8990)
val MicrophoneRed = Color(0xFFE45055)

fun categoryColor(category: String): Color = when (category) {
    "nature" -> Color(0xFF5B8C5A)
    "mer" -> Color(0xFF4A7C9B)
    "village" -> Color(0xFFC49A6C)
    "animal" -> Color(0xFF8B6B8A)
    "musique" -> Color(0xFF7B6B9B)
    "marche" -> Color(0xFFB8864E)
    else -> Color(0xFF6B7B8B)
}

fun categoryEmoji(category: String): String = when (category) {
    "nature" -> "🌿"
    "mer" -> "🌊"
    "village" -> "🏘️"
    "animal" -> "🐦"
    "musique" -> "🎵"
    "marche" -> "🥾"
    else -> "🔊"
}

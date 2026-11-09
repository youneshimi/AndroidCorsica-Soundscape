package corsica.universita.carnet.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import corsica.universita.carnet.R

private val inter = GoogleFont("Inter")
private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)
private val interFamily = FontFamily(
    Font(inter, provider, weight = FontWeight.Normal),
    Font(inter, provider, weight = FontWeight.Medium),
    Font(inter, provider, weight = FontWeight.SemiBold),
    Font(inter, provider, weight = FontWeight.Bold),
)
private val base = Typography()
val Typography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = interFamily),
    displayMedium = base.displayMedium.copy(fontFamily = interFamily),
    displaySmall = base.displaySmall.copy(fontFamily = interFamily),
    headlineLarge = base.headlineLarge.copy(fontFamily = interFamily),
    headlineMedium = base.headlineMedium.copy(fontFamily = interFamily),
    headlineSmall = base.headlineSmall.copy(fontFamily = interFamily),
    titleLarge = base.titleLarge.copy(fontFamily = interFamily),
    titleMedium = base.titleMedium.copy(fontFamily = interFamily),
    titleSmall = base.titleSmall.copy(fontFamily = interFamily),
    bodyLarge = base.bodyLarge.copy(fontFamily = interFamily),
    bodyMedium = base.bodyMedium.copy(fontFamily = interFamily),
    bodySmall = base.bodySmall.copy(fontFamily = interFamily),
    labelLarge = base.labelLarge.copy(fontFamily = interFamily),
    labelMedium = base.labelMedium.copy(fontFamily = interFamily),
    labelSmall = base.labelSmall.copy(fontFamily = interFamily),
)

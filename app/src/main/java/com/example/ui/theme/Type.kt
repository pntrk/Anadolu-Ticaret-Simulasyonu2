package com.example.ui.theme
import com.example.ui.theme.RobotoMonoFontFamily

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.example.R

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val RajdhaniFont = GoogleFont("Rajdhani")
val PlusJakartaSansFont = GoogleFont("Plus Jakarta Sans")

val RobotoMonoFont = GoogleFont("Roboto Mono")

val RobotoMonoFontFamily = FontFamily(
    Font(googleFont = RobotoMonoFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = RobotoMonoFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = RobotoMonoFont, fontProvider = provider, weight = FontWeight.Bold)
)

val RajdhaniFontFamily = FontFamily(
    Font(googleFont = RajdhaniFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = RajdhaniFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = RajdhaniFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = RajdhaniFont, fontProvider = provider, weight = FontWeight.Bold)
)

val PlusJakartaSansFontFamily = FontFamily(
    Font(googleFont = PlusJakartaSansFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = PlusJakartaSansFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = PlusJakartaSansFont, fontProvider = provider, weight = FontWeight.Bold)
)

// Set of Material typography styles to start with
val Typography = Typography(
    displayLarge = TextStyle(fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.Bold, fontSize = 57.sp),
    displayMedium = TextStyle(fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.Bold, fontSize = 45.sp),
    displaySmall = TextStyle(fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.Bold, fontSize = 36.sp),
    headlineLarge = TextStyle(fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.Bold, fontSize = 32.sp),
    headlineMedium = TextStyle(fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 28.sp),
    headlineSmall = TextStyle(fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 24.sp),
    titleLarge = TextStyle(fontFamily = PlusJakartaSansFontFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = PlusJakartaSansFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = PlusJakartaSansFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    bodyLarge = TextStyle(fontFamily = PlusJakartaSansFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = PlusJakartaSansFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = PlusJakartaSansFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = RobotoMonoFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = RobotoMonoFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = RobotoMonoFontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp),
)

package com.loganmartlew.rangework.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF1F6A45),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB5F1C9),
    onPrimaryContainer = Color(0xFF002111),
    secondary = Color(0xFF4E6354),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD1E8D5),
    onSecondaryContainer = Color(0xFF0B1F13),
    tertiary = Color(0xFF3C6473),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC0E9FB),
    onTertiaryContainer = Color(0xFF001F28),
    background = Color(0xFFF7FBF5),
    onBackground = Color(0xFF171D18),
    surface = Color(0xFFF7FBF5),
    onSurface = Color(0xFF171D18),
    surfaceVariant = Color(0xFFDDE5DB),
    onSurfaceVariant = Color(0xFF414941),
    outline = Color(0xFF727970),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF99D4AE),
    onPrimary = Color(0xFF003920),
    primaryContainer = Color(0xFF005231),
    onPrimaryContainer = Color(0xFFB5F1C9),
    secondary = Color(0xFFB6CCBA),
    onSecondary = Color(0xFF223528),
    secondaryContainer = Color(0xFF384B3D),
    onSecondaryContainer = Color(0xFFD1E8D5),
    tertiary = Color(0xFFA4CDDE),
    onTertiary = Color(0xFF053543),
    tertiaryContainer = Color(0xFF234C5A),
    onTertiaryContainer = Color(0xFFC0E9FB),
    background = Color(0xFF0F1511),
    onBackground = Color(0xFFDFE4DD),
    surface = Color(0xFF0F1511),
    onSurface = Color(0xFFDFE4DD),
    surfaceVariant = Color(0xFF414941),
    onSurfaceVariant = Color(0xFFC1C9BF),
    outline = Color(0xFF8B9389),
)

private val RangeworkTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
)

@Composable
fun RangeworkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = RangeworkTypography,
        content = content,
    )
}

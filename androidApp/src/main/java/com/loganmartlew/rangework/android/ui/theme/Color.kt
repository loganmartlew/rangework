package com.loganmartlew.rangework.android.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// Rangework — Material 3 colour tokens
// Source palette: Warm Graphite + Forest Green + Sage
// Generated from: Primary #2A2A28 · Secondary #4A7C59 · Tertiary #8A9E94
// ---------------------------------------------------------------------------

// ---------------------------------------------------------------------------
// Primary tonal palette — Warm Graphite
// ---------------------------------------------------------------------------

val Primary0 = Color(0xFFF5F3EF)
val Primary10 = Color(0xFFEBE9E4)
val Primary20 = Color(0xFFD8D6D0)
val Primary30 = Color(0xFFC4C2BB)
val Primary40 = Color(0xFFB0AEA8)
val Primary50 = Color(0xFF888680)
val Primary60 = Color(0xFF5E5C58)
val Primary70 = Color(0xFF3C3A37)
val Primary80 = Color(0xFF2A2A28)
val Primary90 = Color(0xFF1C1C1A)
val Primary100 = Color(0xFF0E0E0D)

// ---------------------------------------------------------------------------
// Secondary tonal palette — Forest Green
// ---------------------------------------------------------------------------

val Secondary0 = Color(0xFFF0F6F2)
val Secondary10 = Color(0xFFDDEEE4)
val Secondary20 = Color(0xFFBBDDC9)
val Secondary30 = Color(0xFF93C8A8)
val Secondary40 = Color(0xFF6BB286)
val Secondary50 = Color(0xFF4A7C59)
val Secondary60 = Color(0xFF386044)
val Secondary70 = Color(0xFF274530)
val Secondary80 = Color(0xFF192D1F)
val Secondary90 = Color(0xFF0D1810)
val Secondary100 = Color(0xFF050C08)

// ---------------------------------------------------------------------------
// Tertiary tonal palette — Sage
// ---------------------------------------------------------------------------

val Tertiary0 = Color(0xFFF1F4F2)
val Tertiary10 = Color(0xFFE2E8E4)
val Tertiary20 = Color(0xFFC6D2CC)
val Tertiary30 = Color(0xFFAABCB4)
val Tertiary40 = Color(0xFF8FA8A0)
val Tertiary50 = Color(0xFF748E88)
val Tertiary60 = Color(0xFF5A7470)
val Tertiary70 = Color(0xFF425858)
val Tertiary80 = Color(0xFF2C3C3A)
val Tertiary90 = Color(0xFF182020)
val Tertiary100 = Color(0xFF0A1010)

// ---------------------------------------------------------------------------
// Neutral tonal palette — Warm Off-White Surfaces
// ---------------------------------------------------------------------------

val Neutral0 = Color(0xFFFAFAF8)
val Neutral10 = Color(0xFFF5F3EF)
val Neutral20 = Color(0xFFEAE8E4)
val Neutral30 = Color(0xFFD5D3CF)
val Neutral40 = Color(0xFFBFBDB9)
val Neutral50 = Color(0xFF8E8C88)
val Neutral60 = Color(0xFF605E5A)
val Neutral70 = Color(0xFF3E3C38)
val Neutral80 = Color(0xFF282624)
val Neutral90 = Color(0xFF161412)
val Neutral100 = Color(0xFF0A0908)

// ---------------------------------------------------------------------------
// Neutral Variant tonal palette
// ---------------------------------------------------------------------------

val NeutralVariant30 = Color(0xFFD5D3CF)
val NeutralVariant40 = Color(0xFFBFBDB9)
val NeutralVariant50 = Color(0xFF8E8C88)
val NeutralVariant60 = Color(0xFF605E5A)
val NeutralVariant70 = Color(0xFF3E3C38)

// ---------------------------------------------------------------------------
// Error — Material 3 default red (no override needed)
// ---------------------------------------------------------------------------

val Error0 = Color(0xFFFFF8F7)
val Error40 = Color(0xFFBA1A1A)
val Error80 = Color(0xFF690005)
val Error90 = Color(0xFF410002)
val ErrorContainer = Color(0xFFFFDAD6)
val OnErrorContainer = Color(0xFF410002)

// ---------------------------------------------------------------------------
// Light colour scheme
// ---------------------------------------------------------------------------

val RangeworkLightColorScheme: ColorScheme = lightColorScheme(
    primary = Primary80,
    onPrimary = Primary0,
    primaryContainer = Primary20,
    onPrimaryContainer = Primary90,
    secondary = Secondary60,
    onSecondary = Secondary0,
    secondaryContainer = Secondary20,
    onSecondaryContainer = Secondary90,
    tertiary = Tertiary60,
    onTertiary = Tertiary0,
    tertiaryContainer = Tertiary20,
    onTertiaryContainer = Tertiary90,
    error = Error40,
    onError = Color.White,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    background = Neutral0,
    onBackground = Neutral90,
    surface = Neutral10,
    onSurface = Neutral80,
    surfaceVariant = NeutralVariant40,
    onSurfaceVariant = NeutralVariant70,
    inverseSurface = Neutral80,
    inverseOnSurface = Neutral20,
    inversePrimary = Primary30,
    outline = NeutralVariant60,
    outlineVariant = NeutralVariant30,
    scrim = Color(0xFF000000),
)

// ---------------------------------------------------------------------------
// Dark colour scheme
// ---------------------------------------------------------------------------

val RangeworkDarkColorScheme: ColorScheme = darkColorScheme(
    primary = Primary30,
    onPrimary = Primary90,
    primaryContainer = Primary70,
    onPrimaryContainer = Primary20,
    secondary = Secondary30,
    onSecondary = Secondary90,
    secondaryContainer = Secondary70,
    onSecondaryContainer = Secondary20,
    tertiary = Tertiary30,
    onTertiary = Tertiary90,
    tertiaryContainer = Tertiary70,
    onTertiaryContainer = Tertiary20,
    error = Color(0xFFFFB4AB),
    onError = Error80,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Neutral90,
    onBackground = Neutral20,
    surface = Color(0xFF1C1A18),
    onSurface = Neutral30,
    surfaceVariant = NeutralVariant70,
    onSurfaceVariant = NeutralVariant30,
    inverseSurface = Neutral30,
    inverseOnSurface = Neutral80,
    inversePrimary = Primary80,
    outline = NeutralVariant50,
    outlineVariant = NeutralVariant70,
    scrim = Color(0xFF000000),
)

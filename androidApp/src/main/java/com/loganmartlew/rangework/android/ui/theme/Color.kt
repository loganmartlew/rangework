package com.loganmartlew.rangework.android.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// =============================================================================
// Rangework — Material 3 colour tokens
// Combo A: Deep Fairway primary · Lighter Sage secondary · Warm Graphite surfaces
// =============================================================================

// ---------------------------------------------------------------------------
// Primary tonal palette — Deep Fairway
// ---------------------------------------------------------------------------

val Primary0   = Color(0xFFF0FAF5)
val Primary10  = Color(0xFFDDEEE6)
val Primary20  = Color(0xFFB8DCC8)
val Primary30  = Color(0xFF8FC8A8)
val Primary40  = Color(0xFF60A882)
val Primary50  = Color(0xFF2D6A4F)
val Primary60  = Color(0xFF235438)
val Primary70  = Color(0xFF1A3D28)
val Primary80  = Color(0xFF12291A)
val Primary90  = Color(0xFF09160D)
val Primary100 = Color(0xFF030804)

// ---------------------------------------------------------------------------
// Secondary tonal palette — Lighter Sage
// ---------------------------------------------------------------------------

val Secondary0   = Color(0xFFF0F6F4)
val Secondary10  = Color(0xFFD6E8E4)
val Secondary20  = Color(0xFFB0D4CC)
val Secondary30  = Color(0xFF8ABFB6)
val Secondary40  = Color(0xFF6AA89E)
val Secondary50  = Color(0xFF52796F)
val Secondary60  = Color(0xFF3F6058)
val Secondary70  = Color(0xFF2E4740)
val Secondary80  = Color(0xFF1E302A)
val Secondary90  = Color(0xFF0F1A17)
val Secondary100 = Color(0xFF050D0B)

// ---------------------------------------------------------------------------
// Tertiary tonal palette — Sage Mist
// ---------------------------------------------------------------------------

val Tertiary0   = Color(0xFFF1F4F2)
val Tertiary10  = Color(0xFFE2E8E4)
val Tertiary20  = Color(0xFFC6D2CC)
val Tertiary30  = Color(0xFFAABCB4)
val Tertiary40  = Color(0xFF8FA8A0)
val Tertiary50  = Color(0xFF748E88)
val Tertiary60  = Color(0xFF5A7470)
val Tertiary70  = Color(0xFF425858)
val Tertiary80  = Color(0xFF2C3C3A)
val Tertiary90  = Color(0xFF182020)
val Tertiary100 = Color(0xFF0A1010)

// ---------------------------------------------------------------------------
// Neutral tonal palette — Warm Graphite surfaces
// ---------------------------------------------------------------------------

val Neutral0   = Color(0xFFFAFAF8)
val Neutral10  = Color(0xFFF5F3EF)
val Neutral20  = Color(0xFFEAE8E4)
val Neutral30  = Color(0xFFD5D3CF)
val Neutral40  = Color(0xFFBFBDB9)
val Neutral50  = Color(0xFF8E8C88)
val Neutral60  = Color(0xFF605E5A)
val Neutral70  = Color(0xFF3E3C38)
val Neutral80  = Color(0xFF282624)
val Neutral88  = Color(0xFF1C1A18)
val Neutral90  = Color(0xFF161412)
val Neutral100 = Color(0xFF0A0908)

// ---------------------------------------------------------------------------
// Neutral Variant tonal palette — Graphite borders & variants
// ---------------------------------------------------------------------------

val NeutralVariant0   = Color(0xFFF5F4F0)
val NeutralVariant10  = Color(0xFFEBE9E4)
val NeutralVariant20  = Color(0xFFD8D6D0)
val NeutralVariant30  = Color(0xFFC4C2BB)
val NeutralVariant40  = Color(0xFFB0AEA8)
val NeutralVariant50  = Color(0xFF888680)
val NeutralVariant60  = Color(0xFF5E5C58)
val NeutralVariant70  = Color(0xFF3C3A37)
val NeutralVariant80  = Color(0xFF2A2826)
val NeutralVariant90  = Color(0xFF181614)
val NeutralVariant100 = Color(0xFF0C0A08)

// ---------------------------------------------------------------------------
// Error — Material 3 default red
// ---------------------------------------------------------------------------

val Error0   = Color(0xFFFFFBFF)
val Error10  = Color(0xFFFFEDE9)
val Error20  = Color(0xFFFFDAD6)
val Error30  = Color(0xFFFFB4AB)
val Error40  = Color(0xFFFF897D)
val Error50  = Color(0xFFFF5449)
val Error60  = Color(0xFFDD3730)
val Error70  = Color(0xFFBA1A1A)
val Error80  = Color(0xFF93000A)
val Error90  = Color(0xFF690005)
val Error100 = Color(0xFF410002)

// ---------------------------------------------------------------------------
// Surface container stops — fractional neutral stops for surfaceContainer* roles
// ---------------------------------------------------------------------------

val NeutralSurface4      = Color(0xFFF7F5F1)
val NeutralSurface6      = Color(0xFFF5F3EF)
val NeutralSurface8      = Color(0xFFEFEDEA)
val NeutralSurface10     = Color(0xFFEAE8E4)
val NeutralSurface12     = Color(0xFFE4E2DE)
val NeutralSurfaceBright = Color(0xFFF9F7F3)
val NeutralSurfaceDim    = Color(0xFFD5D3CF)

val NeutralDark4      = Color(0xFF161412)
val NeutralDark6      = Color(0xFF1C1A18)
val NeutralDark8      = Color(0xFF222020)
val NeutralDark10     = Color(0xFF282624)
val NeutralDark12     = Color(0xFF2E2C2A)
val NeutralDarkBright = Color(0xFF363432)
val NeutralDarkDim    = Color(0xFF161412)

// =============================================================================
// Light scheme tokens
// =============================================================================

val LightPrimary            = Primary50          // #2D6A4F  Deep Fairway
val LightOnPrimary          = Neutral0           // #FAFAF8
val LightPrimaryContainer   = Primary20          // #B8DCC8  Light sage wash
val LightOnPrimaryContainer = Primary90          // #09160D

val LightSecondary            = Secondary50       // #52796F  Lighter Sage
val LightOnSecondary          = Secondary0        // #F0F6F4
val LightSecondaryContainer   = Secondary20       // #B0D4CC
val LightOnSecondaryContainer = Secondary90       // #0F1A17

val LightTertiary            = Tertiary60         // #5A7470
val LightOnTertiary          = Tertiary0          // #F1F4F2
val LightTertiaryContainer   = Tertiary20         // #C6D2CC
val LightOnTertiaryContainer = Tertiary90         // #182020

val LightError            = Error70              // #BA1A1A
val LightOnError          = Error0               // #FFFBFF
val LightErrorContainer   = Error20              // #FFDAD6
val LightOnErrorContainer = Error90              // #690005

val LightBackground   = Neutral0                // #FAFAF8
val LightOnBackground = Neutral90               // #161412

val LightSurface          = Neutral10            // #F5F3EF
val LightOnSurface        = Neutral80            // #282624
val LightSurfaceVariant   = Neutral20            // #EAE8E4
val LightOnSurfaceVariant = NeutralVariant70     // #3C3A37

val LightOutline        = NeutralVariant60       // #5E5C58
val LightOutlineVariant = NeutralVariant20       // #D8D6D0

val LightInverseSurface   = Neutral90           // #161412
val LightInverseOnSurface = Neutral20           // #EAE8E4
val LightInversePrimary   = Primary30           // #8FC8A8

val LightSurfaceTint = Primary50                // #2D6A4F
val LightScrim       = Color(0xFF000000)

// =============================================================================
// Dark scheme tokens
// =============================================================================

val DarkPrimary            = Primary30           // #8FC8A8  light green for dark bg
val DarkOnPrimary          = Primary90           // #09160D
val DarkPrimaryContainer   = Primary70           // #1A3D28  dark green container
val DarkOnPrimaryContainer = Primary20           // #B8DCC8

val DarkSecondary            = Secondary30        // #8ABFB6
val DarkOnSecondary          = Secondary90        // #0F1A17
val DarkSecondaryContainer   = Secondary70        // #2E4740
val DarkOnSecondaryContainer = Secondary20        // #B0D4CC

val DarkTertiary            = Tertiary30          // #AABCB4
val DarkOnTertiary          = Tertiary90          // #182020
val DarkTertiaryContainer   = Tertiary70          // #425858
val DarkOnTertiaryContainer = Tertiary20          // #C6D2CC

val DarkError            = Error30               // #FFB4AB
val DarkOnError          = Error80               // #93000A
val DarkErrorContainer   = Error70               // #BA1A1A
val DarkOnErrorContainer = Error20               // #FFDAD6

val DarkBackground   = Neutral90                // #161412
val DarkOnBackground = Neutral20                // #EAE8E4

val DarkSurface          = Neutral88             // #1C1A18
val DarkOnSurface        = Neutral30             // #D5D3CF
val DarkSurfaceVariant   = NeutralVariant70      // #3C3A37
val DarkOnSurfaceVariant = NeutralVariant30      // #C4C2BB

val DarkOutline        = Neutral50               // #8E8C88
val DarkOutlineVariant = NeutralVariant70        // #3C3A37

val DarkInverseSurface   = Neutral20             // #EAE8E4
val DarkInverseOnSurface = Neutral80             // #282624
val DarkInversePrimary   = Primary60             // #235438  dark green on light surface

val DarkSurfaceTint = Primary30                  // #8FC8A8
val DarkScrim       = Color(0xFF000000)

// =============================================================================
// Light colour scheme
// =============================================================================

val RangeworkLightColorScheme: ColorScheme = lightColorScheme(
    primary                = LightPrimary,
    onPrimary              = LightOnPrimary,
    primaryContainer       = LightPrimaryContainer,
    onPrimaryContainer     = LightOnPrimaryContainer,
    secondary              = LightSecondary,
    onSecondary            = LightOnSecondary,
    secondaryContainer     = LightSecondaryContainer,
    onSecondaryContainer   = LightOnSecondaryContainer,
    tertiary               = LightTertiary,
    onTertiary             = LightOnTertiary,
    tertiaryContainer      = LightTertiaryContainer,
    onTertiaryContainer    = LightOnTertiaryContainer,
    error                  = LightError,
    onError                = LightOnError,
    errorContainer         = LightErrorContainer,
    onErrorContainer       = LightOnErrorContainer,
    background             = LightBackground,
    onBackground           = LightOnBackground,
    surface                = LightSurface,
    onSurface              = LightOnSurface,
    surfaceVariant         = LightSurfaceVariant,
    onSurfaceVariant       = LightOnSurfaceVariant,
    outline                = LightOutline,
    outlineVariant         = LightOutlineVariant,
    inverseSurface         = LightInverseSurface,
    inverseOnSurface       = LightInverseOnSurface,
    inversePrimary         = LightInversePrimary,
    surfaceTint            = LightSurfaceTint,
    scrim                  = LightScrim,
    surfaceContainerLowest = NeutralSurface4,
    surfaceContainerLow    = NeutralSurface6,
    surfaceContainer       = NeutralSurface8,
    surfaceContainerHigh   = NeutralSurface10,
    surfaceContainerHighest = NeutralSurface12,
    surfaceBright          = NeutralSurfaceBright,
    surfaceDim             = NeutralSurfaceDim,
)

// =============================================================================
// Dark colour scheme
// =============================================================================

val RangeworkDarkColorScheme: ColorScheme = darkColorScheme(
    primary                = DarkPrimary,
    onPrimary              = DarkOnPrimary,
    primaryContainer       = DarkPrimaryContainer,
    onPrimaryContainer     = DarkOnPrimaryContainer,
    secondary              = DarkSecondary,
    onSecondary            = DarkOnSecondary,
    secondaryContainer     = DarkSecondaryContainer,
    onSecondaryContainer   = DarkOnSecondaryContainer,
    tertiary               = DarkTertiary,
    onTertiary             = DarkOnTertiary,
    tertiaryContainer      = DarkTertiaryContainer,
    onTertiaryContainer    = DarkOnTertiaryContainer,
    error                  = DarkError,
    onError                = DarkOnError,
    errorContainer         = DarkErrorContainer,
    onErrorContainer       = DarkOnErrorContainer,
    background             = DarkBackground,
    onBackground           = DarkOnBackground,
    surface                = DarkSurface,
    onSurface              = DarkOnSurface,
    surfaceVariant         = DarkSurfaceVariant,
    onSurfaceVariant       = DarkOnSurfaceVariant,
    outline                = DarkOutline,
    outlineVariant         = DarkOutlineVariant,
    inverseSurface         = DarkInverseSurface,
    inverseOnSurface       = DarkInverseOnSurface,
    inversePrimary         = DarkInversePrimary,
    surfaceTint            = DarkSurfaceTint,
    scrim                  = DarkScrim,
    surfaceContainerLowest = NeutralDark4,
    surfaceContainerLow    = NeutralDark6,
    surfaceContainer       = NeutralDark8,
    surfaceContainerHigh   = NeutralDark10,
    surfaceContainerHighest = NeutralDark12,
    surfaceBright          = NeutralDarkBright,
    surfaceDim             = NeutralDarkDim,
)

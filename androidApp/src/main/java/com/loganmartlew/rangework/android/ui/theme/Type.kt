package com.loganmartlew.rangework.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.loganmartlew.rangework.android.R

private val DmSans = FontFamily(
    Font(R.font.dm_sans_light, weight = FontWeight.W300),
    Font(R.font.dm_sans_light_italic, weight = FontWeight.W300, style = FontStyle.Italic),
    Font(R.font.dm_sans_regular, weight = FontWeight.W400),
    Font(R.font.dm_sans_regular_italic, weight = FontWeight.W400, style = FontStyle.Italic),
    Font(R.font.dm_sans_medium, weight = FontWeight.W500),
    Font(R.font.dm_sans_medium_italic, weight = FontWeight.W500, style = FontStyle.Italic),
)

val DmMono = FontFamily(
    Font(R.font.dm_mono_regular, weight = FontWeight.W400),
    Font(R.font.dm_mono_regular_italic, weight = FontWeight.W400, style = FontStyle.Italic),
    Font(R.font.dm_mono_medium, weight = FontWeight.W500),
    Font(R.font.dm_mono_medium_italic, weight = FontWeight.W500, style = FontStyle.Italic),
)

val RangeworkTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.W300,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.W300,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.W400,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.W400,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.W400,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.W400,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.W500,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.W500,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.W500,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.W400,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.W400,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.W400,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.W500,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.W500,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.W500,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
)

object RangeworkMono {

    /** Rest timers, large standalone metric values (e.g. "02:30", "68%"). */
    val large = TextStyle(
        fontFamily    = DmMono,
        fontWeight    = FontWeight.Medium,
        fontSize      = 32.sp,
        lineHeight    = 36.sp,
        letterSpacing = (-0.5).sp,
    )

    /** Ball counts, carry distances, rep totals shown as primary data. */
    val medium = TextStyle(
        fontFamily    = DmMono,
        fontWeight    = FontWeight.Medium,
        fontSize      = 20.sp,
        lineHeight    = 24.sp,
        letterSpacing = 0.sp,
    )

    /** Inline reps, step numbers, secondary numeric annotations. */
    val small = TextStyle(
        fontFamily    = DmMono,
        fontWeight    = FontWeight.Normal,
        fontSize      = 13.sp,
        lineHeight    = 18.sp,
        letterSpacing = 0.sp,
    )
}

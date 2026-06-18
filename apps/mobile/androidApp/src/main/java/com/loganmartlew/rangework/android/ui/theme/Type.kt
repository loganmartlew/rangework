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
    Font(R.font.dm_sans_light, weight = FontWeight(GeneratedRangeworkTypographyTokens.LightWeight)),
    Font(R.font.dm_sans_light_italic, weight = FontWeight(GeneratedRangeworkTypographyTokens.LightWeight), style = FontStyle.Italic),
    Font(R.font.dm_sans_regular, weight = FontWeight(GeneratedRangeworkTypographyTokens.RegularWeight)),
    Font(R.font.dm_sans_regular_italic, weight = FontWeight(GeneratedRangeworkTypographyTokens.RegularWeight), style = FontStyle.Italic),
    Font(R.font.dm_sans_medium, weight = FontWeight(GeneratedRangeworkTypographyTokens.MediumWeight)),
    Font(R.font.dm_sans_medium_italic, weight = FontWeight(GeneratedRangeworkTypographyTokens.MediumWeight), style = FontStyle.Italic),
)

val DmMono = FontFamily(
    Font(R.font.dm_mono_regular, weight = FontWeight(GeneratedRangeworkTypographyTokens.RegularWeight)),
    Font(R.font.dm_mono_regular_italic, weight = FontWeight(GeneratedRangeworkTypographyTokens.RegularWeight), style = FontStyle.Italic),
    Font(R.font.dm_mono_medium, weight = FontWeight(GeneratedRangeworkTypographyTokens.MediumWeight)),
    Font(R.font.dm_mono_medium_italic, weight = FontWeight(GeneratedRangeworkTypographyTokens.MediumWeight), style = FontStyle.Italic),
)

val RangeworkTypography = Typography(
    displayLarge = materialStyle(
        fontWeight = GeneratedRangeworkTypographyTokens.DisplayLargeFontWeight,
        fontSize = GeneratedRangeworkTypographyTokens.DisplayLargeFontSizeSp,
        lineHeight = GeneratedRangeworkTypographyTokens.DisplayLargeLineHeightSp,
        letterSpacing = GeneratedRangeworkTypographyTokens.DisplayLargeLetterSpacingSp,
    ),
    displayMedium = materialStyle(
        fontWeight = GeneratedRangeworkTypographyTokens.DisplayMediumFontWeight,
        fontSize = GeneratedRangeworkTypographyTokens.DisplayMediumFontSizeSp,
        lineHeight = GeneratedRangeworkTypographyTokens.DisplayMediumLineHeightSp,
        letterSpacing = GeneratedRangeworkTypographyTokens.DisplayMediumLetterSpacingSp,
    ),
    displaySmall = materialStyle(
        fontWeight = GeneratedRangeworkTypographyTokens.DisplaySmallFontWeight,
        fontSize = GeneratedRangeworkTypographyTokens.DisplaySmallFontSizeSp,
        lineHeight = GeneratedRangeworkTypographyTokens.DisplaySmallLineHeightSp,
        letterSpacing = GeneratedRangeworkTypographyTokens.DisplaySmallLetterSpacingSp,
    ),
    headlineLarge = materialStyle(
        fontWeight = GeneratedRangeworkTypographyTokens.HeadlineLargeFontWeight,
        fontSize = GeneratedRangeworkTypographyTokens.HeadlineLargeFontSizeSp,
        lineHeight = GeneratedRangeworkTypographyTokens.HeadlineLargeLineHeightSp,
        letterSpacing = GeneratedRangeworkTypographyTokens.HeadlineLargeLetterSpacingSp,
    ),
    headlineMedium = materialStyle(
        fontWeight = GeneratedRangeworkTypographyTokens.HeadlineMediumFontWeight,
        fontSize = GeneratedRangeworkTypographyTokens.HeadlineMediumFontSizeSp,
        lineHeight = GeneratedRangeworkTypographyTokens.HeadlineMediumLineHeightSp,
        letterSpacing = GeneratedRangeworkTypographyTokens.HeadlineMediumLetterSpacingSp,
    ),
    headlineSmall = materialStyle(
        fontWeight = GeneratedRangeworkTypographyTokens.HeadlineSmallFontWeight,
        fontSize = GeneratedRangeworkTypographyTokens.HeadlineSmallFontSizeSp,
        lineHeight = GeneratedRangeworkTypographyTokens.HeadlineSmallLineHeightSp,
        letterSpacing = GeneratedRangeworkTypographyTokens.HeadlineSmallLetterSpacingSp,
    ),
    titleLarge = materialStyle(
        fontWeight = GeneratedRangeworkTypographyTokens.TitleLargeFontWeight,
        fontSize = GeneratedRangeworkTypographyTokens.TitleLargeFontSizeSp,
        lineHeight = GeneratedRangeworkTypographyTokens.TitleLargeLineHeightSp,
        letterSpacing = GeneratedRangeworkTypographyTokens.TitleLargeLetterSpacingSp,
    ),
    titleMedium = materialStyle(
        fontWeight = GeneratedRangeworkTypographyTokens.TitleMediumFontWeight,
        fontSize = GeneratedRangeworkTypographyTokens.TitleMediumFontSizeSp,
        lineHeight = GeneratedRangeworkTypographyTokens.TitleMediumLineHeightSp,
        letterSpacing = GeneratedRangeworkTypographyTokens.TitleMediumLetterSpacingSp,
    ),
    titleSmall = materialStyle(
        fontWeight = GeneratedRangeworkTypographyTokens.TitleSmallFontWeight,
        fontSize = GeneratedRangeworkTypographyTokens.TitleSmallFontSizeSp,
        lineHeight = GeneratedRangeworkTypographyTokens.TitleSmallLineHeightSp,
        letterSpacing = GeneratedRangeworkTypographyTokens.TitleSmallLetterSpacingSp,
    ),
    bodyLarge = materialStyle(
        fontWeight = GeneratedRangeworkTypographyTokens.BodyLargeFontWeight,
        fontSize = GeneratedRangeworkTypographyTokens.BodyLargeFontSizeSp,
        lineHeight = GeneratedRangeworkTypographyTokens.BodyLargeLineHeightSp,
        letterSpacing = GeneratedRangeworkTypographyTokens.BodyLargeLetterSpacingSp,
    ),
    bodyMedium = materialStyle(
        fontWeight = GeneratedRangeworkTypographyTokens.BodyMediumFontWeight,
        fontSize = GeneratedRangeworkTypographyTokens.BodyMediumFontSizeSp,
        lineHeight = GeneratedRangeworkTypographyTokens.BodyMediumLineHeightSp,
        letterSpacing = GeneratedRangeworkTypographyTokens.BodyMediumLetterSpacingSp,
    ),
    bodySmall = materialStyle(
        fontWeight = GeneratedRangeworkTypographyTokens.BodySmallFontWeight,
        fontSize = GeneratedRangeworkTypographyTokens.BodySmallFontSizeSp,
        lineHeight = GeneratedRangeworkTypographyTokens.BodySmallLineHeightSp,
        letterSpacing = GeneratedRangeworkTypographyTokens.BodySmallLetterSpacingSp,
    ),
    labelLarge = materialStyle(
        fontWeight = GeneratedRangeworkTypographyTokens.LabelLargeFontWeight,
        fontSize = GeneratedRangeworkTypographyTokens.LabelLargeFontSizeSp,
        lineHeight = GeneratedRangeworkTypographyTokens.LabelLargeLineHeightSp,
        letterSpacing = GeneratedRangeworkTypographyTokens.LabelLargeLetterSpacingSp,
    ),
    labelMedium = materialStyle(
        fontWeight = GeneratedRangeworkTypographyTokens.LabelMediumFontWeight,
        fontSize = GeneratedRangeworkTypographyTokens.LabelMediumFontSizeSp,
        lineHeight = GeneratedRangeworkTypographyTokens.LabelMediumLineHeightSp,
        letterSpacing = GeneratedRangeworkTypographyTokens.LabelMediumLetterSpacingSp,
    ),
    labelSmall = materialStyle(
        fontWeight = GeneratedRangeworkTypographyTokens.LabelSmallFontWeight,
        fontSize = GeneratedRangeworkTypographyTokens.LabelSmallFontSizeSp,
        lineHeight = GeneratedRangeworkTypographyTokens.LabelSmallLineHeightSp,
        letterSpacing = GeneratedRangeworkTypographyTokens.LabelSmallLetterSpacingSp,
    ),
)

object RangeworkMono {

    /** Rest timers, large standalone metric values (e.g. "02:30", "68%"). */
    val large = TextStyle(
        fontFamily = DmMono,
        fontWeight = FontWeight(GeneratedRangeworkTypographyTokens.MonoLargeFontWeight),
        fontSize = GeneratedRangeworkTypographyTokens.MonoLargeFontSizeSp.sp,
        lineHeight = GeneratedRangeworkTypographyTokens.MonoLargeLineHeightSp.sp,
        letterSpacing = GeneratedRangeworkTypographyTokens.MonoLargeLetterSpacingSp.sp,
    )

    /** Ball counts, carry distances, rep totals shown as primary data. */
    val medium = TextStyle(
        fontFamily = DmMono,
        fontWeight = FontWeight(GeneratedRangeworkTypographyTokens.MonoMediumFontWeight),
        fontSize = GeneratedRangeworkTypographyTokens.MonoMediumFontSizeSp.sp,
        lineHeight = GeneratedRangeworkTypographyTokens.MonoMediumLineHeightSp.sp,
        letterSpacing = GeneratedRangeworkTypographyTokens.MonoMediumLetterSpacingSp.sp,
    )

    /** Inline reps, step numbers, secondary numeric annotations. */
    val small = TextStyle(
        fontFamily = DmMono,
        fontWeight = FontWeight(GeneratedRangeworkTypographyTokens.MonoSmallFontWeight),
        fontSize = GeneratedRangeworkTypographyTokens.MonoSmallFontSizeSp.sp,
        lineHeight = GeneratedRangeworkTypographyTokens.MonoSmallLineHeightSp.sp,
        letterSpacing = GeneratedRangeworkTypographyTokens.MonoSmallLetterSpacingSp.sp,
    )
}

private fun materialStyle(
    fontWeight: Int,
    fontSize: Float,
    lineHeight: Float,
    letterSpacing: Float,
): TextStyle = TextStyle(
    fontFamily = DmSans,
    fontWeight = FontWeight(fontWeight),
    fontSize = fontSize.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
)

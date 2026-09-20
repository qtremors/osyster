package dev.qtremors.osyster.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.qtremors.osyster.core.ui.R

/**
 * Roboto Flex is one variable font file. Each weight is an instance of it, so the APK carries one
 * file instead of four.
 */
@OptIn(ExperimentalTextApi::class)
private fun flex(weight: FontWeight) = Font(
    R.font.roboto_flex,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

val RobotoFlex = FontFamily(
    flex(FontWeight.Normal),
    flex(FontWeight.Medium),
    flex(FontWeight.SemiBold),
    flex(FontWeight.Bold)
)

private fun style(size: Int, line: Int, weight: FontWeight = FontWeight.Normal, tracking: Double = 0.0) =
    TextStyle(
        fontFamily = RobotoFlex,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = line.sp,
        letterSpacing = tracking.sp
    )

val OsysterTypography = Typography(
    displayLarge = style(57, 64, tracking = -0.25),
    displayMedium = style(45, 52),
    displaySmall = style(36, 44),
    headlineLarge = style(32, 40, FontWeight.Medium),
    headlineMedium = style(28, 36, FontWeight.Medium),
    headlineSmall = style(24, 32, FontWeight.Medium),
    titleLarge = style(22, 28, FontWeight.Medium),
    titleMedium = style(16, 24, FontWeight.Medium, 0.15),
    titleSmall = style(14, 20, FontWeight.Medium, 0.1),
    bodyLarge = style(16, 24, tracking = 0.5),
    bodyMedium = style(14, 20, tracking = 0.25),
    bodySmall = style(12, 16, tracking = 0.4),
    labelLarge = style(14, 20, FontWeight.Medium, 0.1),
    labelMedium = style(12, 16, FontWeight.Medium, 0.5),
    labelSmall = style(11, 16, FontWeight.Medium, 0.5)
)

/** A reading or measurement shown large, such as "74%" or "5.1 GB". Digits stay aligned. */
val MetricTextStyle: TextStyle = style(28, 34, FontWeight.SemiBold).copy(
    fontFeatureSettings = "tnum"
)

/** Values in fact rows that are machine strings (build IDs, kernels, codec names). */
val ValueTextStyle: TextStyle = style(14, 20, tracking = 0.1).copy(fontFeatureSettings = "tnum")

package kz.chaykin.potracheno.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

private val Base = Typography()

/** Цифры одинаковой ширины: иначе сумма «прыгает», пока её вводишь или пересчитываешь. */
private fun TextStyle.tabular(weight: FontWeight) = copy(fontWeight = weight, fontFeatureSettings = "tnum")

/**
 * Системный шрифт, как у брата: он уже подогнан под язык и размер шрифта в настройках.
 * Характер добирается жирностью — крупные суммы набраны Black.
 */
internal val PotrachenoTypography = Typography(
    displayLarge = Base.displayLarge.tabular(FontWeight.Black),
    displayMedium = Base.displayMedium.tabular(FontWeight.Black),
    displaySmall = Base.displaySmall.tabular(FontWeight.Black),
    headlineLarge = Base.headlineLarge.tabular(FontWeight.ExtraBold),
    headlineMedium = Base.headlineMedium.tabular(FontWeight.ExtraBold),
    headlineSmall = Base.headlineSmall.tabular(FontWeight.Bold),
    titleLarge = Base.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
    titleMedium = Base.titleMedium.tabular(FontWeight.Bold),
    titleSmall = Base.titleSmall.copy(fontWeight = FontWeight.Bold),
    bodyLarge = Base.bodyLarge,
    bodyMedium = Base.bodyMedium,
    bodySmall = Base.bodySmall,
    labelLarge = Base.labelLarge.copy(fontWeight = FontWeight.Bold),
    labelMedium = Base.labelMedium.copy(fontWeight = FontWeight.SemiBold),
    labelSmall = Base.labelSmall.copy(fontWeight = FontWeight.SemiBold),
)

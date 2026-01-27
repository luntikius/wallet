package com.luntikius.wallet.ui.utils

import android.text.Html
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.luntikius.wallet.data.model.Pass

/**
 * Parse hex color string to Color.
 */
fun parseColor(colorString: String): Color = try {
    Color(android.graphics.Color.parseColor(colorString))
} catch (e: Exception) {
    Color.Unspecified
}

/**
 * Ensure a color has sufficient contrast against a background.
 * Returns the original color if it has good contrast, otherwise returns the fallback color.
 */
fun ensureContrast(
    foregroundColor: Color?,
    backgroundColor: Color?,
    isDarkTheme: Boolean,
    lightFallback: Color,
    darkFallback: Color,
): Color {
    if (foregroundColor == null || backgroundColor == null) {
        return if (isDarkTheme) darkFallback else lightFallback
    }

    val contrast = calculateContrastRatio(foregroundColor, backgroundColor)

    // WCAG AA standard requires 4.5:1 for normal text
    return if (contrast >= 4.5) {
        foregroundColor
    } else {
        if (isDarkTheme) darkFallback else lightFallback
    }
}

/**
 * Calculate contrast ratio between two colors.
 * https://www.w3.org/TR/WCAG20-TECHS/G17.html
 */
private fun calculateContrastRatio(color1: Color, color2: Color): Float {
    val lum1 = color1.luminance()
    val lum2 = color2.luminance()
    val lighter = maxOf(lum1, lum2)
    val darker = minOf(lum1, lum2)
    return (lighter + 0.05f) / (darker + 0.05f)
}

/**
 * Strip HTML tags from a string and decode HTML entities.
 */
fun stripHtml(html: String): String = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString().trim()

/**
 * Blend two colors together with a given ratio.
 * @param color1 The first color (starting color)
 * @param color2 The second color (ending color)
 * @param ratio How much to blend towards color2 (0.0 = all color1, 1.0 = all color2)
 */
fun blendColors(color1: Color, color2: Color, ratio: Float): Color {
    val clampedRatio = ratio.coerceIn(0f, 1f)
    val inverseRatio = 1f - clampedRatio

    return Color(
        red = color1.red * inverseRatio + color2.red * clampedRatio,
        green = color1.green * inverseRatio + color2.green * clampedRatio,
        blue = color1.blue * inverseRatio + color2.blue * clampedRatio,
        alpha = color1.alpha * inverseRatio + color2.alpha * clampedRatio,
    )
}

/**
 * Create a gradient brush for custom pass cards.
 * Creates a linear gradient from lighter (top-left) to darker (bottom-right).
 * @param backgroundColor The pastel background color
 * @param foregroundColor The darker foreground/text color
 * @return Brush with gradient from backgroundColor to slightly darker blended color
 */
fun createCustomPassGradient(backgroundColor: Color, foregroundColor: Color): androidx.compose.ui.graphics.Brush {
    val darkerColor = blendColors(backgroundColor, foregroundColor, 0.2f)
    return androidx.compose.ui.graphics.Brush.linearGradient(
        colors = listOf(
            backgroundColor, // Lighter at top-left
            darkerColor, // Darker at bottom-right
        ),
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset.Infinite,
    )
}

/**
 * Data class holding the computed card colors for pass cards.
 *
 * @param background The background color of the card
 * @param foreground The foreground/accent color (may be null)
 * @param text The text color (guaranteed to have sufficient contrast with background)
 */
data class CardColors(
    val background: Color,
    val foreground: Color?,
    val text: Color,
)

/**
 * Compute card colors for a pass with proper contrast handling.
 *
 * Centralizes the color calculation logic used across all pass card components.
 * Ensures text color has sufficient contrast against the background.
 *
 * @param pass The pass to compute colors for
 * @param isDarkTheme Whether the system is in dark theme mode
 * @return CardColors with background, foreground, and contrast-checked text color
 */
@Composable
fun rememberCardColors(
    pass: Pass,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
): CardColors {
    val backgroundColor = pass.backgroundColor?.let { parseColor(it) }
        ?: MaterialTheme.colorScheme.surface

    val foregroundColor = pass.foregroundColor?.let { parseColor(it) }

    val textColor = ensureContrast(
        foregroundColor = foregroundColor,
        backgroundColor = backgroundColor,
        isDarkTheme = isDarkTheme,
        lightFallback = MaterialTheme.colorScheme.onSurface,
        darkFallback = MaterialTheme.colorScheme.onSurface,
    )

    return CardColors(
        background = backgroundColor,
        foreground = foregroundColor,
        text = textColor,
    )
}

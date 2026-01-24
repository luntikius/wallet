package com.luntikius.wallet.ui.utils

import android.text.Html
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Parse hex color string to Color.
 */
fun parseColor(colorString: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorString))
    } catch (e: Exception) {
        Color.Unspecified
    }
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
    darkFallback: Color
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
fun stripHtml(html: String): String {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
        Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString().trim()
    } else {
        @Suppress("DEPRECATION")
        Html.fromHtml(html).toString().trim()
    }
}

package com.luntikius.wallet.ui.utils

import android.text.Html
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.luntikius.wallet.data.model.Pass
import com.luntikius.wallet.designsystem.foundation.color.ensureContrast
import com.luntikius.wallet.designsystem.foundation.color.parseColor

/**
 * Strip HTML tags from a string and decode HTML entities.
 */
fun stripHtml(html: String): String = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString().trim()

/**
 * Data class holding the computed card colors for pass cards.
 *
 * @param background The background color of the card
 * @param foreground The foreground/accent color (may be null)
 * @param text The text color (guaranteed to have sufficient contrast with background)
 */
data class CardColors(val background: Color, val foreground: Color?, val text: Color)

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
fun rememberCardColors(pass: Pass, isDarkTheme: Boolean = isSystemInDarkTheme()): CardColors {
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

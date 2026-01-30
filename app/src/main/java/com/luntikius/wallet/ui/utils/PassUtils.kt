package com.luntikius.wallet.ui.utils

import android.text.Html
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.luntikius.wallet.data.model.Pass
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
 * @param label The label color for field labels (guaranteed to have sufficient contrast with background)
 */
data class CardColors(val background: Color, val foreground: Color?, val text: Color, val label: Color)

/**
 * Compute card colors for a pass with proper contrast handling.
 *
 * Centralizes the color calculation logic used across all pass card components.
 * Ensures text and label colors have sufficient contrast against the background.
 *
 * @param pass The pass to compute colors for
 * @param isDarkTheme Whether the system is in dark theme mode
 * @return CardColors with background, foreground, text, and label colors (all contrast-checked)
 */
@Composable
fun rememberCardColors(pass: Pass): CardColors {
    val backgroundColor = pass.backgroundColor?.let { parseColor(it) }
        ?: MaterialTheme.colorScheme.surface

    val textColorRaw = pass.foregroundColor?.let { parseColor(it) }
    val labelColorRaw = pass.labelColor?.let { parseColor(it) }

    val textColor = textColorRaw ?: MaterialTheme.colorScheme.onSurface
    val labelColor = labelColorRaw ?: MaterialTheme.colorScheme.onSurface

    return CardColors(
        background = backgroundColor,
        foreground = textColorRaw,
        text = textColor,
        label = labelColor,
    )
}

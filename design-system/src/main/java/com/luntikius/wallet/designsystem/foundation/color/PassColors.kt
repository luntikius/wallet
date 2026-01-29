package com.luntikius.wallet.designsystem.foundation.color

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Pass color palette data class.
 *
 * Represents a color pair for custom pass cards:
 * - background: Soft pastel color for the card background
 * - foreground: Darker variant for text and accents
 * - name: Display name for the color (e.g., "Red", "Blue")
 *
 * @property background The pastel background color
 * @property foreground The darker foreground/text color
 * @property name The human-readable name of the color
 */
@Immutable
data class PassColorPalette(val background: Color, val foreground: Color, val name: String)

/**
 * Default pass color palettes.
 *
 * A collection of 8 soft pastel background colors with corresponding darker foreground colors.
 * These colors are designed for custom pass cards and ensure good readability with sufficient contrast.
 */
val DefaultPassColors = listOf(
    PassColorPalette(
        background = Color(0xFFFFADAD),
        foreground = Color(0xFFCC5A5A),
        name = "Red",
    ),
    PassColorPalette(
        background = Color(0xFFFFD6A5),
        foreground = Color(0xFFCC8952),
        name = "Orange",
    ),
    PassColorPalette(
        background = Color(0xFFFDFFB6),
        foreground = Color(0xFFCACC63),
        name = "Yellow",
    ),
    PassColorPalette(
        background = Color(0xFFCAFFBF),
        foreground = Color(0xFF77CC6C),
        name = "Green",
    ),
    PassColorPalette(
        background = Color(0xFF9BF6FF),
        foreground = Color(0xFF48C3CC),
        name = "Cyan",
    ),
    PassColorPalette(
        background = Color(0xFFA0C4FF),
        foreground = Color(0xFF4D77CC),
        name = "Blue",
    ),
    PassColorPalette(
        background = Color(0xFFBDB2FF),
        foreground = Color(0xFF6A5FCC),
        name = "Purple",
    ),
    PassColorPalette(
        background = Color(0xFFFFC6FF),
        foreground = Color(0xFFCC73CC),
        name = "Pink",
    ),
)

/**
 * CompositionLocal for accessing pass color palettes throughout the app.
 *
 * Usage:
 * ```
 * val passColors = MaterialTheme.walletColors
 * ```
 */
val LocalWalletColors = staticCompositionLocalOf { DefaultPassColors }

/**
 * Extension property to access wallet pass colors from MaterialTheme.
 *
 * Usage:
 * ```
 * @Composable
 * fun MyComposable() {
 *     val passColors = MaterialTheme.walletColors
 *     // Use passColors[0].background, passColors[0].foreground, etc.
 * }
 * ```
 */
val MaterialTheme.walletColors: List<PassColorPalette>
    @Composable
    @ReadOnlyComposable
    get() = LocalWalletColors.current

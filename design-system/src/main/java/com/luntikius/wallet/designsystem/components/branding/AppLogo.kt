package com.luntikius.wallet.designsystem.components.branding

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.luntikius.wallet.designsystem.theme.AppNameTextStyle

/**
 * App logo component displaying ".wallet" text.
 *
 * Renders the app name ".wallet" using the Ultra font (AppNameTextStyle).
 * Used for branding throughout the app.
 *
 * Usage:
 * ```
 * AppLogo()
 * AppLogo(color = Color.White)
 * ```
 *
 * @param color The color of the text. If null, uses the default color from the text style.
 * @param modifier Optional modifier for the text
 */
@Composable
fun AppLogo(
    modifier: Modifier = Modifier,
    color: Color? = null,
) {
    Text(
        text = ".wallet",
        style = if (color != null) {
            AppNameTextStyle.copy(color = color)
        } else {
            AppNameTextStyle
        },
        modifier = modifier,
    )
}

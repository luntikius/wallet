package com.luntikius.wallet.ui.components.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Reusable flip button component for pass cards.
 *
 * Displays a semi-transparent white outlined button with a refresh icon and "Flip" text.
 * Used to trigger card flip animations in pass detail views.
 *
 * @param onClick Callback invoked when the button is clicked
 * @param modifier Optional modifier for the button
 */
@Composable
fun FlipButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth(0.5f)
            .height(40.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
    ) {
        Icon(
            Icons.Default.Refresh,
            contentDescription = "Flip card",
            modifier = Modifier.size(16.dp),
            tint = Color.White,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Flip",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
        )
    }
}

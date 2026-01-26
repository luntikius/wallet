package com.luntikius.wallet.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luntikius.wallet.data.model.RefreshStatus

/**
 * Custom snackbar for showing refresh status.
 * Displays loading, success, or error states with appropriate styling.
 */
@Composable
fun RefreshLoadingSnackbar(refreshStatus: RefreshStatus, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = refreshStatus !is RefreshStatus.Idle,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        val (backgroundColor, textColor, message, showProgress) = when (refreshStatus) {
            is RefreshStatus.Loading -> {
                val msg = if (refreshStatus.passId == null) {
                    "Refreshing all passes..."
                } else {
                    "Refreshing pass..."
                }
                Tuple4(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.onPrimaryContainer,
                    msg,
                    true,
                )
            }
            is RefreshStatus.Success -> {
                val msg = if (refreshStatus.updatedCount > 0) {
                    "${refreshStatus.updatedCount} pass(es) updated"
                } else {
                    "All passes are up to date"
                }
                Tuple4(
                    MaterialTheme.colorScheme.tertiaryContainer,
                    MaterialTheme.colorScheme.onTertiaryContainer,
                    msg,
                    false,
                )
            }
            is RefreshStatus.Error -> {
                Tuple4(
                    MaterialTheme.colorScheme.errorContainer,
                    MaterialTheme.colorScheme.onErrorContainer,
                    refreshStatus.message,
                    false,
                )
            }
            RefreshStatus.Idle -> {
                // Not visible, but need to provide values
                Tuple4(
                    MaterialTheme.colorScheme.surface,
                    MaterialTheme.colorScheme.onSurface,
                    "",
                    false,
                )
            }
        }

        Surface(
            color = backgroundColor,
            shape = RoundedCornerShape(8.dp),
            shadowElevation = 4.dp,
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = textColor,
                        strokeWidth = 2.dp,
                    )
                }
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                )
            }
        }
    }
}

/**
 * Helper data class for destructuring multiple values.
 */
private data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

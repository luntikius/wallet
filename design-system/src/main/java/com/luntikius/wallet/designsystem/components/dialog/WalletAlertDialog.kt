package com.luntikius.wallet.designsystem.components.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Alert dialog for the Wallet design system.
 *
 * A styled Material 3 alert dialog with icon, title, text, and action buttons.
 * Used for important user decisions and confirmations.
 *
 * Usage:
 * ```
 * WalletAlertDialog(
 *     onDismissRequest = { /* dismiss */ },
 *     onConfirmation = { /* confirm */ },
 *     dialogTitle = "Delete Pass?",
 *     dialogText = "This action cannot be undone.",
 *     icon = Icons.Default.Delete
 * )
 * ```
 *
 * @param onDismissRequest Called when the user dismisses the dialog
 * @param onConfirmation Called when the user confirms the action
 * @param dialogTitle The title of the dialog
 * @param dialogText The explanatory text in the dialog
 * @param icon Optional icon to display in the dialog
 * @param confirmText Text for the confirm button (default: "Confirm")
 * @param dismissText Text for the dismiss button (default: "Cancel")
 */
@Composable
fun WalletAlertDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    dialogTitle: String,
    dialogText: String,
    icon: ImageVector? = null,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel",
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        modifier = modifier,
        icon = if (icon != null) {
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        } else {
            null
        },
        title = {
            Text(text = dialogTitle)
        },
        text = {
            Text(text = dialogText)
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onConfirmation) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(dismissText)
            }
        },
    )
}

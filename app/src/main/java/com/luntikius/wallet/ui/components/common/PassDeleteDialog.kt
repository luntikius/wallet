package com.luntikius.wallet.ui.components.common

import androidx.compose.runtime.Composable
import com.luntikius.wallet.designsystem.components.dialog.WalletAlertDialog

/**
 * Reusable delete confirmation dialog for passes.
 *
 * Displays a confirmation dialog when the user attempts to delete a pass.
 * Handles the delete action and dismisses the dialog and parent screen on confirmation.
 *
 * @param showDialog Whether the dialog is currently visible
 * @param onDelete Callback invoked when user decides to delete a pass
 * @param onDismiss Callback invoked when the dialog is dismissed (both cancel and after delete)
 */
@Composable
fun PassDeleteDialog(showDialog: Boolean, onDelete: () -> Unit, onDismiss: () -> Unit) {
    if (showDialog) {
        WalletAlertDialog(
            onDismissRequest = onDismiss,
            onConfirmation = onDelete,
            dialogTitle = "Delete Pass",
            dialogText = "Are you sure you want to delete this pass? This action cannot be undone.",
            confirmText = "Delete",
            dismissText = "Cancel",
        )
    }
}

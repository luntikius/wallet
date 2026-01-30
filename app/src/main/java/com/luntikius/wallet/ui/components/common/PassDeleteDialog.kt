package com.luntikius.wallet.ui.components.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

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
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(text = "Delete Pass")
            },
            text = {
                Text(text = "Are you sure you want to delete this pass? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

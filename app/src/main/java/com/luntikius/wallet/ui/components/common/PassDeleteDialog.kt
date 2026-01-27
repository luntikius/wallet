package com.luntikius.wallet.ui.components.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.luntikius.wallet.data.model.Pass
import com.luntikius.wallet.ui.viewmodel.PassViewModel

/**
 * Reusable delete confirmation dialog for passes.
 *
 * Displays a confirmation dialog when the user attempts to delete a pass.
 * Handles the delete action and dismisses the dialog and parent screen on confirmation.
 *
 * @param pass The pass to delete
 * @param viewModel The ViewModel to handle the delete operation
 * @param showDialog Whether the dialog is currently visible
 * @param onDismiss Callback invoked when the dialog is dismissed (both cancel and after delete)
 */
@Composable
fun PassDeleteDialog(pass: Pass, viewModel: PassViewModel, showDialog: Boolean, onDismiss: () -> Unit) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text("Delete Pass")
            },
            text = {
                Text("Are you sure you want to delete this pass? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePass(pass)
                        onDismiss()
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                ) {
                    Text("Cancel")
                }
            },
        )
    }
}

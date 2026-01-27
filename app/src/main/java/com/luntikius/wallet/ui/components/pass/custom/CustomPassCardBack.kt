package com.luntikius.wallet.ui.components.pass.custom

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.luntikius.wallet.data.model.CustomPassJson
import com.luntikius.wallet.data.model.Pass
import com.luntikius.wallet.ui.components.common.EmptyStateMessage
import com.luntikius.wallet.ui.components.common.PassDeleteDialog
import com.luntikius.wallet.ui.components.pass.PassCardBackHeader
import com.luntikius.wallet.ui.utils.rememberCardColors
import com.luntikius.wallet.ui.viewmodel.PassViewModel

/**
 * Back side of a custom pass card.
 * Shows auto-refresh toggle (always disabled) and empty state.
 */
@Composable
fun CustomPassCardBack(
    pass: Pass,
    viewModel: PassViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardColors = rememberCardColors(pass)
    val backgroundColor = cardColors.background
    val textColor = cardColors.text

    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor, RoundedCornerShape(16.dp)),
    ) {
        // 1. HEADER ROW: Logo (left) + Action Buttons (right)
        PassCardBackHeader(
            logoPath = null, // Custom passes don't have logo images
            iconPath = null,
            textColor = textColor,
            onShareClick = {
                // TODO: Implement share functionality
            },
            onDeleteClick = { showDeleteDialog = true },
        )

        // 2. INFO BLOCKS: Scrollable content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center,
        ) {
            EmptyStateMessage(
                icon = Icons.Outlined.Info,
                message = "No additional information",
                tint = textColor,
            )
        }
    }

    // Delete confirmation dialog
    // Note: PassDeleteDialog will close itself and call onDismiss for both cancel and delete
    // The expansion overlay will auto-close when pass is deleted (pass becomes null in state)
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Pass") },
            text = { Text("Are you sure you want to delete this pass? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePass(pass)
                        showDeleteDialog = false
                        onDismiss() // Close expansion after delete
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

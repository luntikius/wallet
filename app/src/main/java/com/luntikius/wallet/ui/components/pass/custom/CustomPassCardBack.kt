package com.luntikius.wallet.ui.components.pass.custom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
fun CustomPassCardBack(pass: Pass, viewModel: PassViewModel, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
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
        EmptyStateMessage(
            message = "No additional information",
            tint = textColor,
            modifier = Modifier.fillMaxSize(),
        )
    }

    PassDeleteDialog(
        showDialog = showDeleteDialog,
        onDelete = {
            viewModel.deletePass(pass)
            showDeleteDialog = false
            onDismiss()
        },
        onDismiss = {
            showDeleteDialog = false
        },
    )
}

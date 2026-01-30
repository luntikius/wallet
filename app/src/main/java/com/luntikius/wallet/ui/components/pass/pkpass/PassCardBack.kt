package com.luntikius.wallet.ui.components.pass.pkpass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.luntikius.wallet.data.model.Pass
import com.luntikius.wallet.data.model.PassFormat
import com.luntikius.wallet.data.model.ShareStatus
import com.luntikius.wallet.data.parser.pkpass.PKPassJson
import com.luntikius.wallet.designsystem.foundation.spacing.spacing
import com.luntikius.wallet.ui.components.common.EmptyStateMessage
import com.luntikius.wallet.ui.components.common.PassDeleteDialog
import com.luntikius.wallet.ui.components.pass.PassCardBackHeader
import com.luntikius.wallet.ui.utils.rememberCardColors
import com.luntikius.wallet.ui.utils.sharePassFile
import com.luntikius.wallet.ui.viewmodel.PassViewModel
import kotlinx.coroutines.launch

/**
 * Back side of the pass card.
 */
@Composable
fun PassCardBack(
    pass: Pass,
    pkPassJson: PKPassJson?,
    viewModel: PassViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardColors = rememberCardColors(pass)
    val backgroundColor = cardColors.background
    val textColor = cardColors.text

    val shareStatus by viewModel.shareStatus.collectAsState()
    val context = LocalContext.current

    var showDeleteDialog by remember { mutableStateOf(false) }

    // Handle share status changes
    LaunchedEffect(shareStatus) {
        when (val status = shareStatus) {
            is ShareStatus.Success -> {
                if (status.passId == pass.id) {
                    sharePassFile(context, status.exportResult)
                    viewModel.resetShareStatus()
                }
            }
            is ShareStatus.Error -> {
                if (status.passId == pass.id) {
                    // Log error silently (matches app pattern)
                    viewModel.resetShareStatus()
                }
            }
            else -> { /* Idle or Loading */ }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor, RoundedCornerShape(16.dp)),
    ) {
        // 1. HEADER ROW: Logo (left) + Action Buttons (right)
        PassCardBackHeader(
            logoPath = pass.logoPath,
            iconPath = pass.iconPath,
            textColor = textColor,
            onShareClick = { viewModel.prepareSharePass(pass.id) }.takeIf { pass.format == PassFormat.PKPASS },
            onDeleteClick = { showDeleteDialog = true },
        )

        // 2. INFO BLOCKS: Scrollable content with back fields
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = MaterialTheme.spacing.mediumLarge,
                    end = MaterialTheme.spacing.mediumLarge,
                    bottom = MaterialTheme.spacing.mediumLarge,
                )
                .clip(RoundedCornerShape(12.dp)),
        ) {
            item {
                val coroutineScope = rememberCoroutineScope()
                val supportsAutoRefresh = remember(pkPassJson) {
                    pkPassJson?.webServiceURL != null
                }

                var isEnabled by remember(pass.autoRefreshEnabled, supportsAutoRefresh) {
                    mutableStateOf(if (supportsAutoRefresh) pass.autoRefreshEnabled else false)
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MaterialTheme.spacing.mediumLarge),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Automatic Refresh",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = if (supportsAutoRefresh) {
                                    "Automatically check for updates daily"
                                } else {
                                    "Not available for this pass"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = MaterialTheme.spacing.extraSmall),
                            )
                        }

                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { newValue ->
                                isEnabled = newValue
                                coroutineScope.launch {
                                    viewModel.setAutoRefreshEnabled(pass.id, newValue)
                                }
                            },
                            enabled = supportsAutoRefresh,
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
            }

            // Check if there are back fields to show
            val hasBackFields = pkPassJson?.let { json ->
                val structure = json.boardingPass ?: json.eventTicket
                    ?: json.coupon ?: json.storeCard ?: json.generic
                structure?.backFields?.any { field ->
                    field.value?.toString()?.isNotBlank() == true
                } ?: false
            } ?: false

            if (hasBackFields) {
                pkPassJson.let { json ->
                    val structure = json.boardingPass ?: json.eventTicket
                        ?: json.coupon ?: json.storeCard ?: json.generic

                    structure?.backFields?.let { fields ->
                        items(fields) { field ->
                            val content = field.value?.toString() ?: ""
                            if (content.isNotBlank()) {
                                InfoBlock(
                                    title = field.label ?: "",
                                    htmlContent = content,
                                    textColor = textColor,
                                )
                                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                            }
                        }
                    }
                }
            } else {
                item {
                    EmptyStateMessage(
                        message = "No additional information",
                        tint = textColor,
                        modifier = Modifier
                            .height(200.dp)
                            .fillMaxWidth(),
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
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

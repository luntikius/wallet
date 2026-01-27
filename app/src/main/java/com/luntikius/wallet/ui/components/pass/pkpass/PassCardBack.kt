package com.luntikius.wallet.ui.components.pass.pkpass

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.luntikius.wallet.data.model.Pass
import com.luntikius.wallet.data.parser.pkpass.PKPassJson
import com.luntikius.wallet.ui.utils.ensureContrast
import com.luntikius.wallet.ui.utils.parseColor
import com.luntikius.wallet.ui.viewmodel.PassViewModel
import kotlinx.coroutines.launch
import java.io.File

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
    val isDarkTheme = isSystemInDarkTheme()
    val backgroundColor = pass.backgroundColor?.let { parseColor(it) }
        ?: MaterialTheme.colorScheme.surface

    val textColor = ensureContrast(
        foregroundColor = pass.foregroundColor?.let { parseColor(it) },
        backgroundColor = backgroundColor,
        isDarkTheme = isDarkTheme,
        lightFallback = MaterialTheme.colorScheme.onSurface,
        darkFallback = MaterialTheme.colorScheme.onSurface,
    )

    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor, RoundedCornerShape(16.dp)),
    ) {
        // 1. HEADER ROW: Logo (left) + Action Buttons (right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Logo (prioritize logo over icon)
            val logoPath = pass.logoPath ?: pass.iconPath
            val logoFile = File(logoPath)
            if (logoFile.exists()) {
                val bitmap = BitmapFactory.decodeFile(logoFile.absolutePath)
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Logo",
                        modifier = Modifier
                            .height(40.dp)
                            .widthIn(max = 120.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Share button
            IconButton(
                onClick = {
                    // TODO: Implement share functionality
                },
            ) {
                Icon(
                    Icons.Outlined.Share,
                    contentDescription = "Share",
                    tint = textColor,
                )
            }

            // Delete button
            IconButton(
                onClick = { showDeleteDialog = true },
            ) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Delete",
                    tint = textColor,
                )
            }
        }

        // 2. INFO BLOCKS: Scrollable content with back fields
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
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
                            .padding(16.dp),
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
                                modifier = Modifier.padding(top = 4.dp),
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
                Spacer(modifier = Modifier.height(12.dp))
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
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillParentMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = textColor.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp),
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No additional information",
                                style = MaterialTheme.typography.bodyLarge,
                                color = textColor.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
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
                        showDeleteDialog = false
                        onDismiss()
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                ) {
                    Text("Cancel")
                }
            },
        )
    }
}

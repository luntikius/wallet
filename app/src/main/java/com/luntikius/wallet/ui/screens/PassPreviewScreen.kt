package com.luntikius.wallet.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.luntikius.wallet.data.model.PassData
import com.luntikius.wallet.data.model.getPassData
import com.luntikius.wallet.designsystem.components.branding.AppLogo
import com.luntikius.wallet.designsystem.components.button.WalletFilledButton
import com.luntikius.wallet.designsystem.components.button.WalletOutlinedButton
import com.luntikius.wallet.designsystem.components.feedback.WalletCircularProgressIndicator
import com.luntikius.wallet.designsystem.foundation.spacing.spacing
import com.luntikius.wallet.ui.components.pass.custom.CustomPassCardFront
import com.luntikius.wallet.ui.components.pass.pkpass.PassCardFront
import com.luntikius.wallet.ui.viewmodel.PassViewModel
import com.luntikius.wallet.ui.viewmodel.PreviewStatus

/**
 * Preview screen that displays a pass card before adding it to the wallet.
 * Shows the card at expanded size with "Cancel" and "Add" buttons.
 */
@Composable
fun PassPreviewScreen(
    viewModel: PassViewModel,
    onAdd: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val previewPass by viewModel.previewPass.collectAsState()
    val previewStatus by viewModel.previewStatus.collectAsState()

    // Handle back button press
    BackHandler(enabled = previewStatus !is PreviewStatus.Loading) {
        onCancel()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        when (previewStatus) {
            is PreviewStatus.Loading -> {
                // Loading state
                WalletCircularProgressIndicator(
                    modifier = Modifier.padding(MaterialTheme.spacing.mediumLarge),
                )
            }

            is PreviewStatus.Ready -> {
                // Show preview with card and buttons
                previewPass?.let { pass ->
                    val passData = remember(pass) {
                        pass.getPassData()
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .widthIn(max = 600.dp)
                            .padding(vertical = MaterialTheme.spacing.extraLarge),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Title
                        Row(
                            modifier = Modifier.padding(bottom = MaterialTheme.spacing.mediumLarge),
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            Text(
                                text = "Add to ",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            AppLogo(color = MaterialTheme.colorScheme.onSurface)
                        }

                        // Card preview
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.7f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.Transparent,
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                            border = null,
                        ) {
                            when (passData) {
                                is PassData.PKPass -> {
                                    PassCardFront(pass = pass, pkPassJson = passData.pkPassJson)
                                }

                                is PassData.Custom -> {
                                    CustomPassCardFront(pass = pass, customPassJson = passData.customPassJson)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraLarge))

                        // Action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                        ) {
                            // Cancel button
                            WalletOutlinedButton(
                                onClick = onCancel,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                            ) {
                                Text(
                                    text = "Cancel",
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }

                            // Add button - vibrant green
                            WalletFilledButton(
                                onClick = onAdd,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                            ) {
                                Text(
                                    text = "Add",
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    }
                }
            }

            is PreviewStatus.Error -> {
                // Error state
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "Failed to load pass",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

                    Text(
                        text = (previewStatus as PreviewStatus.Error).message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraLarge))

                    WalletOutlinedButton(
                        onClick = onCancel,
                    ) {
                        Text(
                            text = "Close",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }

            is PreviewStatus.Idle -> {
                // Should not happen, but handle gracefully
                WalletCircularProgressIndicator(
                    modifier = Modifier.padding(MaterialTheme.spacing.mediumLarge),
                )
            }
        }
    }
}

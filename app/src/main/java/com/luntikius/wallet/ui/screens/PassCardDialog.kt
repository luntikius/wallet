package com.luntikius.wallet.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.luntikius.wallet.data.model.BarcodeFormatType
import com.luntikius.wallet.data.model.Pass
import com.luntikius.wallet.data.model.PassData
import com.luntikius.wallet.data.model.getPassData
import com.luntikius.wallet.data.parser.pkpass.PKPassJson
import com.luntikius.wallet.ui.components.BarcodeDisplay
import com.luntikius.wallet.ui.components.HtmlText
import com.luntikius.wallet.ui.components.pass.custom.CustomPassCardBack
import com.luntikius.wallet.ui.components.pass.custom.CustomPassCardFront
import com.luntikius.wallet.ui.utils.ensureContrast
import com.luntikius.wallet.ui.utils.parseColor
import com.luntikius.wallet.ui.utils.stripHtml
import com.luntikius.wallet.ui.viewmodel.PassViewModel
import kotlinx.coroutines.launch
import java.io.File

/**
 * Dialog that displays a pass card with flip animation.
 * Front shows: icon, logo, barcode
 * Back shows: additional fields
 */
@Composable
fun PassCardDialog(passId: String, viewModel: PassViewModel, onDismiss: () -> Unit) {
    var pass by remember { mutableStateOf<Pass?>(null) }
    var passData by remember { mutableStateOf<PassData?>(null) }

    LaunchedEffect(passId) {
        pass = viewModel.getPassById(passId)
        pass?.let { p ->
            passData = p.getPassData()
        }
    }

    pass?.let { currentPass ->
        // Custom cards use smaller height since they have less content (matching preview at 280dp)
        val isCustomPass = passData is PassData.Custom
        val heightFraction = if (isCustomPass) 0.45f else 0.85f

        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .widthIn(max = 600.dp)
                        .fillMaxHeight(heightFraction)
                        .windowInsetsPadding(WindowInsets.systemBars)
                        .clickable(enabled = false) { /* Prevent dismissing when clicking */ },
                ) {
                    // Flippable card
                    FlippablePassCard(
                        pass = currentPass,
                        passData = passData,
                        viewModel = viewModel,
                        onDismiss = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                    )
                }
            }
        }
    }
}

/**
 * A card that can be flipped to show front and back.
 */
@Composable
fun FlippablePassCard(
    pass: Pass,
    passData: PassData?,
    viewModel: PassViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFlipped by remember { mutableStateOf(false) }

    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "card flip",
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 12f * density
                },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = null,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (rotation <= 90f) {
                    // Front side
                    passData?.let { data ->
                        when (data) {
                            is PassData.PKPass -> {
                                PassCardFront(pass = pass, pkPassJson = data.pkPassJson)
                            }

                            is PassData.Custom -> {
                                CustomPassCardFront(pass = pass, customPassJson = data.customPassJson)
                            }
                        }
                    }
                } else {
                    // Back side (flip horizontally to correct orientation)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { rotationY = 180f },
                    ) {
                        passData?.let { data ->
                            when (data) {
                                is PassData.PKPass -> {
                                    PassCardBack(
                                        pass = pass,
                                        pkPassJson = data.pkPassJson,
                                        viewModel = viewModel,
                                        onDismiss = onDismiss,
                                    )
                                }

                                is PassData.Custom -> {
                                    CustomPassCardBack(
                                        pass = pass,
                                        viewModel = viewModel,
                                        onDismiss = onDismiss,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.3f))

        // Flip button
        OutlinedButton(
            onClick = { isFlipped = !isFlipped },
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(40.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = Color.White,
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = "Flip card",
                modifier = Modifier.size(16.dp),
                tint = Color.White,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Flip",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Close button
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(0.5f),
        ) {
            Text(
                text = "Close",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * Front side of the pass card.
 */
@Composable
fun PassCardFront(pass: Pass, pkPassJson: PKPassJson?, modifier: Modifier = Modifier) {
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor, RoundedCornerShape(16.dp)),
    ) {
        // 1. HEADER ROW: Logo (left) + Header Fields (right)
        val structure = pkPassJson?.let { json ->
            json.boardingPass ?: json.eventTicket ?: json.coupon ?: json.storeCard ?: json.generic
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Top,
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

            // Header fields (right-aligned in a row)
            structure?.headerFields?.let { headerFields ->
                if (headerFields.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        headerFields.forEach { field ->
                            Column(
                                horizontalAlignment = Alignment.End,
                            ) {
                                Text(
                                    text = stripHtml(field.label ?: ""),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = textColor.copy(alpha = 0.6f),
                                    textAlign = TextAlign.End,
                                )
                                Text(
                                    text = stripHtml(field.value?.toString() ?: ""),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textColor,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.End,
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. STRIP IMAGE (if present)
        pass.stripPath?.let { stripPath ->
            val stripFile = File(stripPath)
            if (stripFile.exists()) {
                val bitmap = BitmapFactory.decodeFile(stripFile.absolutePath)
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Strip image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        contentScale = ContentScale.FillWidth,
                    )
                }
            }
        }

        // 3. INFO SECTION: Primary and Secondary fields
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            // Primary fields
            structure?.primaryFields?.let { fields ->
                if (fields.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = when {
                            fields.size == 1 -> Arrangement.Start
                            fields.size == 2 -> Arrangement.SpaceBetween
                            else -> Arrangement.SpaceEvenly
                        },
                    ) {
                        fields.forEachIndexed { index, field ->
                            Column(
                                horizontalAlignment = Alignment.Start,
                                modifier = when {
                                    fields.size > 2 -> Modifier.weight(1f)
                                    else -> Modifier
                                },
                            ) {
                                Text(
                                    text = stripHtml(field.label ?: field.key),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = textColor.copy(alpha = 0.6f),
                                )
                                Text(
                                    text = stripHtml(field.value?.toString() ?: ""),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = textColor,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Secondary fields
            structure?.secondaryFields?.let { fields ->
                if (fields.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = when {
                            fields.size == 1 -> Arrangement.Start
                            fields.size == 2 -> Arrangement.SpaceBetween
                            else -> Arrangement.SpaceEvenly
                        },
                    ) {
                        fields.forEachIndexed { index, field ->
                            Column(
                                modifier = when {
                                    fields.size > 2 -> Modifier.weight(1f)
                                    else -> Modifier
                                },
                            ) {
                                Text(
                                    text = stripHtml(field.label ?: field.key),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = textColor.copy(alpha = 0.6f),
                                )
                                Text(
                                    text = stripHtml(field.value?.toString() ?: ""),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textColor,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Flexible spacer to push barcode to center of remaining space
        Spacer(modifier = Modifier.weight(1f))

        // 4. BARCODE SECTION - Centered in bottom portion
        pkPassJson?.let { json ->
            val barcode = json.barcodes?.firstOrNull() ?: json.barcode
            barcode?.let { barcodeData ->
                BarcodeFormatType.fromPKPassFormat(barcodeData.format)?.let { format ->
                    BarcodeDisplay(
                        barcodeValue = barcodeData.message,
                        barcodeFormat = format,
                        textColor = textColor,
                        altText = barcodeData.altText,
                    )
                }
            }
        }

        // Flexible spacer to balance and center barcode
        Spacer(modifier = Modifier.weight(1f))
    }
}

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

/**
 * Info block for displaying back field with HTML content.
 */
@Composable
private fun InfoBlock(title: String, htmlContent: String, textColor: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            // Block title/label (only if not blank)
            if (title.isNotBlank()) {
                SelectionContainer {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Black.copy(alpha = 0.6f),
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // HTML content with clickable links - selectable
            SelectionContainer {
                HtmlText(
                    html = htmlContent,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black,
                )
            }
        }
    }
}

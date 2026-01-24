package com.luntikius.wallet.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.gson.Gson
import com.luntikius.wallet.data.model.Pass
import com.luntikius.wallet.data.parser.pkpass.PKField
import com.luntikius.wallet.data.parser.pkpass.PKPassJson
import com.luntikius.wallet.ui.components.HtmlText
import com.luntikius.wallet.ui.utils.ensureContrast
import com.luntikius.wallet.ui.utils.parseColor
import com.luntikius.wallet.ui.utils.stripHtml
import com.luntikius.wallet.ui.utils.generateBarcodeBitmap
import com.luntikius.wallet.ui.utils.pkPassFormatToZXingFormat
import com.luntikius.wallet.ui.viewmodel.PassViewModel
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import java.io.File

/**
 * Dialog that displays a pass card with flip animation.
 * Front shows: icon, logo, barcode
 * Back shows: additional fields
 */
@Composable
fun PassCardDialog(
    passId: String,
    viewModel: PassViewModel,
    onDismiss: () -> Unit
) {
    var pass by remember { mutableStateOf<Pass?>(null) }
    var pkPassJson by remember { mutableStateOf<PKPassJson?>(null) }

    LaunchedEffect(passId) {
        pass = viewModel.getPassById(passId)
        pass?.let { p ->
            pkPassJson = Gson().fromJson(p.rawData, PKPassJson::class.java)
        }
    }

    pass?.let { currentPass ->
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .fillMaxHeight(0.85f)
                        .windowInsetsPadding(WindowInsets.systemBars)
                        .clickable(enabled = false) { /* Prevent dismissing when clicking */ }
                ) {
                    // Flippable card
                    FlippablePassCard(
                        pass = currentPass,
                        pkPassJson = pkPassJson,
                        viewModel = viewModel,
                        onDismiss = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
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
    pkPassJson: PKPassJson?,
    viewModel: PassViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFlipped by remember { mutableStateOf(false) }

    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "card flip"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
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
                containerColor = Color.Transparent
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = null
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (rotation <= 90f) {
                    // Front side
                    PassCardFront(pass = pass, pkPassJson = pkPassJson)
                } else {
                    // Back side (flip horizontally to correct orientation)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { rotationY = 180f }
                    ) {
                        PassCardBack(
                            pass = pass,
                            pkPassJson = pkPassJson,
                            viewModel = viewModel,
                            onDismiss = onDismiss
                        )
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
                contentColor = Color.White
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.7f))
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = "Flip card",
                modifier = Modifier.size(16.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Flip",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Close button
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(0.5f)
        ) {
            Text(
                text = "Close",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * Front side of the pass card.
 */
@Composable
fun PassCardFront(
    pass: Pass,
    pkPassJson: PKPassJson?,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = isSystemInDarkTheme()
    val backgroundColor = pass.backgroundColor?.let { parseColor(it) }
        ?: MaterialTheme.colorScheme.surface

    val textColor = ensureContrast(
        foregroundColor = pass.foregroundColor?.let { parseColor(it) },
        backgroundColor = backgroundColor,
        isDarkTheme = isDarkTheme,
        lightFallback = MaterialTheme.colorScheme.onSurface,
        darkFallback = MaterialTheme.colorScheme.onSurface
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor, RoundedCornerShape(16.dp))
    ) {
        // 1. HEADER ROW: Logo (left) + Header Fields (right)
        val structure = pkPassJson?.let { json ->
            json.boardingPass ?: json.eventTicket ?: json.coupon ?: json.storeCard ?: json.generic
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Top
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
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Header fields (right-aligned in a row)
            structure?.headerFields?.let { headerFields ->
                if (headerFields.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        headerFields.forEach { field ->
                            Column(
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = stripHtml(field.label ?: ""),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = textColor.copy(alpha = 0.6f),
                                    textAlign = TextAlign.End
                                )
                                Text(
                                    text = stripHtml(field.value?.toString() ?: ""),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textColor,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.End
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
                        contentScale = ContentScale.FillWidth
                    )
                }
            }
        }

        // 3. INFO SECTION: Primary and Secondary fields
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
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
                        }
                    ) {
                        fields.forEachIndexed { index, field ->
                            Column(
                                horizontalAlignment = Alignment.Start,
                                modifier = when {
                                    fields.size > 2 -> Modifier.weight(1f)
                                    else -> Modifier
                                }
                            ) {
                                Text(
                                    text = stripHtml(field.label ?: field.key),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = textColor.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = stripHtml(field.value?.toString() ?: ""),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = textColor,
                                    fontWeight = FontWeight.Bold
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
                        }
                    ) {
                        fields.forEachIndexed { index, field ->
                            Column(
                                modifier = when {
                                    fields.size > 2 -> Modifier.weight(1f)
                                    else -> Modifier
                                }
                            ) {
                                Text(
                                    text = stripHtml(field.label ?: field.key),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = textColor.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = stripHtml(field.value?.toString() ?: ""),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textColor,
                                    fontWeight = FontWeight.Medium
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
                val barcodeFormat = pkPassFormatToZXingFormat(barcodeData.format)
                val barcodeBitmap = remember(barcodeData.message, barcodeData.format) {
                    barcodeFormat?.let {
                        generateBarcodeBitmap(
                            message = barcodeData.message,
                            format = it,
                            width = if (barcodeData.format == "PKBarcodeFormatQR") 600 else 800,
                            height = if (barcodeData.format == "PKBarcodeFormatQR") 600 else 400
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    barcodeBitmap?.let { bitmap ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (barcodeData.format == "PKBarcodeFormatQR") 140.dp else 100.dp),
                            color = Color.White,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Barcode",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    barcodeData.altText?.let { alt ->
                        if (alt.isNotBlank()) {
                            Text(
                                text = alt,
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
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
    modifier: Modifier = Modifier
) {
    val isDarkTheme = isSystemInDarkTheme()
    val backgroundColor = pass.backgroundColor?.let { parseColor(it) }
        ?: MaterialTheme.colorScheme.surface

    val textColor = ensureContrast(
        foregroundColor = pass.foregroundColor?.let { parseColor(it) },
        backgroundColor = backgroundColor,
        isDarkTheme = isDarkTheme,
        lightFallback = MaterialTheme.colorScheme.onSurface,
        darkFallback = MaterialTheme.colorScheme.onSurface
    )

    var showOptionsMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor, RoundedCornerShape(16.dp))
    ) {
        // 1. HEADER ROW: Logo (left) + Options Menu (right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
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
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Options menu
            Box {
                IconButton(onClick = { showOptionsMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = textColor
                    )
                }

                DropdownMenu(
                    expanded = showOptionsMenu,
                    onDismissRequest = { showOptionsMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        },
                        onClick = {
                            viewModel.deletePass(pass)
                            showOptionsMenu = false
                            onDismiss()
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Share") },
                        leadingIcon = {
                            Icon(Icons.Default.Share, contentDescription = null)
                        },
                        onClick = {
                            // TODO: Implement share functionality
                            showOptionsMenu = false
                        }
                    )
                }
            }
        }

        // 2. INFO BLOCKS: Scrollable content with back fields
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            pkPassJson?.let { json ->
                val structure = json.boardingPass ?: json.eventTicket
                    ?: json.coupon ?: json.storeCard ?: json.generic

                structure?.backFields?.let { fields ->
                    fields.forEach { field ->
                        val content = field.value?.toString() ?: ""
                        if (content.isNotBlank()) {
                            InfoBlock(
                                title = field.label ?: "",
                                htmlContent = content,
                                textColor = textColor
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Info block for displaying back field with HTML content.
 */
@Composable
private fun InfoBlock(
    title: String,
    htmlContent: String,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Block title/label (only if not blank)
            if (title.isNotBlank()) {
                SelectionContainer {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Black.copy(alpha = 0.6f),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // HTML content with clickable links - selectable
            SelectionContainer {
                HtmlText(
                    html = htmlContent,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )
            }
        }
    }
}

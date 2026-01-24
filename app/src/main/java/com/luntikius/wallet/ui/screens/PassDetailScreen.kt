package com.luntikius.wallet.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.luntikius.wallet.data.model.Pass
import com.luntikius.wallet.data.parser.pkpass.PKField
import com.luntikius.wallet.data.parser.pkpass.PKPassJson
import com.luntikius.wallet.ui.viewmodel.PassViewModel
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import java.io.File

/**
 * Detail screen for displaying a pass.
 */
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PassDetailScreen(
    passId: String,
    viewModel: PassViewModel,
    onBackClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier
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
        with(sharedTransitionScope) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(currentPass.organizationName) },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                            }
                        }
                    )
                },
                modifier = modifier
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header card with shared element
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .sharedElement(
                                rememberSharedContentState(key = "card-${currentPass.id}"),
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = currentPass.backgroundColor?.let { parseColor(it) }
                                ?: MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Icon
                            val iconFile = File(currentPass.iconPath)
                            if (iconFile.exists()) {
                                val bitmap = BitmapFactory.decodeFile(iconFile.absolutePath)
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(80.dp)
                                        .sharedElement(
                                            rememberSharedContentState(key = "icon-${currentPass.id}"),
                                            animatedVisibilityScope = animatedVisibilityScope
                                        ),
                                    contentScale = ContentScale.Fit
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = currentPass.organizationName,
                                style = MaterialTheme.typography.headlineSmall,
                                color = currentPass.foregroundColor?.let { parseColor(it) }
                                    ?: MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = currentPass.description,
                                style = MaterialTheme.typography.bodyLarge,
                                color = currentPass.foregroundColor?.let { parseColor(it) }
                                    ?: MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Pass fields
                    pkPassJson?.let { json ->
                        val structure = json.boardingPass ?: json.eventTicket
                            ?: json.coupon ?: json.storeCard ?: json.generic

                        structure?.let { s ->
                            // Primary fields
                            s.primaryFields?.let { fields ->
                                if (fields.isNotEmpty()) {
                                    FieldSection(
                                        title = "Primary",
                                        fields = fields,
                                        foregroundColor = currentPass.foregroundColor,
                                        labelColor = currentPass.labelColor
                                    )
                                }
                            }

                            // Secondary fields
                            s.secondaryFields?.let { fields ->
                                if (fields.isNotEmpty()) {
                                    FieldSection(
                                        title = "Details",
                                        fields = fields,
                                        foregroundColor = currentPass.foregroundColor,
                                        labelColor = currentPass.labelColor
                                    )
                                }
                            }

                            // Auxiliary fields
                            s.auxiliaryFields?.let { fields ->
                                if (fields.isNotEmpty()) {
                                    FieldSection(
                                        title = "Additional Information",
                                        fields = fields,
                                        foregroundColor = currentPass.foregroundColor,
                                        labelColor = currentPass.labelColor
                                    )
                                }
                            }

                            // Back fields
                            s.backFields?.let { fields ->
                                if (fields.isNotEmpty()) {
                                    FieldSection(
                                        title = "Back",
                                        fields = fields,
                                        foregroundColor = currentPass.foregroundColor,
                                        labelColor = currentPass.labelColor
                                    )
                                }
                            }
                        }

                        // Barcode
                        val barcode = json.barcodes?.firstOrNull() ?: json.barcode
                        barcode?.let {
                            BarcodeSection(
                                format = it.format,
                                message = it.message,
                                altText = it.altText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun FieldSection(
    title: String,
    fields: List<PKField>,
    foregroundColor: String?,
    labelColor: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        fields.forEach { field ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = field.label ?: field.key,
                    style = MaterialTheme.typography.bodySmall,
                    color = labelColor?.let { parseColor(it) }
                        ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = field.value?.toString() ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = foregroundColor?.let { parseColor(it) }
                        ?: MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun BarcodeSection(
    format: String,
    message: String,
    altText: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Barcode",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color.White)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            when (format) {
                "PKBarcodeFormatQR" -> {
                    val painter = rememberQrCodePainter(message)
                    Image(
                        painter = painter,
                        contentDescription = "QR Code",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    // For other formats, display the message as text for now
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Barcode Format: ${format.removePrefix("PKBarcodeFormat")}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        altText?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

private fun parseColor(colorString: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorString))
    } catch (e: Exception) {
        Color.Unspecified
    }
}

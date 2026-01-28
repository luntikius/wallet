package com.luntikius.wallet.ui.components.pass.pkpass

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.luntikius.wallet.data.model.BarcodeFormatType
import com.luntikius.wallet.data.model.Pass
import com.luntikius.wallet.data.parser.pkpass.PKPassJson
import com.luntikius.wallet.ui.components.BarcodeDisplay
import com.luntikius.wallet.ui.utils.rememberCardColors
import com.luntikius.wallet.ui.utils.stripHtml
import java.io.File

/**
 * Front side of the pass card.
 */
@Composable
fun PassCardFront(pass: Pass, pkPassJson: PKPassJson?, modifier: Modifier = Modifier) {
    val cardColors = rememberCardColors(pass)
    val backgroundColor = cardColors.background
    val textColor = cardColors.text

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

package com.luntikius.wallet.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.luntikius.wallet.data.model.BarcodeFormatType
import com.luntikius.wallet.ui.utils.generateBarcodeBitmap

/**
 * Common barcode display component used in both preview and detail views.
 * Matches PKPass card styling from PassCardDialog.kt.
 */
@Composable
fun BarcodeDisplay(
    barcodeValue: String,
    barcodeFormat: BarcodeFormatType,
    textColor: Color,
    modifier: Modifier = Modifier,
    altText: String? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        val barcodeBitmap = remember(barcodeValue, barcodeFormat) {
            generateBarcodeBitmap(
                message = barcodeValue,
                format = barcodeFormat.zxingFormat,
                width = if (barcodeFormat.isQRCode) 600 else 800,
                height = if (barcodeFormat.isQRCode) 600 else 400,
            )
        }

        barcodeBitmap?.let { bitmap ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (barcodeFormat.isQRCode) 180.dp else 130.dp),
                color = Color.White,
                shape = RoundedCornerShape(12.dp),
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Barcode",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    contentScale = ContentScale.Fit,
                )
            }
        } ?: run {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (barcodeFormat.isQRCode) 180.dp else 130.dp)
                    .background(
                        color = textColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Barcode unavailable",
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.5f),
                )
            }
        }

        // Display altText or barcode value below
        val displayText = altText?.takeIf { it.isNotBlank() } ?: barcodeValue
        if (displayText.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = displayText,
                style = MaterialTheme.typography.bodySmall,
                color = textColor.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

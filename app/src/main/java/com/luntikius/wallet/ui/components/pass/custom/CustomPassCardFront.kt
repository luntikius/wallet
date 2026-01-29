package com.luntikius.wallet.ui.components.pass.custom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luntikius.wallet.data.model.BarcodeFormatType
import com.luntikius.wallet.data.model.CustomPassJson
import com.luntikius.wallet.data.model.Pass
import com.luntikius.wallet.designsystem.components.input.WalletTransparentTextField
import com.luntikius.wallet.designsystem.foundation.color.createCustomPassGradient
import com.luntikius.wallet.designsystem.foundation.spacing.spacing
import com.luntikius.wallet.ui.components.BarcodeDisplay
import com.luntikius.wallet.ui.utils.IconMapper
import com.luntikius.wallet.ui.utils.rememberCardColors

/**
 * Front side of a custom pass card.
 * Displays: Icon + Card name (top left in row) + Barcode (bottom)
 */
@Composable
fun CustomPassCardFront(pass: Pass, customPassJson: CustomPassJson, modifier: Modifier = Modifier) {
    val cardColors = rememberCardColors(pass)
    val backgroundColor = cardColors.background
    val foregroundColor = cardColors.foreground ?: MaterialTheme.colorScheme.onSurface
    val textColor = cardColors.text

    val icon = IconMapper.getIconByName(customPassJson.iconName)

    CustomPassCard(
        cardName = pass.organizationName,
        icon = icon,
        backgroundColor = backgroundColor,
        foregroundColor = foregroundColor,
        textColor = textColor,
        barcodeValue = customPassJson.barcodeValue,
        barcodeFormat = customPassJson.barcodeFormat,
        isEditable = false,
        onCardNameChange = {},
        modifier = modifier,
    )
}

/**
 * Shared custom pass card component that can be used in both expansion and builder views.
 * Supports both editable (TextField) and non-editable (Text) card name.
 */
@Suppress("LongParameterList")
@Composable
fun CustomPassCard(
    cardName: String,
    icon: ImageVector,
    backgroundColor: Color,
    foregroundColor: Color,
    textColor: Color,
    barcodeValue: String,
    barcodeFormat: String,
    isEditable: Boolean,
    onCardNameChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gradient = createCustomPassGradient(backgroundColor, foregroundColor)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(gradient, RoundedCornerShape(16.dp)),
    ) {
        // 1. HEADER ROW: Icon + Card name (left aligned)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = MaterialTheme.spacing.mediumLarge,
                    end = MaterialTheme.spacing.large,
                    top = MaterialTheme.spacing.medium,
                    bottom = MaterialTheme.spacing.medium,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = textColor,
            )
            Spacer(modifier = Modifier.size(MaterialTheme.spacing.extraSmall))

            if (isEditable) {
                WalletTransparentTextField(
                    value = cardName,
                    onValueChange = onCardNameChange,
                    placeholder = {
                        Text(
                            text = "Enter Card Name",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = textColor.copy(alpha = 0.6f),
                        )
                    },
                    textColor = textColor,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            } else {
                Text(
                    text = cardName,
                    style = MaterialTheme.typography.titleLarge,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        // Flexible spacer to push barcode to center of remaining space
        Spacer(modifier = Modifier.weight(1f))

        // 2. BARCODE SECTION - Centered in bottom portion
        BarcodeDisplay(
            barcodeValue = barcodeValue,
            barcodeFormat = BarcodeFormatType.fromName(barcodeFormat),
            textColor = textColor,
        )

        // Flexible spacer to balance and center barcode
        Spacer(modifier = Modifier.weight(1f))
    }
}

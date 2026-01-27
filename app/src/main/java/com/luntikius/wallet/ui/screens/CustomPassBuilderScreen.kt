package com.luntikius.wallet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luntikius.wallet.data.builder.CustomPassBuilder
import com.luntikius.wallet.ui.components.custom.CustomPassCard
import com.luntikius.wallet.ui.theme.CustomPassColors
import com.luntikius.wallet.ui.utils.IconMapper
import com.luntikius.wallet.ui.utils.createCustomPassGradient
import com.luntikius.wallet.ui.utils.ensureContrast
import com.luntikius.wallet.ui.viewmodel.PassViewModel

/**
 * Screen for building custom passes from scanned barcodes.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CustomPassBuilderScreen(
    barcodeValue: String,
    barcodeFormat: String,
    viewModel: PassViewModel,
    onCancel: () -> Unit,
    onPassCreated: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var cardName by remember { mutableStateOf("") }
    var selectedIconIndex by remember { mutableStateOf(0) }
    var selectedColorIndex by remember { mutableStateOf(0) }
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Card") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
            )
        },
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState),
        ) {
            // Preview card at the top with editable name
            val (_, selectedIcon) = IconMapper.availableIcons[selectedIconIndex]
            val backgroundColor = CustomPassColors[selectedColorIndex].first
            val foregroundColor = CustomPassColors[selectedColorIndex].second
            val isDarkTheme = isSystemInDarkTheme()
            val textColor = ensureContrast(
                foregroundColor = foregroundColor,
                backgroundColor = backgroundColor,
                isDarkTheme = isDarkTheme,
                lightFallback = MaterialTheme.colorScheme.onSurface,
                darkFallback = MaterialTheme.colorScheme.onSurface,
            )

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                CustomPassCard(
                    cardName = cardName,
                    onCardNameChange = { cardName = it },
                    icon = selectedIcon,
                    backgroundColor = backgroundColor,
                    foregroundColor = foregroundColor,
                    textColor = textColor,
                    barcodeValue = barcodeValue,
                    barcodeFormat = barcodeFormat,
                    isEditable = true,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .aspectRatio(1.25f)
                        .padding(vertical = 12.dp)
                        .shadow(8.dp, RoundedCornerShape(12.dp)),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Icon picker
            Text(
                text = "Icon",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            IconPicker(
                icons = IconMapper.availableIcons,
                selectedIndex = selectedIconIndex,
                onIconSelected = { selectedIconIndex = it },
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Color picker
            Text(
                text = "Color",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            ColorPicker(
                colorPairs = CustomPassColors,
                selectedIndex = selectedColorIndex,
                onColorSelected = { selectedColorIndex = it },
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        if (cardName.isNotBlank()) {
                            val selectedColor = CustomPassColors[selectedColorIndex]
                            val (iconName, _) = IconMapper.availableIcons[selectedIconIndex]
                            val pass = CustomPassBuilder.createCustomPass(
                                cardName = cardName,
                                barcodeValue = barcodeValue,
                                barcodeFormat = barcodeFormat,
                                iconName = iconName,
                                backgroundColor = String.format(
                                    "#%06X",
                                    0xFFFFFF and selectedColor.first.toArgb(),
                                ),
                                foregroundColor = String.format(
                                    "#%06X",
                                    0xFFFFFF and selectedColor.second.toArgb(),
                                ),
                            )
                            viewModel.createCustomPass(pass)
                            onPassCreated()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = cardName.isNotBlank(),
                ) {
                    Text("Add to Wallet")
                }
            }
        }
    }
}

/**
 * Color picker with circular swatches in a horizontally scrolling row.
 */
@Composable
private fun ColorPicker(
    colorPairs: List<Pair<Color, Color>>,
    selectedIndex: Int,
    onColorSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(modifier = Modifier.width(4.dp))
        colorPairs.forEachIndexed { index, (backgroundColor, foregroundColor) ->
            ColorCircle(
                backgroundColor = backgroundColor,
                foregroundColor = foregroundColor,
                isSelected = index == selectedIndex,
                onClick = { onColorSelected(index) },
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
    }
}

/**
 * Individual circular color swatch with gradient.
 */
@Composable
private fun ColorCircle(
    backgroundColor: Color,
    foregroundColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gradient = createCustomPassGradient(backgroundColor, foregroundColor)

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(gradient)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                    )
                } else {
                    Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        shape = CircleShape,
                    )
                },
            )
            .clickable(onClick = onClick),
    )
}

/**
 * Icon picker with circular icon buttons in a horizontally scrolling row.
 */
@Composable
private fun IconPicker(
    icons: List<Pair<String, ImageVector>>,
    selectedIndex: Int,
    onIconSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(modifier = Modifier.width(4.dp))
        icons.forEachIndexed { index, (name, icon) ->
            IconCircle(
                icon = icon,
                isSelected = index == selectedIndex,
                onClick = { onIconSelected(index) },
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
    }
}

/**
 * Individual circular icon button.
 */
@Composable
private fun IconCircle(icon: ImageVector, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                    )
                } else {
                    Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        shape = CircleShape,
                    )
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

package com.luntikius.wallet.ui.components.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.luntikius.wallet.ui.utils.createCustomPassGradient

/**
 * Color picker with circular swatches in a horizontally scrolling row.
 */
@Composable
fun ColorPicker(
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
fun ColorCircle(
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

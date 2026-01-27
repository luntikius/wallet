package com.luntikius.wallet.ui.components.pass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.luntikius.wallet.data.model.Pass
import com.luntikius.wallet.data.model.PassData
import com.luntikius.wallet.ui.components.pass.custom.CustomPassCardBack
import com.luntikius.wallet.ui.components.pass.custom.CustomPassCardFront
import com.luntikius.wallet.ui.components.pass.pkpass.PassCardBack
import com.luntikius.wallet.ui.components.pass.pkpass.PassCardFront
import com.luntikius.wallet.ui.viewmodel.PassViewModel

/**
 * Renders the appropriate side of a pass card based on rotation angle.
 *
 * Handles the logic for determining front vs. back side and dispatching to
 * the correct card component (PKPass or Custom). Used by PassCardExpansion
 * to centralize card rendering logic.
 *
 * @param rotation Current rotation angle in degrees (0-360)
 * @param pass The pass to render
 * @param passData The parsed pass data (PKPass or Custom)
 * @param viewModel The ViewModel for pass operations
 * @param onDismiss Callback to dismiss the parent screen
 * @param modifier Optional modifier for the root Box
 */
@Composable
fun RenderCardSide(
    rotation: Float,
    pass: Pass,
    passData: PassData?,
    viewModel: PassViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Normalize rotation to 0-360 range and determine front vs back
        val normalizedRotation = ((rotation % 360) + 360) % 360
        val showFront = normalizedRotation < 90f || normalizedRotation >= 270f

        passData?.let { data ->
            if (showFront) {
                // Front side
                when (data) {
                    is PassData.PKPass -> {
                        PassCardFront(pass = pass, pkPassJson = data.pkPassJson)
                    }

                    is PassData.Custom -> {
                        CustomPassCardFront(pass = pass, customPassJson = data.customPassJson)
                    }
                }
            } else {
                // Back side (flip horizontally to correct orientation)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationY = 180f },
                ) {
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

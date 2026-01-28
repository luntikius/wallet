package com.luntikius.wallet.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import com.luntikius.wallet.data.model.Pass
import com.luntikius.wallet.data.model.PassData
import com.luntikius.wallet.data.model.RefreshStatus
import com.luntikius.wallet.data.model.getPassData
import com.luntikius.wallet.ui.animation.AnimationConstants
import com.luntikius.wallet.ui.components.pass.custom.CustomPassCardBack
import com.luntikius.wallet.ui.components.pass.custom.CustomPassCardFront
import com.luntikius.wallet.ui.components.pass.pkpass.PassCardBack
import com.luntikius.wallet.ui.components.pass.pkpass.PassCardFront
import com.luntikius.wallet.ui.viewmodel.PassViewModel
import kotlin.math.abs
import kotlinx.coroutines.launch

/**
 * Overlay that animates a pass card expanding from its tile position to full screen.
 *
 * Features:
 * - Animates card expansion from tile position to center screen
 * - Animated scrim background
 * - Slide-up button animation
 * - Swipe-to-flip gesture
 * - Smooth dismissal animation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassCardExpansion(
    passId: String,
    tilePosition: IntRect?,
    viewModel: PassViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onTileVisibilityChange: (visible: Boolean) -> Unit = {},
) {
    var pass by remember { mutableStateOf<Pass?>(null) }
    var passData by remember { mutableStateOf<PassData?>(null) }
    var isDismissing by remember { mutableStateOf(false) }
    val refreshStatus by viewModel.refreshStatus.collectAsState()

    // Animation states
    val scale = remember { Animatable(0f) }
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val scrimAlpha = remember { Animatable(0f) }
    val buttonSlide = remember { Animatable(1f) } // 1 = off screen, 0 = visible
    val contentAlpha = remember { Animatable(0f) } // Card content fade-in

    // Card flip rotation - tracks cumulative rotation in degrees
    var targetRotation by remember { mutableFloatStateOf(0f) }
    val rotation by animateFloatAsState(
        targetValue = targetRotation,
        animationSpec = tween(
            durationMillis = AnimationConstants.FLIP_DURATION_MS,
            easing = FastOutSlowInEasing,
        ),
        label = "card flip",
    )

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }

    val coroutineScope = rememberCoroutineScope()

    // Load pass data
    LaunchedEffect(passId) {
        pass = viewModel.getPassById(passId)
        pass?.let { p ->
            passData = p.getPassData()
        }
    }

    // Two-phase expansion animation on first composition
    LaunchedEffect(Unit) {
        if (tilePosition != null) {
            // Calculate scale and position
            val targetCardWidth = screenWidth * 0.9f
            val tileScale = tilePosition.width.toFloat() / targetCardWidth
            val targetCenterX = screenWidth / 2f
            val targetCenterY = screenHeight / 2f
            val tileCenterX = tilePosition.left + tilePosition.width / 2f
            val tileCenterY = tilePosition.top + tilePosition.height / 2f

            // PHASE 1: Fade in card at tile size (100ms)
            // Set initial state: card at tile position, scaled to tile size, invisible
            scale.snapTo(tileScale)
            offsetX.snapTo(tileCenterX - targetCenterX)
            offsetY.snapTo(tileCenterY - targetCenterY)
            contentAlpha.snapTo(0f)
            scrimAlpha.snapTo(0f)
            buttonSlide.snapTo(1f)

            // Fade in the full card content at tile size
            launch {
                contentAlpha.animateTo(1f, AnimationConstants.phaseOneCardFadeIn)
            }

            // Wait for phase 1 to complete, then delay before phase 2
            kotlinx.coroutines.delay(
                (AnimationConstants.PHASE_1_DURATION_MS + AnimationConstants.INTER_PHASE_DELAY_MS).toLong(),
            )

            // Hide tile before Phase 2 starts
            onTileVisibilityChange(false)

            // PHASE 2: Scale up, add scrim, show buttons (300ms)
            launch {
                scale.animateTo(1f, AnimationConstants.phaseTwoScale)
            }
            launch {
                offsetX.animateTo(0f, AnimationConstants.phaseTwoScale)
            }
            launch {
                offsetY.animateTo(0f, AnimationConstants.phaseTwoScale)
            }
            launch {
                scrimAlpha.animateTo(0.6f, AnimationConstants.phaseTwoScrim)
            }
            launch {
                buttonSlide.animateTo(0f, AnimationConstants.phaseTwoButtons)
            }
        } else {
            // No tile position, just fade in instantly
            scale.snapTo(1f)
            offsetX.snapTo(0f)
            offsetY.snapTo(0f)
            scrimAlpha.snapTo(0.6f)
            buttonSlide.snapTo(0f)
            contentAlpha.snapTo(1f)
        }
    }

    // Dismissal handler
    fun dismiss() {
        if (isDismissing) return
        isDismissing = true

        coroutineScope.launch {
            if (tilePosition != null) {
                val targetCardWidth = screenWidth * 0.9f

                val targetScale = tilePosition.width.toFloat() / targetCardWidth
                val targetCenterX = screenWidth / 2f
                val targetCenterY = screenHeight / 2f

                val targetOffsetX = (tilePosition.left + tilePosition.width / 2f) - targetCenterX
                val targetOffsetY = (tilePosition.top + tilePosition.height / 2f) - targetCenterY

                // Two-phase dismissal (mirrors expansion)
                // Phase 1: Scale down to tile size (250ms) with content visible
                launch {
                    scale.animateTo(targetScale, AnimationConstants.dismissalPhaseOneScale)
                }
                launch {
                    offsetX.animateTo(targetOffsetX, AnimationConstants.dismissalPhaseOneScale)
                }
                launch {
                    offsetY.animateTo(targetOffsetY, AnimationConstants.dismissalPhaseOneScale)
                }
                launch {
                    scrimAlpha.animateTo(0f, AnimationConstants.dismissalPhaseOneScale)
                }
                launch {
                    buttonSlide.animateTo(
                        1f,
                        tween(
                            AnimationConstants.DISMISSAL_PHASE_1_DURATION_MS - 50,
                            easing = FastOutSlowInEasing,
                        ),
                    )
                }

                // Wait for Phase 1 to complete, then delay before Phase 2
                launch {
                    kotlinx.coroutines.delay(
                        (
                            AnimationConstants.DISMISSAL_PHASE_1_DURATION_MS +
                                AnimationConstants.INTER_PHASE_DELAY_MS
                            ).toLong(),
                    )

                    // Show tile at start of Phase 2
                    onTileVisibilityChange(true)

                    // Phase 2: Fade out content at tile size (100ms)
                    contentAlpha.animateTo(0f, AnimationConstants.dismissalPhaseTwoFade)
                }

                // Wait for all animations to complete before calling onDismiss
                launch {
                    kotlinx.coroutines.delay(AnimationConstants.DISMISSAL_TOTAL_MS.toLong())
                    onDismiss()
                }
            } else {
                // Just fade out (no tile position)
                launch {
                    scrimAlpha.animateTo(0f, AnimationConstants.dismissalPhaseOneScale)
                }
                launch {
                    kotlinx.coroutines.delay(
                        (
                            AnimationConstants.DISMISSAL_PHASE_1_DURATION_MS +
                                AnimationConstants.INTER_PHASE_DELAY_MS
                            ).toLong(),
                    )
                    onTileVisibilityChange(true)
                    contentAlpha.animateTo(0f, AnimationConstants.dismissalPhaseTwoFade)
                }
                launch {
                    kotlinx.coroutines.delay(AnimationConstants.DISMISSAL_TOTAL_MS.toLong())
                    onDismiss()
                }
            }
        }
    }

    // Handle back button
    BackHandler(enabled = !isDismissing) {
        dismiss()
    }

    pass?.let { currentPass ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha.value))
                .clickable(
                    onClick = { dismiss() },
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ),
            contentAlignment = Alignment.Center,
        ) {
            // Pull-to-refresh wrapping the card
            PullToRefreshBox(
                isRefreshing =
                refreshStatus is RefreshStatus.Loading &&
                    (refreshStatus as? RefreshStatus.Loading)?.passId == passId,
                onRefresh = { viewModel.refreshPass(passId) },
                modifier = Modifier.fillMaxSize(),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    passData?.let { data ->
                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = scale.value
                                    scaleY = scale.value
                                    translationX = offsetX.value
                                    translationY = offsetY.value
                                }
                                .pointerInput(targetRotation) {
                                    var dragOffset = 0f
                                    detectHorizontalDragGestures(
                                        onDragEnd = {
                                            if (abs(dragOffset) >
                                                AnimationConstants.SWIPE_THRESHOLD_DP * density.density
                                            ) {
                                                val normalizedRotation =
                                                    ((targetRotation % 360) + 360) % 360
                                                val isShowingFront =
                                                    normalizedRotation < 90f || normalizedRotation >= 270f

                                                val flipDirection =
                                                    if (dragOffset > 0) 180f else -180f
                                                targetRotation += if (isShowingFront) flipDirection else -flipDirection
                                            }
                                            dragOffset = 0f
                                        },
                                        onDragCancel = { dragOffset = 0f },
                                        onHorizontalDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffset += dragAmount
                                        },
                                    )
                                },
                        ) {
                            FlippableCardContent(
                                pass = currentPass,
                                passData = data,
                                rotation = rotation,
                                contentAlpha = contentAlpha.value,
                                density = density,
                                viewModel = viewModel,
                                onDismiss = { dismiss() },
                            )
                        }
                    }

                    // Buttons that slide up from bottom
                    CardControls(
                        buttonSlide = buttonSlide.value,
                        density = density,
                        onFlip = { targetRotation += 180f },
                        onClose = { dismiss() },
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }
    }
}

/**
 * Renders the flippable card content with front and back sides.
 */
@Composable
private fun FlippableCardContent(
    pass: Pass,
    passData: PassData,
    rotation: Float,
    contentAlpha: Float,
    density: androidx.compose.ui.unit.Density,
    viewModel: PassViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val aspectRatio = when (passData) {
        is PassData.PKPass -> 0.7f
        is PassData.Custom -> 1.25f
    }

    Box(
        modifier = modifier
            .fillMaxWidth(0.9f)
            .aspectRatio(aspectRatio),
    ) {
        // Flippable card with swipe gesture
        Card(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 12f * density.density
                }
                .clickable(enabled = false) { /* Prevent dismissing when clicking card */ },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = null,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = contentAlpha },
            ) {
                // Determine which side to show based on rotation
                val normalizedRotation = ((rotation % 360) + 360) % 360
                val showFront = normalizedRotation < 90f || normalizedRotation >= 270f

                if (showFront) {
                    // Front side
                    when (passData) {
                        is PassData.PKPass -> {
                            PassCardFront(pass = pass, pkPassJson = passData.pkPassJson)
                        }
                        is PassData.Custom -> {
                            CustomPassCardFront(
                                pass = pass,
                                customPassJson = passData.customPassJson,
                            )
                        }
                    }
                } else {
                    // Back side (flip horizontally to correct orientation)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { rotationY = 180f },
                    ) {
                        when (passData) {
                            is PassData.PKPass -> {
                                PassCardBack(
                                    pass = pass,
                                    pkPassJson = passData.pkPassJson,
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
}

/**
 * Renders the flip and close buttons that slide up from the bottom.
 */
@Composable
private fun CardControls(
    buttonSlide: Float,
    density: androidx.compose.ui.unit.Density,
    onFlip: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth(0.9f)
            .graphicsLayer {
                translationY = buttonSlide * 200f * density.density
                alpha = 1f - buttonSlide
            }
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Flip button
        OutlinedButton(
            onClick = onFlip,
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(40.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = Color.White,
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                Color.White.copy(alpha = 0.7f),
            ),
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
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(0.5f),
        ) {
            Text(
                text = "Close",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
            )
        }
    }
}

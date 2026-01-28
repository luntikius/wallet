package com.luntikius.wallet.ui.animation

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween

/**
 * Animation specifications and constants for the pass card expansion effect.
 *
 * Defines timing and easing for the multi-phase expansion and dismissal animations.
 */
object AnimationConstants {
    // Phase 1: Fade-in at tile size (card appears scaled down)
    const val PHASE_1_DURATION_MS = 50

    // Delay between phases
    const val INTER_PHASE_DELAY_MS = 16

    // Phase 2: Scale up to full size with scrim and buttons
    const val PHASE_2_DURATION_MS = 300

    // Total expansion time
    const val TOTAL_EXPANSION_MS = PHASE_1_DURATION_MS + INTER_PHASE_DELAY_MS + PHASE_2_DURATION_MS

    // Dismissal timing (mirrors expansion: scale then fade)
    const val DISMISSAL_PHASE_1_DURATION_MS = 250 // Scale down to tile size
    const val DISMISSAL_PHASE_2_DURATION_MS = 100 // Fade out at tile size
    const val DISMISSAL_TOTAL_MS =
        DISMISSAL_PHASE_1_DURATION_MS + INTER_PHASE_DELAY_MS + DISMISSAL_PHASE_2_DURATION_MS

    // Card flip
    const val FLIP_DURATION_MS = 600

    // Swipe threshold
    const val SWIPE_THRESHOLD_DP = 50

    // Animation specs
    val phaseOneCardFadeIn = tween<Float>(
        durationMillis = PHASE_1_DURATION_MS,
        easing = LinearEasing,
    )

    val phaseTwoScale = tween<Float>(
        durationMillis = PHASE_2_DURATION_MS,
        easing = FastOutSlowInEasing,
    )

    val phaseTwoScrim = tween<Float>(
        durationMillis = PHASE_2_DURATION_MS,
        easing = LinearEasing,
    )

    val phaseTwoButtons = tween<Float>(
        durationMillis = PHASE_2_DURATION_MS,
        easing = EaseOutCubic,
    )

    val dismissalPhaseOneScale = tween<Float>(
        durationMillis = DISMISSAL_PHASE_1_DURATION_MS,
        easing = FastOutSlowInEasing,
    )

    val dismissalPhaseTwoFade = tween<Float>(
        durationMillis = DISMISSAL_PHASE_2_DURATION_MS,
        easing = LinearEasing,
    )
}

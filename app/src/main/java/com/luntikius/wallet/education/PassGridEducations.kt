package com.luntikius.wallet.education

import com.luntikius.wallet.educations.Education
import com.luntikius.wallet.educations.EducationIllustration
import com.luntikius.wallet.educations.EducationStep
import com.luntikius.wallet.educations.EducationStepPlacement

object PassGridEducationIds {
    const val EMPTY = "pass_grid_empty"
    const val FIRST_CARD = "pass_grid_first_card"
}

object PassGridEducationTarget {
    const val ADD_BUTTON = "pass_grid_add_button"
    const val FIRST_PASS_CARD = "pass_grid_first_pass_card"
}

fun createPassGridEmptyEducation(): Education = Education(
    id = PassGridEducationIds.EMPTY,
    steps = listOf(
        EducationStep(
            id = "add_pass",
            targetKey = PassGridEducationTarget.ADD_BUTTON,
            text = "Add a pass with the camera or from a file. You can also export a pass from another app.",
        ),
    ),
)

fun createPassGridFirstCardEducation(): Education = Education(
    id = PassGridEducationIds.FIRST_CARD,
    steps = listOf(
        EducationStep(
            id = "first_card_actions",
            targetKey = PassGridEducationTarget.FIRST_PASS_CARD,
            text = "Use your pass card from the grid.",
            bullets = listOf(
                "Tap a card to view details.",
                "Drag and drop cards to reorder them.",
                "Drag a card to the bottom of the screen to delete it.",
            ),
        ),
        EducationStep(
            id = "pull_to_refresh",
            text = "Pull the screen down to refresh your passes.",
            placement = EducationStepPlacement.Center,
            illustration = EducationIllustration.PullToRefresh,
        ),
    ),
)

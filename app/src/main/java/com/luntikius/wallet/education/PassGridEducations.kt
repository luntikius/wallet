package com.luntikius.wallet.education

import android.content.Context
import com.luntikius.wallet.corestrings.R
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

fun createPassGridEmptyEducation(context: Context): Education = Education(
    id = PassGridEducationIds.EMPTY,
    steps = listOf(
        EducationStep(
            id = "add_pass",
            targetKey = PassGridEducationTarget.ADD_BUTTON,
            text = context.getString(R.string.education_add_pass),
        ),
    ),
)

fun createPassGridFirstCardEducation(context: Context): Education = Education(
    id = PassGridEducationIds.FIRST_CARD,
    steps = listOf(
        EducationStep(
            id = "first_card_actions",
            targetKey = PassGridEducationTarget.FIRST_PASS_CARD,
            text = context.getString(R.string.education_use_card),
            bullets = listOf(
                context.getString(R.string.education_tap_card),
                context.getString(R.string.education_drag_reorder),
                context.getString(R.string.education_drag_delete),
            ),
        ),
        EducationStep(
            id = "pull_to_refresh",
            text = context.getString(R.string.education_pull_refresh),
            placement = EducationStepPlacement.Center,
            illustration = EducationIllustration.PullToRefresh,
        ),
    ),
)

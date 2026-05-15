package com.luntikius.wallet.educations

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EducationHostTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun targetlessCenteredStep_rendersText() {
        composeRule.setContent {
            MaterialTheme {
                EducationTargetProvider {
                    EducationHost(
                        activeEducation = ActiveEducation(
                            education = Education(
                                id = "test",
                                steps = listOf(EducationStep(id = "step", text = "Centered education")),
                            ),
                            stepIndex = 0,
                        ),
                        onNext = {},
                        onBack = {},
                        onFinish = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("Centered education").assertIsDisplayed()
    }

    @Test
    fun multiStepEducation_buttonsMoveForwardAndBack() {
        val education = Education(
            id = "test",
            steps = listOf(
                EducationStep(id = "first", text = "First step"),
                EducationStep(id = "second", text = "Second step"),
            ),
        )
        var activeEducation by mutableStateOf(ActiveEducation(education = education, stepIndex = 0))

        composeRule.setContent {
            MaterialTheme {
                EducationTargetProvider {
                    EducationHost(
                        activeEducation = activeEducation,
                        onNext = {
                            activeEducation = activeEducation.copy(stepIndex = activeEducation.stepIndex + 1)
                        },
                        onBack = {
                            activeEducation = activeEducation.copy(stepIndex = activeEducation.stepIndex - 1)
                        },
                        onFinish = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("Next").performClick()
        composeRule.onNodeWithText("Second step").assertIsDisplayed()
        composeRule.onNodeWithText("Back").performClick()
        composeRule.onNodeWithText("First step").assertIsDisplayed()
    }

    @Test
    fun tappingScrim_invokesFinish() {
        var finished = false

        composeRule.setContent {
            MaterialTheme {
                EducationTargetProvider {
                    EducationHost(
                        activeEducation = ActiveEducation(
                            education = Education(
                                id = "test",
                                steps = listOf(EducationStep(id = "step", text = "Dismiss me")),
                            ),
                            stepIndex = 0,
                        ),
                        onNext = {},
                        onBack = {},
                        onFinish = { finished = true },
                    )
                }
            }
        }

        composeRule.onNodeWithTag("education_scrim").performClick()

        assertTrue(finished)
    }
}

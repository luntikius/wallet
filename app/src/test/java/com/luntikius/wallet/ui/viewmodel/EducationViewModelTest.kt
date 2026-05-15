package com.luntikius.wallet.ui.viewmodel

import com.luntikius.wallet.education.EducationConfig
import com.luntikius.wallet.education.EducationProgressRepository
import com.luntikius.wallet.education.PassGridEducationIds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EducationViewModelTest {

    @Test
    fun shouldShowOnboarding_returnsTrueOnlyBeforeCompletionOnNormalLaunch() {
        val repository = FakeEducationProgressRepository()
        val viewModel = EducationViewModel(repository, EducationConfig(forceShowEducations = false))

        assertTrue(viewModel.shouldShowOnboarding(isExternalImport = false))

        viewModel.completeOnboarding()

        assertFalse(viewModel.shouldShowOnboarding(isExternalImport = false))
    }

    @Test
    fun shouldShowOnboarding_returnsFalseForExternalImport() {
        val repository = FakeEducationProgressRepository()
        val viewModel = EducationViewModel(repository, EducationConfig(forceShowEducations = true))

        assertFalse(viewModel.shouldShowOnboarding(isExternalImport = true))
    }

    @Test
    fun emptyGridEducation_appearsAfterOnboardingWhenThereAreNoPasses() {
        val repository = FakeEducationProgressRepository(onboardingCompleted = true)
        val viewModel = EducationViewModel(repository, EducationConfig(forceShowEducations = false))

        viewModel.showPassGridEducationIfNeeded(passCount = 0, isInitialLoading = false)

        assertEquals(PassGridEducationIds.EMPTY, viewModel.activeEducation.value?.education?.id)
    }

    @Test
    fun firstCardEducation_appearsAfterOnboardingWhenThereArePasses() {
        val repository = FakeEducationProgressRepository(onboardingCompleted = true)
        val viewModel = EducationViewModel(repository, EducationConfig(forceShowEducations = false))

        viewModel.showPassGridEducationIfNeeded(passCount = 1, isInitialLoading = false)

        assertEquals(PassGridEducationIds.FIRST_CARD, viewModel.activeEducation.value?.education?.id)
    }

    @Test
    fun firstCardEducation_stepsAdvanceStrictlyInOrder() {
        val repository = FakeEducationProgressRepository(onboardingCompleted = true)
        val viewModel = EducationViewModel(repository, EducationConfig(forceShowEducations = false))

        viewModel.showPassGridEducationIfNeeded(passCount = 1, isInitialLoading = false)

        assertEquals("first_card_actions", viewModel.activeEducation.value?.step?.id)

        viewModel.nextEducationStep()

        assertEquals("pull_to_refresh", viewModel.activeEducation.value?.step?.id)
    }

    @Test
    fun finishActiveEducation_marksEducationCompletedInNormalMode() {
        val repository = FakeEducationProgressRepository(onboardingCompleted = true)
        val viewModel = EducationViewModel(repository, EducationConfig(forceShowEducations = false))

        viewModel.showPassGridEducationIfNeeded(passCount = 0, isInitialLoading = false)
        viewModel.finishActiveEducation()

        assertTrue(repository.isEducationCompleted(PassGridEducationIds.EMPTY))
        assertNull(viewModel.activeEducation.value)
    }

    @Test
    fun debugForce_ignoresPersistedCompletion() {
        val repository = FakeEducationProgressRepository(
            onboardingCompleted = true,
            completedEducations = setOf(PassGridEducationIds.EMPTY),
        )
        val viewModel = EducationViewModel(repository, EducationConfig(forceShowEducations = true))

        viewModel.showPassGridEducationIfNeeded(passCount = 0, isInitialLoading = false)

        assertEquals(PassGridEducationIds.EMPTY, viewModel.activeEducation.value?.education?.id)
    }

    @Test
    fun debugForce_finishSuppressesReplayOnlyInCurrentSession() {
        val repository = FakeEducationProgressRepository(onboardingCompleted = true)
        val firstViewModel = EducationViewModel(repository, EducationConfig(forceShowEducations = true))

        firstViewModel.showPassGridEducationIfNeeded(passCount = 0, isInitialLoading = false)
        firstViewModel.finishActiveEducation()
        firstViewModel.showPassGridEducationIfNeeded(passCount = 0, isInitialLoading = false)

        assertNull(firstViewModel.activeEducation.value)

        val secondViewModel = EducationViewModel(repository, EducationConfig(forceShowEducations = true))
        secondViewModel.showPassGridEducationIfNeeded(passCount = 0, isInitialLoading = false)

        assertEquals(PassGridEducationIds.EMPTY, secondViewModel.activeEducation.value?.education?.id)
    }

    private class FakeEducationProgressRepository(
        private var onboardingCompleted: Boolean = false,
        completedEducations: Set<String> = emptySet(),
    ) : EducationProgressRepository {
        private val completedEducationIds = completedEducations.toMutableSet()

        override fun isOnboardingCompleted(): Boolean = onboardingCompleted

        override fun setOnboardingCompleted() {
            onboardingCompleted = true
        }

        override fun isEducationCompleted(educationId: String): Boolean = educationId in completedEducationIds

        override fun setEducationCompleted(educationId: String) {
            completedEducationIds += educationId
        }
    }
}

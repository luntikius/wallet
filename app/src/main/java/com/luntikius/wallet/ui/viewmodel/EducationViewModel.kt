package com.luntikius.wallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.luntikius.wallet.education.EducationConfig
import com.luntikius.wallet.education.EducationProgressRepository
import com.luntikius.wallet.education.PassGridEducationIds
import com.luntikius.wallet.education.createPassGridEmptyEducation
import com.luntikius.wallet.education.createPassGridFirstCardEducation
import com.luntikius.wallet.educations.ActiveEducation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EducationViewModel(
    private val progressRepository: EducationProgressRepository,
    private val config: EducationConfig,
) : ViewModel() {

    private val sessionDismissedEducationIds = mutableSetOf<String>()
    private var isExternalEntry = false

    private val _activeEducation = MutableStateFlow<ActiveEducation?>(null)
    val activeEducation: StateFlow<ActiveEducation?> = _activeEducation.asStateFlow()

    fun startAppEntry(isExternalImport: Boolean) {
        isExternalEntry = isExternalImport
    }

    fun shouldShowOnboarding(isExternalImport: Boolean): Boolean {
        if (isExternalImport) return false
        return config.forceShowEducations || !progressRepository.isOnboardingCompleted()
    }

    fun completeOnboarding() {
        if (!config.forceShowEducations) {
            progressRepository.setOnboardingCompleted()
        }
    }

    fun showPassGridEducationIfNeeded(passCount: Int, isInitialLoading: Boolean) {
        if (isInitialLoading || isExternalEntry || _activeEducation.value != null) return

        val education = when {
            passCount == 0 -> createPassGridEmptyEducation()
            passCount > 0 -> createPassGridFirstCardEducation()
            else -> null
        } ?: return

        if (shouldShowEducation(education.id)) {
            _activeEducation.value = ActiveEducation(education = education, stepIndex = 0)
        }
    }

    fun nextEducationStep() {
        val active = _activeEducation.value ?: return
        if (active.isLastStep) {
            finishActiveEducation()
        } else {
            _activeEducation.value = active.copy(stepIndex = active.stepIndex + 1)
        }
    }

    fun previousEducationStep() {
        val active = _activeEducation.value ?: return
        if (active.canGoBack) {
            _activeEducation.value = active.copy(stepIndex = active.stepIndex - 1)
        }
    }

    fun finishActiveEducation() {
        val educationId = _activeEducation.value?.education?.id ?: return
        if (config.forceShowEducations) {
            sessionDismissedEducationIds += educationId
        } else {
            progressRepository.setEducationCompleted(educationId)
        }
        _activeEducation.value = null
    }

    private fun shouldShowEducation(educationId: String): Boolean {
        if (config.forceShowEducations) {
            return educationId !in sessionDismissedEducationIds
        }

        val onboardingReady = progressRepository.isOnboardingCompleted()
        if (!onboardingReady) return false

        return when (educationId) {
            PassGridEducationIds.EMPTY,
            PassGridEducationIds.FIRST_CARD,
            -> !progressRepository.isEducationCompleted(educationId)
            else -> false
        }
    }
}

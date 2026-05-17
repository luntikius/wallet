package com.luntikius.wallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luntikius.wallet.data.model.ShareStatus
import com.luntikius.wallet.data.repository.WalletArchiveRepository
import com.luntikius.wallet.settings.AppThemeMode
import com.luntikius.wallet.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val walletArchiveRepository: WalletArchiveRepository,
) : ViewModel() {

    companion object {
        const val WALLET_ARCHIVE_SHARE_ID = "__wallet_archive__"
    }

    val themeMode: StateFlow<AppThemeMode> = settingsRepository.themeMode
    val showEducations: StateFlow<Boolean> = settingsRepository.showEducations

    private val _shareStatus = MutableStateFlow<ShareStatus>(ShareStatus.Idle)
    val shareStatus: StateFlow<ShareStatus> = _shareStatus.asStateFlow()

    fun setThemeMode(themeMode: AppThemeMode) {
        settingsRepository.setThemeMode(themeMode)
    }

    fun setShowEducations(enabled: Boolean) {
        settingsRepository.setShowEducations(enabled)
    }

    fun prepareShareWalletArchive() {
        viewModelScope.launch {
            _shareStatus.value = ShareStatus.Loading(WALLET_ARCHIVE_SHARE_ID)
            val result = walletArchiveRepository.exportAllPassesForSharing()

            _shareStatus.value = if (result.isSuccess) {
                ShareStatus.Success(result.getOrThrow(), WALLET_ARCHIVE_SHARE_ID)
            } else {
                ShareStatus.Error(
                    result.exceptionOrNull()?.message ?: "Failed to export wallet",
                    WALLET_ARCHIVE_SHARE_ID,
                )
            }
        }
    }

    fun resetShareStatus() {
        _shareStatus.value = ShareStatus.Idle
    }
}

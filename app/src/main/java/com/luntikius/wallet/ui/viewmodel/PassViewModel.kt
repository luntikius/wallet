package com.luntikius.wallet.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luntikius.wallet.data.model.Pass
import com.luntikius.wallet.data.model.RefreshStatus
import com.luntikius.wallet.data.network.PKPassUpdateService
import com.luntikius.wallet.data.repository.PassRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for pass management.
 * Handles pass import, deletion, and state management.
 */
class PassViewModel(private val repository: PassRepository) : ViewModel() {

    /**
     * Loading state for initial data load.
     */
    private val _isInitialLoading = MutableStateFlow(true)
    val isInitialLoading: StateFlow<Boolean> = _isInitialLoading.asStateFlow()

    /**
     * All passes from the repository.
     */
    val passes: StateFlow<List<Pass>> = repository.getAllPasses()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList(),
        )

    init {
        // Mark loading as complete after first emission from repository
        viewModelScope.launch {
            repository.getAllPasses().first()
            _isInitialLoading.value = false
        }
    }

    /**
     * Import status for showing loading/error states.
     */
    private val _importStatus = MutableStateFlow<ImportStatus>(ImportStatus.Idle)
    val importStatus: StateFlow<ImportStatus> = _importStatus.asStateFlow()

    /**
     * Refresh status for showing refresh loading/success/error states.
     */
    private val _refreshStatus = MutableStateFlow<RefreshStatus>(RefreshStatus.Idle)
    val refreshStatus: StateFlow<RefreshStatus> = _refreshStatus.asStateFlow()

    /**
     * Preview pass state for showing pass before import.
     */
    private val _previewPass = MutableStateFlow<Pass?>(null)
    val previewPass: StateFlow<Pass?> = _previewPass.asStateFlow()

    /**
     * Preview status for showing loading/error states during preview.
     */
    private val _previewStatus = MutableStateFlow<PreviewStatus>(PreviewStatus.Idle)
    val previewStatus: StateFlow<PreviewStatus> = _previewStatus.asStateFlow()

    /**
     * Import a pass from a URI.
     */
    fun importPass(uri: Uri) {
        viewModelScope.launch {
            _importStatus.value = ImportStatus.Loading
            val result = repository.importPass(uri)
            _importStatus.value = if (result.isSuccess) {
                ImportStatus.Success
            } else {
                ImportStatus.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }

            // Reset status after a delay
            kotlinx.coroutines.delay(2000)
            _importStatus.value = ImportStatus.Idle
        }
    }

    /**
     * Refresh a single pass by fetching updated data from its web service.
     */
    fun refreshPass(passId: String) {
        viewModelScope.launch {
            _refreshStatus.value = RefreshStatus.Loading(passId)
            val result = repository.refreshPass(passId)

            if (result.isSuccess) {
                val updateResult = result.getOrThrow()
                _refreshStatus.value = when (updateResult) {
                    is PKPassUpdateService.UpdateResult.Updated -> {
                        RefreshStatus.Success(1)
                    }
                    is PKPassUpdateService.UpdateResult.NotModified -> {
                        RefreshStatus.Success(0)
                    }
                    is PKPassUpdateService.UpdateResult.Deleted -> {
                        RefreshStatus.Error("Pass was deleted", passId)
                    }
                    is PKPassUpdateService.UpdateResult.Unauthorized -> {
                        RefreshStatus.Error("Update authorization failed", passId)
                    }
                    is PKPassUpdateService.UpdateResult.NetworkError -> {
                        RefreshStatus.Error(updateResult.message, passId)
                    }
                    is PKPassUpdateService.UpdateResult.NoWebService -> {
                        RefreshStatus.Error("This pass does not support updates", passId)
                    }
                }
            } else {
                _refreshStatus.value = RefreshStatus.Error(
                    result.exceptionOrNull()?.message ?: "Unknown error",
                    passId,
                )
            }

            // Reset status after a delay
            kotlinx.coroutines.delay(2000)
            _refreshStatus.value = RefreshStatus.Idle
        }
    }

    /**
     * Refresh all passes by fetching updated data from their web services.
     */
    fun refreshAllPasses() {
        viewModelScope.launch {
            _refreshStatus.value = RefreshStatus.Loading(null) // null = all passes
            val result = repository.refreshAllPasses()

            if (result.isSuccess) {
                val results = result.getOrThrow()
                val updatedCount = results.values.count { it is PKPassUpdateService.UpdateResult.Updated }
                val errorResults = results.values.filterIsInstance<PKPassUpdateService.UpdateResult.NetworkError>()

                _refreshStatus.value = if (errorResults.isNotEmpty()) {
                    RefreshStatus.Error(errorResults.first().message, null)
                } else {
                    RefreshStatus.Success(updatedCount)
                }
            } else {
                _refreshStatus.value = RefreshStatus.Error(
                    result.exceptionOrNull()?.message ?: "Unknown error",
                    null,
                )
            }

            // Reset status after a delay
            kotlinx.coroutines.delay(2000)
            _refreshStatus.value = RefreshStatus.Idle
        }
    }

    /**
     * Delete a pass.
     */
    fun deletePass(pass: Pass) {
        viewModelScope.launch {
            repository.deletePass(pass)
        }
    }

    /**
     * Update the display order of passes.
     */
    fun updatePassOrder(passOrderMap: Map<String, Int>) {
        viewModelScope.launch {
            repository.updateDisplayOrders(passOrderMap)
        }
    }

    /**
     * Get a specific pass by ID.
     */
    suspend fun getPassById(passId: String): Pass? = repository.getPassById(passId)

    /**
     * Toggle automatic background refresh for a pass.
     */
    fun setAutoRefreshEnabled(passId: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoRefreshEnabled(passId, enabled)
        }
    }

    /**
     * Preview a pass before adding it to the wallet.
     * Parses the pass to temporary storage and sets it in previewPass state.
     */
    fun previewPass(uri: Uri) {
        viewModelScope.launch {
            // Clean up any existing preview first
            _previewPass.value?.let { existingPass ->
                repository.cleanupPreviewAssets(existingPass)
            }

            _previewStatus.value = PreviewStatus.Loading
            val result = repository.parsePassForPreview(uri)

            if (result.isSuccess) {
                _previewPass.value = result.getOrThrow()
                _previewStatus.value = PreviewStatus.Ready
            } else {
                _previewPass.value = null
                _previewStatus.value = PreviewStatus.Error(
                    result.exceptionOrNull()?.message ?: "Failed to load pass",
                )
            }
        }
    }

    /**
     * Confirm adding the previewed pass to the wallet.
     * Moves assets to permanent storage and saves to database.
     */
    fun confirmAddPass() {
        viewModelScope.launch {
            val pass = _previewPass.value ?: return@launch

            _previewStatus.value = PreviewStatus.Loading
            val result = repository.finalizePassImport(pass)

            if (result.isSuccess) {
                _previewPass.value = null
                _previewStatus.value = PreviewStatus.Idle
                _importStatus.value = ImportStatus.Success

                // Reset import status after a delay
                kotlinx.coroutines.delay(2000)
                _importStatus.value = ImportStatus.Idle
            } else {
                _previewStatus.value = PreviewStatus.Error(
                    result.exceptionOrNull()?.message ?: "Failed to add pass",
                )
            }
        }
    }

    /**
     * Cancel the preview and clean up temporary assets.
     */
    fun cancelPreview() {
        viewModelScope.launch {
            _previewPass.value?.let { pass ->
                repository.cleanupPreviewAssets(pass)
            }
            _previewPass.value = null
            _previewStatus.value = PreviewStatus.Idle
        }
    }
}

/**
 * Import status sealed class.
 */
sealed class ImportStatus {
    object Idle : ImportStatus()
    object Loading : ImportStatus()
    object Success : ImportStatus()
    data class Error(val message: String) : ImportStatus()
}

/**
 * Preview status sealed class.
 */
sealed class PreviewStatus {
    object Idle : PreviewStatus()
    object Loading : PreviewStatus()
    object Ready : PreviewStatus()
    data class Error(val message: String) : PreviewStatus()
}

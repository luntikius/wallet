package com.luntikius.wallet.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luntikius.wallet.data.model.Pass
import com.luntikius.wallet.data.repository.PassRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for pass management.
 * Handles pass import, deletion, and state management.
 */
class PassViewModel(
    private val repository: PassRepository
) : ViewModel() {

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
            initialValue = emptyList()
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
    suspend fun getPassById(passId: String): Pass? {
        return repository.getPassById(passId)
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

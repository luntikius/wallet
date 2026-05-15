package com.luntikius.wallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luntikius.wallet.data.model.Pass
import com.luntikius.wallet.data.repository.PassRepository
import kotlinx.coroutines.launch

class CustomPassBuilderViewModel(
    private val passRepository: PassRepository,
    private val importStatusHolder: ImportStatusHolder,
) : ViewModel() {

    fun createCustomPass(pass: Pass) {
        viewModelScope.launch {
            importStatusHolder.setImportStatus(ImportStatus.Loading)
            val result = passRepository.createCustomPass(pass)
            importStatusHolder.setImportStatus(
                if (result.isSuccess) {
                    ImportStatus.Success
                } else {
                    ImportStatus.Error(result.exceptionOrNull()?.message ?: "Unknown error")
                },
            )

            kotlinx.coroutines.delay(2000)
            importStatusHolder.setImportStatus(ImportStatus.Idle)
        }
    }
}

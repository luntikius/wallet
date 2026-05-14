package com.luntikius.wallet.ui.viewmodel

import android.net.Uri
import com.luntikius.wallet.data.model.Pass
import com.luntikius.wallet.data.repository.PassRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PassPreviewViewModelTest {

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun previewPass_success_setsReadyStatus() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val repository = mockk<PassRepository>()
        val pass = mockk<Pass>()
        coEvery { repository.parsePassForPreview(any()) } returns Result.success(pass)
        coEvery { repository.cleanupPreviewAssets(any()) } returns Result.success(Unit)
        val viewModel = PassPreviewViewModel(repository, ImportStatusHolder())
        val uri = mockk<Uri>()

        viewModel.previewPass(uri)

        assertEquals(PreviewStatus.Ready, viewModel.previewStatus.first())
        assertEquals(pass, viewModel.previewPass.first())
    }

    @Test
    fun previewPass_failure_setsErrorStatus() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val repository = mockk<PassRepository>()
        coEvery { repository.parsePassForPreview(any()) } returns Result.failure(Exception("parse error"))
        val viewModel = PassPreviewViewModel(repository, ImportStatusHolder())
        val uri = mockk<Uri>()

        viewModel.previewPass(uri)

        val status = viewModel.previewStatus.first()
        assert(status is PreviewStatus.Error)
        assertNull(viewModel.previewPass.first())
    }

    @Test
    fun confirmAddPass_success_callsFinalizeImport() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val repository = mockk<PassRepository>()
        val pass = mockk<Pass>()
        val holder = ImportStatusHolder()
        coEvery { repository.parsePassForPreview(any()) } returns Result.success(pass)
        coEvery { repository.cleanupPreviewAssets(any()) } returns Result.success(Unit)
        coEvery { repository.finalizePassImport(pass) } returns Result.success(pass)
        val viewModel = PassPreviewViewModel(repository, holder)

        viewModel.previewPass(mockk<Uri>())
        viewModel.confirmAddPass()

        coVerify(exactly = 1) { repository.finalizePassImport(pass) }
    }

    @Test
    fun cancelPreview_cleansUpAssets() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val repository = mockk<PassRepository>()
        val pass = mockk<Pass>()
        coEvery { repository.parsePassForPreview(any()) } returns Result.success(pass)
        coEvery { repository.cleanupPreviewAssets(any()) } returns Result.success(Unit)
        val viewModel = PassPreviewViewModel(repository, ImportStatusHolder())

        viewModel.previewPass(mockk<Uri>())
        viewModel.cancelPreview()

        coVerify(exactly = 1) { repository.cleanupPreviewAssets(pass) }
        assertNull(viewModel.previewPass.first())
    }
}

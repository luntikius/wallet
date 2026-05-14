package com.luntikius.wallet.ui.viewmodel

import com.luntikius.wallet.data.model.Pass
import com.luntikius.wallet.data.repository.PassRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CustomPassBuilderViewModelTest {

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun createCustomPass_success_emitsImportSuccess() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val repository = mockk<PassRepository>()
        every { repository.getAllPasses() } returns flowOf(emptyList())
        val pass = mockk<Pass>()
        coEvery { repository.createCustomPass(pass) } returns Result.success(pass)
        val holder = ImportStatusHolder()
        val viewModel = CustomPassBuilderViewModel(repository, holder)

        viewModel.createCustomPass(pass)

        coVerify(exactly = 1) { repository.createCustomPass(pass) }
    }

    @Test
    fun createCustomPass_failure_emitsImportError() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val repository = mockk<PassRepository>()
        every { repository.getAllPasses() } returns flowOf(emptyList())
        val pass = mockk<Pass>()
        coEvery { repository.createCustomPass(pass) } returns Result.failure(Exception("create error"))
        val holder = ImportStatusHolder()
        val viewModel = CustomPassBuilderViewModel(repository, holder)

        viewModel.createCustomPass(pass)

        coVerify(exactly = 1) { repository.createCustomPass(pass) }
    }
}

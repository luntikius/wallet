package com.luntikius.wallet.ui.viewmodel

import com.luntikius.wallet.data.model.Pass
import com.luntikius.wallet.data.repository.PassRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PassGridViewModelTest {

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun passes_reflectsRepositoryFlow() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val passes = emptyList<Pass>()
        val repository = mockk<PassRepository>()
        every { repository.getAllPasses() } returns flowOf(passes)
        val viewModel = PassGridViewModel(repository, ImportStatusHolder())

        assertEquals(passes, viewModel.passes.first())
    }

    @Test
    fun deletePass_callsRepository() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val repository = mockk<PassRepository>()
        every { repository.getAllPasses() } returns flowOf(emptyList())
        coEvery { repository.deletePass(any()) } returns Result.success(Unit)
        val viewModel = PassGridViewModel(repository, ImportStatusHolder())
        val pass = mockk<Pass>()

        viewModel.deletePass(pass)

        coVerify(exactly = 1) { repository.deletePass(pass) }
    }

    @Test
    fun updatePassOrder_callsRepository() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val repository = mockk<PassRepository>()
        every { repository.getAllPasses() } returns flowOf(emptyList())
        val order = mapOf("a" to 1)
        coEvery { repository.updateDisplayOrders(order) } returns Result.success(Unit)
        val viewModel = PassGridViewModel(repository, ImportStatusHolder())

        viewModel.updatePassOrder(order)

        coVerify(exactly = 1) { repository.updateDisplayOrders(order) }
    }
}

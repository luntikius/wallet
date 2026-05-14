package com.luntikius.wallet.di

import com.luntikius.wallet.data.local.PassDatabase
import com.luntikius.wallet.data.parser.ParserRegistry
import com.luntikius.wallet.data.repository.PassRepository
import com.luntikius.wallet.data.repository.PassRepositoryImpl
import com.luntikius.wallet.ui.viewmodel.CustomPassBuilderViewModel
import com.luntikius.wallet.ui.viewmodel.ImportStatusHolder
import com.luntikius.wallet.ui.viewmodel.PassGridViewModel
import com.luntikius.wallet.ui.viewmodel.PassPreviewViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val dataModule = module {
    single { PassDatabase.getInstance(androidContext()) }
    single { ParserRegistry(androidContext()) }
    single<PassRepository> {
        PassRepositoryImpl(
            passDao = get<PassDatabase>().passDao(),
            parserRegistry = get(),
            context = androidContext(),
        )
    }
}

val domainModule = module {
    single { ImportStatusHolder() }
}

val uiModule = module {
    viewModel { PassGridViewModel(get(), get()) }
    viewModel { PassPreviewViewModel(get(), get()) }
    viewModel { CustomPassBuilderViewModel(get(), get()) }
}

val appModules = listOf(dataModule, domainModule, uiModule)

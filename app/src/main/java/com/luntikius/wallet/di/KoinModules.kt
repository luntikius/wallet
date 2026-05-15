package com.luntikius.wallet.di

import com.google.android.gms.wearable.Wearable
import com.luntikius.wallet.BuildConfig
import com.luntikius.wallet.data.local.PassDatabase
import com.luntikius.wallet.data.parser.ParserRegistry
import com.luntikius.wallet.data.repository.PassRepository
import com.luntikius.wallet.data.repository.PassRepositoryImpl
import com.luntikius.wallet.education.EducationConfig
import com.luntikius.wallet.education.EducationProgressRepository
import com.luntikius.wallet.education.SharedPreferencesEducationProgressRepository
import com.luntikius.wallet.ui.viewmodel.CustomPassBuilderViewModel
import com.luntikius.wallet.ui.viewmodel.EducationViewModel
import com.luntikius.wallet.ui.viewmodel.ImportStatusHolder
import com.luntikius.wallet.ui.viewmodel.PassGridViewModel
import com.luntikius.wallet.ui.viewmodel.PassPreviewViewModel
import com.luntikius.wallet.wear.PhoneWearSyncCoordinator
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
    single { Wearable.getDataClient(androidContext()) }
    single {
        PhoneWearSyncCoordinator(
            dataClient = get(),
            passRepository = get(),
        )
    }
}

val domainModule = module {
    single { ImportStatusHolder() }
    single { EducationConfig(forceShowEducations = BuildConfig.FORCE_SHOW_EDUCATIONS) }
    single<EducationProgressRepository> { SharedPreferencesEducationProgressRepository(androidContext()) }
}

val uiModule = module {
    viewModel { PassGridViewModel(get(), get()) }
    viewModel { PassPreviewViewModel(get(), get()) }
    viewModel { CustomPassBuilderViewModel(get(), get()) }
    viewModel { EducationViewModel(get(), get()) }
}

val appModules = listOf(dataModule, domainModule, uiModule)

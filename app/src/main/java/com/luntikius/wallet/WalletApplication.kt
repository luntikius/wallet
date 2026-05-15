package com.luntikius.wallet

import android.app.Application
import com.luntikius.wallet.data.repository.PassRepository
import com.luntikius.wallet.di.appModules
import com.luntikius.wallet.wear.PhoneWearSyncCoordinator
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class WalletApplication : Application() {
    private lateinit var wearSyncCoordinator: PhoneWearSyncCoordinator

    override fun onCreate() {
        super.onCreate()
        val koinApplication = startKoin {
            androidContext(this@WalletApplication)
            modules(appModules)
        }

        wearSyncCoordinator = PhoneWearSyncCoordinator(
            context = this,
            passRepository = koinApplication.koin.get<PassRepository>(),
        )
        wearSyncCoordinator.start()
    }
}

package com.luntikius.wallet

import android.app.Application
import com.luntikius.wallet.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class WalletApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@WalletApplication)
            modules(appModules)
        }
    }
}

package com.luntikius.wallet.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.luntikius.wallet.wear.data.WearPassRepository
import com.luntikius.wallet.wear.sync.WearPhonePassOpener
import com.luntikius.wallet.wear.sync.WearPhoneSyncRequester
import com.luntikius.wallet.wear.ui.WearWalletApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val repository: WearPassRepository by inject()
    private val syncRequester: WearPhoneSyncRequester by inject()
    private val phonePassOpener: WearPhonePassOpener by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch(Dispatchers.IO) {
            syncRequester.requestSync()
        }

        setContent {
            WearWalletApp(
                repository = repository,
                onOpenPassOnPhone = { passId ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        phonePassOpener.openPassOnPhone(passId)
                    }
                },
            )
        }
    }
}

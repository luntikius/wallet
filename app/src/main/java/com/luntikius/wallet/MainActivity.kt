package com.luntikius.wallet

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.luntikius.wallet.data.local.PassDatabase
import com.luntikius.wallet.data.parser.ParserRegistry
import com.luntikius.wallet.data.repository.PassRepositoryImpl
import com.luntikius.wallet.ui.navigation.PassNavGraph
import com.luntikius.wallet.ui.theme.WalletTheme
import com.luntikius.wallet.ui.viewmodel.PassViewModel

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: PassViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize database, repository, and ViewModel
        val database = PassDatabase.getInstance(applicationContext)
        val parserRegistry = ParserRegistry(applicationContext)
        val repository = PassRepositoryImpl(
            passDao = database.passDao(),
            parserRegistry = parserRegistry,
            context = applicationContext
        )
        viewModel = PassViewModel(repository)

        setContent {
            WalletTheme {
                val navController = rememberNavController()

                PassNavGraph(
                    navController = navController,
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )

                // Handle intent for opening pass files
                LaunchedEffect(Unit) {
                    handleIntent(intent)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            intent.data?.let { uri ->
                viewModel.importPass(uri)
            }
        }
    }
}
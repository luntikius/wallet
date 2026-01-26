package com.luntikius.wallet.ui.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.luntikius.wallet.ui.screens.PassGridScreen
import com.luntikius.wallet.ui.viewmodel.PassViewModel

/**
 * Navigation routes for the app.
 */
object Routes {
    const val GRID = "grid"

    fun detail(passId: String) = "detail/$passId"
}

/**
 * Main navigation graph with shared element transitions.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PassNavGraph(navController: NavHostController, viewModel: PassViewModel, modifier: Modifier = Modifier) {
    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = Routes.GRID,
            modifier = modifier,
        ) {
            composable(Routes.GRID) {
                PassGridScreen(
                    viewModel = viewModel,
                    onPassClick = { passId ->
                        navController.navigate(Routes.detail(passId))
                    },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this,
                )
            }
        }
    }
}

package com.luntikius.wallet.ui.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.luntikius.wallet.ui.screens.PassDetailScreen
import com.luntikius.wallet.ui.screens.PassGridScreen
import com.luntikius.wallet.ui.viewmodel.PassViewModel

/**
 * Navigation routes for the app.
 */
object Routes {
    const val GRID = "grid"
    const val DETAIL = "detail/{passId}"

    fun detail(passId: String) = "detail/$passId"
}

/**
 * Main navigation graph with shared element transitions.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PassNavGraph(
    navController: NavHostController,
    viewModel: PassViewModel,
    modifier: Modifier = Modifier
) {
    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = Routes.GRID,
            modifier = modifier
        ) {
            composable(Routes.GRID) {
                PassGridScreen(
                    viewModel = viewModel,
                    onPassClick = { passId ->
                        navController.navigate(Routes.detail(passId))
                    },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this
                )
            }

            composable(
                route = Routes.DETAIL,
                arguments = listOf(
                    navArgument("passId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val passId = backStackEntry.arguments?.getString("passId") ?: return@composable
                PassDetailScreen(
                    passId = passId,
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this
                )
            }
        }
    }
}

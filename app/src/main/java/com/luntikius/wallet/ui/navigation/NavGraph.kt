package com.luntikius.wallet.ui.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.luntikius.wallet.ui.screens.InitialScreen
import com.luntikius.wallet.ui.screens.PassGridScreen
import com.luntikius.wallet.ui.screens.PassPreviewScreen
import com.luntikius.wallet.ui.viewmodel.PassViewModel

/**
 * Navigation routes for the app.
 */
object Routes {
    const val INITIAL = "initial"
    const val GRID = "grid"
    const val PREVIEW = "preview"

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
    intentUri: android.net.Uri?,
    modifier: Modifier = Modifier,
) {
    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = Routes.INITIAL,
            modifier = modifier,
        ) {
            composable(Routes.INITIAL) {
                InitialScreen(
                    viewModel = viewModel,
                    intentUri = intentUri,
                    onNavigateToGrid = {
                        navController.navigate(Routes.GRID) {
                            popUpTo(Routes.INITIAL) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToPreview = {
                        navController.navigate(Routes.PREVIEW) {
                            popUpTo(Routes.INITIAL) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(Routes.GRID) {
                PassGridScreen(
                    viewModel = viewModel,
                    navController = navController,
                    onPassClick = { passId ->
                        navController.navigate(Routes.detail(passId))
                    },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this,
                )
            }

            composable(Routes.PREVIEW) {
                PassPreviewScreen(
                    viewModel = viewModel,
                    onAdd = {
                        viewModel.confirmAddPass()
                        // Navigate to GRID and clear backstack up to start destination
                        navController.navigate(Routes.GRID) {
                            popUpTo(navController.graph.startDestinationId) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    },
                    onCancel = {
                        viewModel.cancelPreview()
                        navController.popBackStack()
                    },
                )
            }
        }
    }
}

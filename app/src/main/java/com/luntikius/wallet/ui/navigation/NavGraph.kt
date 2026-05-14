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
import com.google.mlkit.vision.barcode.common.Barcode
import com.luntikius.wallet.camera.CameraScanScreen
import com.luntikius.wallet.camera.ScanResult
import com.luntikius.wallet.ui.screens.CustomPassBuilderScreen
import com.luntikius.wallet.ui.screens.InitialScreen
import com.luntikius.wallet.ui.screens.PassGridScreen
import com.luntikius.wallet.ui.screens.PassPreviewScreen
import com.luntikius.wallet.ui.viewmodel.PassGridViewModel
import com.luntikius.wallet.ui.viewmodel.PassPreviewViewModel

/**
 * Navigation routes for the app.
 */
object Routes {
    const val INITIAL = "initial"
    const val GRID = "grid"
    const val PREVIEW = "preview"
    const val CAMERA_SCAN = "camera_scan"
    const val CUSTOM_PASS_BUILDER = "custom_pass_builder/{barcodeValue}/{barcodeFormat}"

    fun customPassBuilder(barcodeValue: String, barcodeFormat: String) =
        "custom_pass_builder/$barcodeValue/$barcodeFormat"
}

/**
 * Main navigation graph with shared element transitions.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PassNavGraph(
    navController: NavHostController,
    gridViewModel: PassGridViewModel,
    previewViewModel: PassPreviewViewModel,
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
                    viewModel = previewViewModel,
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
                    viewModel = gridViewModel,
                    navController = navController,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this,
                    onPreviewPass = { uri -> previewViewModel.previewPass(uri) },
                )
            }

            composable(Routes.PREVIEW) {
                PassPreviewScreen(
                    viewModel = previewViewModel,
                    onAdd = {
                        previewViewModel.confirmAddPass()
                        navController.navigate(Routes.GRID) {
                            popUpTo(navController.graph.startDestinationId) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    },
                    onCancel = {
                        previewViewModel.cancelPreview()
                        navController.popBackStack()
                    },
                )
            }

            composable(Routes.CAMERA_SCAN) {
                CameraScanScreen(
                    onScanResult = { scanResult ->
                        when (scanResult) {
                            is ScanResult.UrlDetected -> {
                                previewViewModel.downloadAndPreviewPass(scanResult.url)
                                navController.navigate(Routes.PREVIEW) {
                                    popUpTo(Routes.GRID) { inclusive = false }
                                }
                            }

                            is ScanResult.BarcodeDetected -> {
                                val formatName = getBarcodeFormatName(scanResult.format)
                                navController.navigate(
                                    Routes.customPassBuilder(
                                        scanResult.value,
                                        formatName,
                                    ),
                                ) {
                                    popUpTo(Routes.GRID) { inclusive = false }
                                }
                            }
                        }
                    },
                    onCancel = {
                        navController.popBackStack()
                    },
                )
            }

            composable(
                route = Routes.CUSTOM_PASS_BUILDER,
                arguments = listOf(
                    navArgument("barcodeValue") { type = NavType.StringType },
                    navArgument("barcodeFormat") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val barcodeValue = backStackEntry.arguments?.getString("barcodeValue") ?: ""
                val barcodeFormat = backStackEntry.arguments?.getString("barcodeFormat") ?: ""

                CustomPassBuilderScreen(
                    barcodeValue = barcodeValue,
                    barcodeFormat = barcodeFormat,
                    onCancel = {
                        navController.popBackStack()
                    },
                    onPassCreated = {
                        navController.navigate(Routes.GRID) {
                            popUpTo(navController.graph.startDestinationId) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    },
                )
            }
        }
    }
}

/**
 * Convert ML Kit barcode format code to readable string name.
 */
private fun getBarcodeFormatName(format: Int): String = when (format) {
    Barcode.FORMAT_CODE_128 -> "CODE_128"
    Barcode.FORMAT_CODE_39 -> "CODE_39"
    Barcode.FORMAT_CODE_93 -> "CODE_93"
    Barcode.FORMAT_CODABAR -> "CODABAR"
    Barcode.FORMAT_DATA_MATRIX -> "DATA_MATRIX"
    Barcode.FORMAT_EAN_13 -> "EAN_13"
    Barcode.FORMAT_EAN_8 -> "EAN_8"
    Barcode.FORMAT_ITF -> "ITF"
    Barcode.FORMAT_QR_CODE -> "QR_CODE"
    Barcode.FORMAT_UPC_A -> "UPC_A"
    Barcode.FORMAT_UPC_E -> "UPC_E"
    Barcode.FORMAT_PDF417 -> "PDF417"
    Barcode.FORMAT_AZTEC -> "AZTEC"
    else -> "UNKNOWN"
}

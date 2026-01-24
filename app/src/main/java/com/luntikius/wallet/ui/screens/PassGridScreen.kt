package com.luntikius.wallet.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luntikius.wallet.data.model.Pass
import com.luntikius.wallet.ui.utils.ensureContrast
import com.luntikius.wallet.ui.utils.parseColor
import com.luntikius.wallet.ui.utils.stripHtml
import com.luntikius.wallet.ui.viewmodel.ImportStatus
import com.luntikius.wallet.ui.viewmodel.PassViewModel
import java.io.File

/**
 * Grid screen displaying all passes.
 */
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PassGridScreen(
    viewModel: PassViewModel,
    onPassClick: (String) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier
) {
    val passes by viewModel.passes.collectAsState()
    val importStatus by viewModel.importStatus.collectAsState()
    var selectedPassId by remember { mutableStateOf<String?>(null) }

    // File picker launcher
    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.importPass(it) }
    }

    // Show dialog if a pass is selected
    selectedPassId?.let { passId ->
        PassCardDialog(
            passId = passId,
            viewModel = viewModel,
            onDismiss = { selectedPassId = null }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        if (passes.isEmpty()) {
            // Empty state
            Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp)
                        .padding(top = 64.dp, bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "No passes yet",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap + to add a pass",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
            }
        } else {
            // Grid of passes
            LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 64.dp, // Extra padding for the add button
                    bottom = 16.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(passes, key = { it.id }) { pass ->
                    PassTile(
                        pass = pass,
                        onClick = { selectedPassId = pass.id },
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                }
            }
        }

        // Import status snackbar
        if (importStatus is ImportStatus.Error) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Text((importStatus as ImportStatus.Error).message)
            }
        }

        if (importStatus is ImportStatus.Loading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Add button in top right corner
        IconButton(
            onClick = { pickFileLauncher.launch("*/*") },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Add Pass",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Individual pass tile in the grid.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PassTile(
    pass: Pass,
    onClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = isSystemInDarkTheme()
    val backgroundColor = pass.backgroundColor?.let { parseColor(it) }
        ?: MaterialTheme.colorScheme.surfaceVariant

    val textColor = ensureContrast(
        foregroundColor = pass.foregroundColor?.let { parseColor(it) },
        backgroundColor = backgroundColor,
        isDarkTheme = isDarkTheme,
        lightFallback = MaterialTheme.colorScheme.onSurface,
        darkFallback = MaterialTheme.colorScheme.onSurface
    )

    with(sharedTransitionScope) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(0.70f) // Vertical rectangle (card-like proportions)
                .sharedElement(
                    rememberSharedContentState(key = "card-${pass.id}"),
                    animatedVisibilityScope = animatedVisibilityScope
                )
                .clickable(onClick = onClick),
            colors = CardDefaults.cardColors(
                containerColor = backgroundColor
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Spacer to push content down
                Spacer(modifier = Modifier.weight(1f))

                // Logo (prioritize logo over icon)
                val logoPath = pass.logoPath ?: pass.iconPath
                val logoFile = File(logoPath)
                if (logoFile.exists()) {
                    val bitmap = BitmapFactory.decodeFile(logoFile.absolutePath)
                    bitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .height(60.dp)
                                .widthIn(max = 120.dp)
                                .sharedElement(
                                    rememberSharedContentState(key = "icon-${pass.id}"),
                                    animatedVisibilityScope = animatedVisibilityScope
                                ),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                // Spacer to push text to bottom
                Spacer(modifier = Modifier.weight(1f))

                // Organization name with HTML stripped - at bottom
                Text(
                    text = stripHtml(pass.organizationName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}


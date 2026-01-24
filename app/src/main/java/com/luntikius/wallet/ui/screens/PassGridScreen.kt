package com.luntikius.wallet.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luntikius.wallet.data.model.Pass
import com.luntikius.wallet.ui.components.DeleteZone
import com.luntikius.wallet.ui.components.PassGridSkeleton
import com.luntikius.wallet.ui.utils.ensureContrast
import com.luntikius.wallet.ui.utils.parseColor
import com.luntikius.wallet.ui.utils.stripHtml
import com.luntikius.wallet.ui.viewmodel.ImportStatus
import com.luntikius.wallet.ui.viewmodel.PassViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
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
    val isInitialLoading by viewModel.isInitialLoading.collectAsState()
    var selectedPassId by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current

    // Local state for optimistic UI updates during drag
    var localPasses by remember { mutableStateOf<List<Pass>>(emptyList()) }

    // Sync local state with ViewModel
    LaunchedEffect(passes) {
        localPasses = passes
    }

    // Lazy grid state for reorderable
    val gridState = rememberLazyGridState()

    // Delete zone state
    var deleteZoneTop by remember { mutableStateOf(0f) }
    var deleteZoneBottom by remember { mutableStateOf(0f) }
    var deleteZoneLeft by remember { mutableStateOf(0f) }
    var deleteZoneRight by remember { mutableStateOf(0f) }
    var fingerPositionY by remember { mutableStateOf(0f) }
    var fingerPositionX by remember { mutableStateOf(0f) }
    var passToDelete by remember { mutableStateOf<Pass?>(null) }
    var isDragging by remember { mutableStateOf(false) }
    var dragStartedPasses by remember { mutableStateOf<List<Pass>>(emptyList()) }

    // Check if finger position is over delete zone
    val isOverDeleteZone = isDragging &&
                           fingerPositionY > deleteZoneTop &&
                           fingerPositionY < deleteZoneBottom &&
                           fingerPositionX > deleteZoneLeft &&
                           fingerPositionX < deleteZoneRight

    // Reorderable state with move callback
    val reorderableState = rememberReorderableLazyGridState(gridState) { from, to ->
        // Only allow reordering when not importing and not over delete zone
        if (importStatus !is ImportStatus.Loading && !isOverDeleteZone) {
            localPasses = localPasses.toMutableList().apply {
                add(to.index, removeAt(from.index))
            }
        }
    }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("wallet") },
                actions = {
                    IconButton(onClick = { pickFileLauncher.launch("*/*") }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Pass"
                        )
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            // Track finger position without consuming events
                            event.changes.firstOrNull()?.let { change ->
                                fingerPositionX = change.position.x
                                fingerPositionY = change.position.y + paddingValues.calculateTopPadding().toPx()
                            }
                        }
                    }
                }
        ) {
            if (isInitialLoading) {
                // Loading skeleton while fetching initial data
                PassGridSkeleton()
            } else if (localPasses.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
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
                    state = gridState,
                    columns = GridCells.Adaptive(160.dp),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = 16.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                items(localPasses, key = { it.id }) { pass ->
                    ReorderableItem(reorderableState, key = pass.id) { itemIsDragging ->
                        // Track when any item starts/stops dragging
                        LaunchedEffect(itemIsDragging) {
                            if (itemIsDragging) {
                                isDragging = true
                                passToDelete = pass
                                dragStartedPasses = localPasses
                            } else if (isDragging && passToDelete?.id == pass.id) {
                                // This item just stopped dragging (finger lifted)
                                isDragging = false

                                // Handle deletion or reordering based on where finger was lifted
                                if (isOverDeleteZone && passToDelete != null) {
                                    // Delete the pass - finger was lifted over delete zone
                                    viewModel.deletePass(passToDelete!!)
                                    localPasses = localPasses.filter { it.id != passToDelete!!.id }
                                    // Trigger haptic feedback
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                } else if (localPasses != dragStartedPasses) {
                                    // Reorder passes (only if order changed and not deleted)
                                    viewModel.updatePassOrder(
                                        localPasses.mapIndexed { index, p ->
                                            p.id to index
                                        }.toMap()
                                    )
                                }

                                // Reset state
                                passToDelete = null
                                fingerPositionY = 0f
                                fingerPositionX = 0f
                                dragStartedPasses = emptyList()
                            }
                        }

                        PassTile(
                            pass = pass,
                            isDragging = itemIsDragging,
                            modifier = Modifier
                                .longPressDraggableHandle(
                                    onDragStarted = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDragStopped = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                ),
                            onClick = { selectedPassId = pass.id },
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    }
                    }
                }
            }

            // Delete zone at bottom (only visible during drag)
            DeleteZone(
                isVisible = isDragging,
                isHovering = isOverDeleteZone,
                onPositioned = { left, top, right, bottom ->
                    deleteZoneLeft = left
                    deleteZoneTop = top
                    deleteZoneRight = right
                    deleteZoneBottom = bottom
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )

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
    isDragging: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
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
                .scale(if (isDragging) 1.05f else 1f)
                .sharedElement(
                    rememberSharedContentState(key = "card-${pass.id}"),
                    animatedVisibilityScope = animatedVisibilityScope
                ),
            colors = CardDefaults.cardColors(
                containerColor = backgroundColor
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isDragging) 16.dp else 4.dp
            ),
            shape = RoundedCornerShape(12.dp),
            onClick = onClick,
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


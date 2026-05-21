package com.luntikius.wallet.wear.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onPreRotaryScrollEvent
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.RotarySnapLayoutInfoProvider
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import kotlin.math.abs
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
internal fun Modifier.lazyListRotarySnapScrollable(
    listState: LazyListState,
): Modifier {
    val focusRequester = remember { FocusRequester() }
    val rotaryBehavior = RotaryScrollableDefaults.snapBehavior(
        scrollableState = listState,
        layoutInfoProvider = remember(listState) { LazyListRotarySnapLayoutInfoProvider(listState) },
    )

    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }

    return rotaryScrollable(
        behavior = rotaryBehavior,
        focusRequester = focusRequester,
    )
        .focusRequester(focusRequester)
        .focusable()
}

@Composable
internal fun Modifier.qrDetailRotaryScrollable(
    listState: LazyListState,
    headerScrollState: ScrollState,
    showHeaderPage: Boolean,
    qrPageIndex: Int,
    headerPageIndex: Int,
): Modifier {
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val rotaryJob = remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }

    fun launchRotaryAction(block: suspend () -> Unit) {
        if (rotaryJob.value?.isActive == true) return
        rotaryJob.value = coroutineScope.launch { block() }
    }

    return onPreRotaryScrollEvent { event ->
        if (!showHeaderPage) return@onPreRotaryScrollEvent false

        val delta = event.verticalScrollPixels.takeIf { it != 0f } ?: event.horizontalScrollPixels
        val direction = delta.toRotaryScrollDirection() ?: return@onPreRotaryScrollEvent false
        val centeredPageIndex = listState.closestItemIndexToCenter()

        when (direction) {
            RotaryScrollDirection.Down -> {
                if (centeredPageIndex < headerPageIndex) {
                    launchRotaryAction { listState.animateScrollToItem(headerPageIndex) }
                } else if (headerScrollState.canScrollForward) {
                    launchRotaryAction { headerScrollState.scrollBy(-delta) }
                }
                true
            }

            RotaryScrollDirection.Up -> {
                if (centeredPageIndex == headerPageIndex && headerScrollState.canScrollBackward) {
                    launchRotaryAction { headerScrollState.scrollBy(-delta) }
                } else if (centeredPageIndex > qrPageIndex) {
                    launchRotaryAction { listState.animateScrollToItem(qrPageIndex) }
                }
                true
            }
        }
    }
        .focusRequester(focusRequester)
        .focusable()
}

@Composable
internal fun CenteredPageChangeEffect(listState: LazyListState, enabled: Boolean, onPageChanged: () -> Unit) {
    val currentOnPageChanged by rememberUpdatedState(onPageChanged)

    LaunchedEffect(listState, enabled) {
        if (!enabled) return@LaunchedEffect

        var previousPage: Int? = null
        snapshotFlow { listState.closestItemIndexToCenter() }
            .distinctUntilChanged()
            .collect { page ->
                val previous = previousPage
                if (previous != null && previous != page) {
                    currentOnPageChanged()
                }
                previousPage = page
            }
    }
}

internal fun LazyListState.closestItemIndexToCenter(): Int {
    val layoutInfo = layoutInfo
    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
    return layoutInfo.visibleItemsInfo
        .minByOrNull { item -> abs(item.center - viewportCenter) }
        ?.index
        ?: firstVisibleItemIndex
}

private class LazyListRotarySnapLayoutInfoProvider(private val listState: LazyListState) :
    RotarySnapLayoutInfoProvider {
    override val averageItemSize: Float
        get() {
            val visibleItems = listState.layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) return 0f

            val centerDistances = visibleItems
                .sortedBy(LazyListItemInfo::index)
                .zipWithNext { current, next ->
                    abs(next.center - current.center)
                }

            return if (centerDistances.isNotEmpty()) {
                centerDistances.average().toFloat()
            } else {
                visibleItems.first().size.toFloat()
            }
        }

    override val currentItemIndex: Int
        get() = listState.closestItemInfoToCenter()?.index ?: listState.firstVisibleItemIndex

    override val currentItemOffset: Float
        get() {
            val itemInfo = listState.closestItemInfoToCenter() ?: return 0f
            return itemInfo.center - listState.viewportCenter
        }

    override val totalItemCount: Int
        get() = listState.layoutInfo.totalItemsCount
}

private fun LazyListState.closestItemInfoToCenter(): LazyListItemInfo? {
    val viewportCenter = viewportCenter
    return layoutInfo.visibleItemsInfo.minByOrNull { item ->
        abs(item.center - viewportCenter)
    }
}

private val LazyListState.viewportCenter: Float
    get() = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f

private val LazyListItemInfo.center: Float
    get() = offset + size / 2f

private enum class RotaryScrollDirection {
    Up,
    Down,
}

private fun Float.toRotaryScrollDirection(): RotaryScrollDirection? = when {
    this < 0f -> RotaryScrollDirection.Down
    this > 0f -> RotaryScrollDirection.Up
    else -> null
}

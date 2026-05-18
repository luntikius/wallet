package com.luntikius.wallet.wear.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.wear.compose.foundation.rotary.RotaryScrollableBehavior
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.RotarySnapLayoutInfoProvider
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
internal fun Modifier.lazyListRotaryScrollable(
    listState: LazyListState,
    nestedScrollState: ScrollState? = null,
    nestedScrollItemIndex: Int? = null,
): Modifier {
    val focusRequester = remember { FocusRequester() }
    val listRotaryBehavior = RotaryScrollableDefaults.snapBehavior(
        scrollableState = listState,
        layoutInfoProvider = remember(listState) { LazyListRotarySnapLayoutInfoProvider(listState) },
    )
    val nestedRotaryBehavior = nestedScrollState?.let { scrollState ->
        RotaryScrollableDefaults.behavior(
            scrollableState = scrollState,
            flingBehavior = null,
        )
    }
    val rotaryBehavior = remember(listRotaryBehavior, nestedRotaryBehavior, nestedScrollState, nestedScrollItemIndex) {
        NestedAwareRotaryScrollableBehavior(
            listState = listState,
            listBehavior = listRotaryBehavior,
            nestedScrollState = nestedScrollState,
            nestedBehavior = nestedRotaryBehavior,
            nestedScrollItemIndex = nestedScrollItemIndex,
        )
    }

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

private fun LazyListState.closestItemIndexToCenter(): Int {
    val layoutInfo = layoutInfo
    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
    return layoutInfo.visibleItemsInfo
        .minByOrNull { item -> abs(item.offset + item.size / 2f - viewportCenter) }
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
            return listState.viewportCenter - itemInfo.center
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

private class NestedAwareRotaryScrollableBehavior(
    private val listState: LazyListState,
    private val listBehavior: RotaryScrollableBehavior,
    private val nestedScrollState: ScrollState?,
    private val nestedBehavior: RotaryScrollableBehavior?,
    private val nestedScrollItemIndex: Int?,
) : RotaryScrollableBehavior {
    override suspend fun CoroutineScope.performScroll(
        timestampMillis: Long,
        delta: Float,
        inputDeviceId: Int,
        orientation: androidx.compose.foundation.gestures.Orientation,
    ) {
        val scrollState = nestedScrollState
        val scrollBehavior = nestedBehavior
        val scrollItemIndex = nestedScrollItemIndex

        if (
            scrollState != null &&
            scrollBehavior != null &&
            scrollItemIndex != null &&
            listState.closestItemIndexToCenter() == scrollItemIndex &&
            scrollState.canConsumeRotaryDelta(delta)
        ) {
            with(scrollBehavior) {
                performScroll(
                    timestampMillis = timestampMillis,
                    delta = delta,
                    inputDeviceId = inputDeviceId,
                    orientation = orientation,
                )
            }
        } else {
            with(listBehavior) {
                performScroll(
                    timestampMillis = timestampMillis,
                    delta = delta,
                    inputDeviceId = inputDeviceId,
                    orientation = orientation,
                )
            }
        }
    }
}

private fun ScrollState.canConsumeRotaryDelta(delta: Float): Boolean = when {
    delta > 0f -> canScrollForward
    delta < 0f -> canScrollBackward
    else -> false
}

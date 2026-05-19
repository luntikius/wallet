package com.luntikius.wallet.wear.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.FlingBehavior
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
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
internal fun Modifier.lazyListRotaryScrollable(
    listState: LazyListState,
    flingBehavior: FlingBehavior,
    nestedScrollState: ScrollState? = null,
    nestedScrollItemIndex: Int? = null,
): Modifier {
    val focusRequester = remember { FocusRequester() }
    val listRotaryBehavior = RotaryScrollableDefaults.behavior(
        scrollableState = listState,
        flingBehavior = flingBehavior,
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

        if (shouldRouteToNestedScroll(scrollState, scrollBehavior, scrollItemIndex, delta)) {
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

    private fun shouldRouteToNestedScroll(
        scrollState: ScrollState?,
        scrollBehavior: RotaryScrollableBehavior?,
        scrollItemIndex: Int?,
        delta: Float,
    ): Boolean {
        if (scrollState == null || scrollBehavior == null || scrollItemIndex == null) return false
        if (listState.closestItemIndexToCenter() != scrollItemIndex) return false
        return scrollState.canConsumeRotaryDelta(delta)
    }
}

private fun ScrollState.canConsumeRotaryDelta(delta: Float): Boolean = when {
    delta > 0f -> canScrollForward
    delta < 0f -> canScrollBackward
    else -> false
}

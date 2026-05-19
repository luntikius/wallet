package com.luntikius.wallet.wear.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onPreRotaryScrollEvent
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import kotlin.math.abs
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
internal fun Modifier.lazyListRotaryScrollable(
    listState: LazyListState,
    flingBehavior: FlingBehavior,
    nestedScrollState: ScrollState? = null,
    nestedScrollItemIndex: Int? = null,
): Modifier {
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val listRotaryBehavior = RotaryScrollableDefaults.behavior(
        scrollableState = listState,
        flingBehavior = flingBehavior,
    )

    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }

    return routeRotaryToNestedScroll(
        listState = listState,
        nestedScrollState = nestedScrollState,
        nestedScrollItemIndex = nestedScrollItemIndex,
        onNestedDelta = { delta ->
            coroutineScope.launch {
                nestedScrollState?.scrollBy(-delta)
            }
        },
    )
        .rotaryScrollable(
            behavior = listRotaryBehavior,
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

private fun Modifier.routeRotaryToNestedScroll(
    listState: LazyListState,
    nestedScrollState: ScrollState?,
    nestedScrollItemIndex: Int?,
    onNestedDelta: (Float) -> Unit,
): Modifier = onPreRotaryScrollEvent { event ->
    val delta = event.verticalScrollPixels.takeIf { it != 0f } ?: event.horizontalScrollPixels
    if (
        nestedScrollState != null &&
        nestedScrollItemIndex != null &&
        listState.closestItemIndexToCenter() == nestedScrollItemIndex &&
        nestedScrollState.canConsumeRotaryDelta(delta)
    ) {
        onNestedDelta(delta)
        true
    } else {
        false
    }
}

private fun ScrollState.canConsumeRotaryDelta(delta: Float): Boolean = when {
    delta > 0f -> canScrollBackward
    delta < 0f -> canScrollForward
    else -> false
}

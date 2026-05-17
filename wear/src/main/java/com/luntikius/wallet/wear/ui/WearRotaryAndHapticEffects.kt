package com.luntikius.wallet.wear.ui

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
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import kotlin.math.abs
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
internal fun Modifier.lazyListRotaryScrollable(listState: LazyListState, flingBehavior: FlingBehavior): Modifier {
    val focusRequester = remember { FocusRequester() }
    val rotaryBehavior = RotaryScrollableDefaults.behavior(
        scrollableState = listState,
        flingBehavior = flingBehavior,
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

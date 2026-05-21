package com.luntikius.wallet.wear.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.RotarySnapLayoutInfoProvider
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import kotlin.math.abs

@Composable
internal fun Modifier.passListRotaryScrollable(
    listState: LazyListState,
): Modifier {
    val focusRequester = remember { FocusRequester() }
    val rotaryBehavior = RotaryScrollableDefaults.snapBehavior(
        scrollableState = listState,
        layoutInfoProvider = remember(listState) {
            PassListRotarySnapLayoutInfoProvider(listState)
        },
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

private class PassListRotarySnapLayoutInfoProvider(
    private val listState: LazyListState,
) : RotarySnapLayoutInfoProvider {
    override val averageItemSize: Float
        get() = listState.layoutInfo.visibleItemsInfo.averageCenterToCenterDistance()

    override val currentItemIndex: Int
        get() = listState.closestItemInfoToCenter()?.index ?: listState.firstVisibleItemIndex

    override val currentItemOffset: Float
        get() {
            val centeredItem = listState.closestItemInfoToCenter() ?: return 0f
            return listState.viewportCenter - centeredItem.center
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

private fun List<LazyListItemInfo>.averageCenterToCenterDistance(): Float {
    if (isEmpty()) return 0f

    val distances = sortedBy(LazyListItemInfo::index)
        .zipWithNext { current, next -> abs(next.center - current.center) }

    return distances
        .takeIf { it.isNotEmpty() }
        ?.average()
        ?.toFloat()
        ?: first().size.toFloat()
}

private val LazyListState.viewportCenter: Float
    get() = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f

private val LazyListItemInfo.center: Float
    get() = offset + size / 2f

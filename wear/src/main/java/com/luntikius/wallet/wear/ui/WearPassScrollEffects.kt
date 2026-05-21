package com.luntikius.wallet.wear.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.pager.PagerState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.rotary.onPreRotaryScrollEvent
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.RotarySnapLayoutInfoProvider
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import kotlin.math.abs
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
internal fun Modifier.wearPassRotaryScrollable(
    pagerState: PagerState,
    headerScrollState: ScrollState,
    showHeaderPage: Boolean,
    headerPageIndex: Int,
): Modifier {
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val rotaryBehavior = RotaryScrollableDefaults.snapBehavior(
        scrollableState = pagerState,
        layoutInfoProvider = remember(pagerState) {
            WearPassRotarySnapLayoutInfoProvider(pagerState)
        },
    )

    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }

    return onPreRotaryScrollEvent { event ->
        val delta = event.verticalScrollPixels.takeIf { it != 0f } ?: event.horizontalScrollPixels
        val shouldScrollHeader = showHeaderPage &&
            pagerState.isPageSettledAt(headerPageIndex) &&
            headerScrollState.canConsumeScrollDelta(delta)

        if (!shouldScrollHeader) return@onPreRotaryScrollEvent false

        coroutineScope.launch {
            headerScrollState.scrollBy(delta)
        }
        true
    }
        .rotaryScrollable(
            behavior = rotaryBehavior,
            focusRequester = focusRequester,
        )
        .focusRequester(focusRequester)
        .focusable()
}

@Composable
internal fun rememberWearPassHeaderTouchScrollConnection(
    pagerState: PagerState,
    headerScrollState: ScrollState,
    showHeaderPage: Boolean,
    headerPageIndex: Int,
): NestedScrollConnection = remember(pagerState, headerScrollState, showHeaderPage, headerPageIndex) {
    object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (source != NestedScrollSource.UserInput || available.y == 0f) return Offset.Zero
            if (!showHeaderPage || !pagerState.isPageAlignedAt(headerPageIndex)) return Offset.Zero

            val scrollDelta = -available.y
            if (!headerScrollState.canConsumeScrollDelta(scrollDelta)) return Offset.Zero

            val consumedScroll = headerScrollState.dispatchRawDelta(scrollDelta)
            return Offset(x = 0f, y = -consumedScroll)
        }
    }
}

@Composable
internal fun CenteredPassPageChangeEffect(
    pagerState: PagerState,
    enabled: Boolean,
    onPageChanged: () -> Unit,
) {
    val currentOnPageChanged by rememberUpdatedState(onPageChanged)

    LaunchedEffect(pagerState, enabled) {
        if (!enabled) return@LaunchedEffect

        var previousPage: Int? = null
        snapshotFlow { pagerState.settledPage }
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

internal fun PagerState.isPageSettledAt(index: Int): Boolean =
    !isScrollInProgress &&
        isPageAlignedAt(index)

internal fun PagerState.isPageAlignedAt(index: Int): Boolean =
    currentPage == index &&
        abs(currentPageOffsetFraction) <= SettledPageTolerance

internal fun PagerState.pagePosition(): Float =
    (currentPage + currentPageOffsetFraction).coerceIn(0f, (pageCount - 1).coerceAtLeast(0).toFloat())

internal fun PagerState.pageOffsetFrom(page: Int): Float =
    ((currentPage - page) + currentPageOffsetFraction).coerceIn(-1f, 1f)

private class WearPassRotarySnapLayoutInfoProvider(
    private val pagerState: PagerState,
) : RotarySnapLayoutInfoProvider {
    override val averageItemSize: Float
        get() = pagerState.pageSizeWithSpacing()

    override val currentItemIndex: Int
        get() = pagerState.currentPage

    override val currentItemOffset: Float
        get() = pagerState.currentPageOffsetFraction * averageItemSize

    override val totalItemCount: Int
        get() = pagerState.pageCount
}

private fun PagerState.pageSizeWithSpacing(): Float {
    val layoutInfo = layoutInfo
    return (layoutInfo.pageSize + layoutInfo.pageSpacing)
        .takeIf { it > 0 }
        ?.toFloat()
        ?: layoutInfo.viewportSize.height.toFloat()
}

private fun ScrollState.canConsumeScrollDelta(delta: Float): Boolean = when {
    delta > 0f -> canScrollForward
    delta < 0f -> canScrollBackward
    else -> false
}

private const val SettledPageTolerance = 0.001f

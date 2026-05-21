package com.luntikius.wallet.wear.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

private val ListScrollTrackColor = Color.White.copy(alpha = 0.22f)
private val ListScrollThumbColor = Color.White.copy(alpha = 0.82f)

@Composable
internal fun ScrollAffordance(unfoldProgress: Float, color: Color, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier.size(width = 34.dp, height = 18.dp),
    ) {
        val strokeWidth = 3.dp.toPx()
        val arrowLeft = Offset(size.width * 0.28f, size.height * 0.62f)
        val arrowTip = Offset(size.width * 0.5f, size.height * 0.32f)
        val arrowRight = Offset(size.width * 0.72f, size.height * 0.62f)
        val lineLeft = Offset(size.width * 0.24f, size.height * 0.5f)
        val lineCenter = Offset(size.width * 0.5f, size.height * 0.5f)
        val lineRight = Offset(size.width * 0.76f, size.height * 0.5f)
        val progress = unfoldProgress.coerceIn(0f, 1f)

        drawLine(
            color = color,
            start = lerpOffset(arrowLeft, lineLeft, progress),
            end = lerpOffset(arrowTip, lineCenter, progress),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = lerpOffset(arrowTip, lineCenter, progress),
            end = lerpOffset(arrowRight, lineRight, progress),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
internal fun AutoHidingScrollPositionIndicator(
    listState: LazyListState,
    itemCount: Int,
    nestedScrollState: ScrollState? = null,
    nestedScrollItemIndex: Int? = null,
    modifier: Modifier = Modifier,
) {
    if (itemCount <= 1) return

    var isVisible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        label = "wear-scroll-indicator-alpha",
    )

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress || nestedScrollState?.isScrollInProgress == true }
            .distinctUntilChanged()
            .collect { isScrolling ->
                if (isScrolling) {
                    isVisible = true
                } else {
                    delay(900L)
                    isVisible = false
                }
            }
    }

    if (alpha <= 0.01f) return

    Canvas(modifier = modifier) {
        val strokeWidth = 3.dp.toPx()
        val inset = strokeWidth / 2 + 3.dp.toPx()
        val arcSize = Size(
            width = size.width - inset * 2,
            height = size.height - inset * 2,
        )
        val progress = listScrollPosition(listState = listState, itemCount = itemCount)
            .let { position -> (position / (itemCount - 1).coerceAtLeast(1)).coerceIn(0f, 1f) }
            .withNestedProgress(
                itemCount = itemCount,
                nestedScrollState = nestedScrollState,
                nestedScrollItemIndex = nestedScrollItemIndex,
                listState = listState,
            )
        val trackStartAngle = -46f
        val trackSweepAngle = 92f
        val thumbSweepAngle = 18f
        val thumbStartAngle = trackStartAngle + (trackSweepAngle - thumbSweepAngle) * progress

        drawArc(
            color = ListScrollTrackColor.copy(alpha = ListScrollTrackColor.alpha * alpha),
            startAngle = trackStartAngle,
            sweepAngle = trackSweepAngle,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
        drawArc(
            color = ListScrollThumbColor.copy(alpha = ListScrollThumbColor.alpha * alpha),
            startAngle = thumbStartAngle,
            sweepAngle = thumbSweepAngle,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
    }
}

@Composable
internal fun AutoHidingPagerScrollPositionIndicator(
    pagerState: PagerState,
    itemCount: Int,
    nestedScrollState: ScrollState? = null,
    nestedScrollItemIndex: Int? = null,
    modifier: Modifier = Modifier,
) {
    if (itemCount <= 1) return

    var isVisible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        label = "wear-pager-scroll-indicator-alpha",
    )

    LaunchedEffect(pagerState, nestedScrollState) {
        snapshotFlow { pagerState.isScrollInProgress || nestedScrollState?.isScrollInProgress == true }
            .distinctUntilChanged()
            .collect { isScrolling ->
                if (isScrolling) {
                    isVisible = true
                } else {
                    delay(900L)
                    isVisible = false
                }
            }
    }

    LaunchedEffect(nestedScrollState?.value) {
        if (nestedScrollState == null) return@LaunchedEffect

        isVisible = true
        delay(900L)
        isVisible = false
    }

    if (alpha <= 0.01f) return

    Canvas(modifier = modifier) {
        val strokeWidth = 3.dp.toPx()
        val inset = strokeWidth / 2 + 3.dp.toPx()
        val arcSize = Size(
            width = size.width - inset * 2,
            height = size.height - inset * 2,
        )
        val progress = pagerScrollProgress(
            pagerState = pagerState,
            itemCount = itemCount,
            nestedScrollState = nestedScrollState,
            nestedScrollItemIndex = nestedScrollItemIndex,
        )
        val trackStartAngle = -46f
        val trackSweepAngle = 92f
        val thumbSweepAngle = 18f
        val thumbStartAngle = trackStartAngle + (trackSweepAngle - thumbSweepAngle) * progress

        drawArc(
            color = ListScrollTrackColor.copy(alpha = ListScrollTrackColor.alpha * alpha),
            startAngle = trackStartAngle,
            sweepAngle = trackSweepAngle,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
        drawArc(
            color = ListScrollThumbColor.copy(alpha = ListScrollThumbColor.alpha * alpha),
            startAngle = thumbStartAngle,
            sweepAngle = thumbSweepAngle,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
    }
}

@Composable
internal fun KeepScreenBrightness(isEnabled: Boolean, brightness: Float) {
    val activity = LocalContext.current.findActivity()
    val originalBrightness = remember(activity) {
        activity?.window?.attributes?.screenBrightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
    }

    LaunchedEffect(activity, isEnabled, brightness, originalBrightness) {
        activity?.setScreenBrightness(if (isEnabled) brightness else originalBrightness)
    }

    DisposableEffect(activity, originalBrightness) {
        onDispose {
            activity?.setScreenBrightness(originalBrightness)
        }
    }
}

@Composable
internal fun KeepScreenOn(isEnabled: Boolean) {
    val activity = LocalContext.current.findActivity()

    DisposableEffect(activity, isEnabled) {
        val window = activity?.window
        val keepScreenOnFlag = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        val hadKeepScreenOn = (window?.attributes?.flags?.and(keepScreenOnFlag) ?: 0) != 0

        if (isEnabled) {
            window?.addFlags(keepScreenOnFlag)
        }

        onDispose {
            if (isEnabled && !hadKeepScreenOn) {
                window?.clearFlags(keepScreenOnFlag)
            }
        }
    }
}

internal fun listItemScale(listState: LazyListState, index: Int): Float {
    val layoutInfo = listState.layoutInfo
    val itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return 0.84f
    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
    val itemCenter = itemInfo.offset + itemInfo.size / 2f
    val viewportHalfHeight = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2f
    if (viewportHalfHeight <= 0f) return 1f

    val distanceFromCenter = abs(itemCenter - viewportCenter)
    val distanceFraction = (distanceFromCenter / viewportHalfHeight).coerceIn(0f, 1f)
    return 1f - (distanceFraction * 0.16f)
}

private fun listScrollPosition(listState: LazyListState, itemCount: Int): Float {
    val layoutInfo = listState.layoutInfo
    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
    val itemInfo = layoutInfo.visibleItemsInfo.minByOrNull { item ->
        abs(item.offset + item.size / 2f - viewportCenter)
    } ?: return listState.firstVisibleItemIndex.toFloat().coerceIn(0f, (itemCount - 1).toFloat())

    val centerDistance = itemInfo.offset + itemInfo.size / 2f - viewportCenter
    val centerOffsetProgress = (-centerDistance / itemInfo.size).coerceIn(-0.5f, 0.5f)
    return (itemInfo.index + centerOffsetProgress).coerceIn(0f, (itemCount - 1).toFloat())
}

private fun Float.withNestedProgress(
    itemCount: Int,
    nestedScrollState: ScrollState?,
    nestedScrollItemIndex: Int?,
    listState: LazyListState,
): Float {
    if (nestedScrollState == null || nestedScrollItemIndex == null || itemCount <= 1) return this
    if (nestedScrollState.maxValue <= 0) return this

    val listPosition = listScrollPosition(listState = listState, itemCount = itemCount)
    if (listPosition < nestedScrollItemIndex) {
        return (listPosition / itemCount).coerceIn(0f, 1f)
    }

    val nestedProgress = nestedScrollState.value.toFloat() / nestedScrollState.maxValue
    return ((nestedScrollItemIndex + nestedProgress) / itemCount).coerceIn(0f, 1f)
}

private fun pagerScrollProgress(
    pagerState: PagerState,
    itemCount: Int,
    nestedScrollState: ScrollState?,
    nestedScrollItemIndex: Int?,
): Float {
    if (itemCount <= 1) return 0f

    val pagePosition = pagerState.pagePosition()
    if (nestedScrollState == null || nestedScrollItemIndex == null || nestedScrollState.maxValue <= 0) {
        return (pagePosition / (itemCount - 1).coerceAtLeast(1)).coerceIn(0f, 1f)
    }

    if (pagePosition < nestedScrollItemIndex) {
        return (pagePosition / itemCount).coerceIn(0f, 1f)
    }

    val nestedProgress = nestedScrollState.value.toFloat() / nestedScrollState.maxValue
    return ((nestedScrollItemIndex + nestedProgress) / itemCount).coerceIn(0f, 1f)
}

private fun Activity.setScreenBrightness(value: Float) {
    val layoutParams = window.attributes
    layoutParams.screenBrightness = value
    window.attributes = layoutParams
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun lerpOffset(start: Offset, end: Offset, fraction: Float): Offset = Offset(
    x = start.x + (end.x - start.x) * fraction,
    y = start.y + (end.y - start.y) * fraction,
)

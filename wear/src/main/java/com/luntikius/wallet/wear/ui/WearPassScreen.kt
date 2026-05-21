package com.luntikius.wallet.wear.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.ScreenScaffold
import com.luntikius.wallet.wear.data.CachedWearPass
import kotlin.math.abs

private const val QR_PAGE_INDEX = 0
private const val HEADER_PAGE_INDEX = 1
private const val PASS_PAGE_COUNT = 2
private val PassPageSpacing = 24.dp

@Composable
internal fun WearPassScreen(pass: CachedWearPass, onOpenPassOnPhone: (String) -> Unit) {
    val pagerState = rememberPagerState(initialPage = QR_PAGE_INDEX, pageCount = { PASS_PAGE_COUNT })
    val headerScrollState = rememberScrollState()
    val pagePosition = pagerState.pagePosition()
    val isHeaderPage = pagerState.isPageSettledAt(HEADER_PAGE_INDEX)
    val headerColor = remember(pass.snapshot.backgroundColor) {
        parseWearColor(pass.snapshot.backgroundColor, Color(0xFF0077B6))
    }
    val headerIndicatorColor = remember(pass.snapshot.foregroundColor, headerColor) {
        readableColor(
            foreground = parseWearColor(pass.snapshot.foregroundColor, Color.White),
            background = headerColor,
        )
    }

    KeepScreenBrightness(isEnabled = pagerState.isPageSettledAt(QR_PAGE_INDEX), brightness = 0.8f)
    KeepScreenOn(isEnabled = pagerState.isPageSettledAt(QR_PAGE_INDEX))

    ScreenScaffold(
        scrollState = headerScrollState,
        timeText = null,
        scrollIndicator = null,
        modifier = Modifier
            .fillMaxSize()
            .background(DetailBackground),
        contentPadding = PaddingValues(),
    ) { contentPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            VerticalPager(
                state = pagerState,
                flingBehavior = PagerDefaults.flingBehavior(
                    state = pagerState,
                    pagerSnapDistance = PagerSnapDistance.atMost(1),
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(
                        rememberWearPassHeaderTouchScrollConnection(
                            pagerState = pagerState,
                            headerScrollState = headerScrollState,
                            headerPageIndex = HEADER_PAGE_INDEX,
                        ),
                    ),
                contentPadding = contentPadding,
                pageSpacing = PassPageSpacing,
            ) { page ->
                when (page) {
                    QR_PAGE_INDEX -> ScaledPagerPage(pagerState = pagerState, page = page) {
                        PassQrCard(
                            pass = pass,
                            scrollHintProgress = (pagePosition * 2f).coerceIn(0f, 1f),
                            showScrollHint = true,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    HEADER_PAGE_INDEX -> ScaledPagerPage(pagerState = pagerState, page = page) {
                        PassHeaderCard(
                            pass = pass,
                            scrollState = headerScrollState,
                            isRotaryEnabled = isHeaderPage,
                            onOpenPassOnPhone = { onOpenPassOnPhone(pass.snapshot.id) },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            PassCurvedScrollIndicator(
                pageProgress = pagePosition,
                headerScrollState = headerScrollState,
                headerColor = headerIndicatorColor,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun ScaledPagerPage(
    pagerState: PagerState,
    page: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val distance = abs(pagerState.pageOffsetFrom(page)).coerceIn(0f, 1f)
    val scale by animateFloatAsState(
        targetValue = 1f - distance * 0.16f,
        label = "wear-pass-page-scale",
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(scale),
        ) {
            content()
        }
    }
}

private fun PagerState.pageOffsetFrom(page: Int): Float = (currentPage - page) + currentPageOffsetFraction

package com.luntikius.wallet.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.luntikius.wallet.wear.data.CachedWearPass
import com.luntikius.wallet.wearsync.WearPassFieldSection

private val FrontCardFieldSections = setOf(
    WearPassFieldSection.HEADER,
    WearPassFieldSection.PRIMARY,
    WearPassFieldSection.SECONDARY,
    WearPassFieldSection.AUXILIARY,
)
private const val CUSTOM_PASS_FORMAT = "CUSTOM"
private const val QR_PAGE_INDEX = 0
private const val HEADER_PAGE_INDEX = 1

@Composable
internal fun WearPassScreen(
    pass: CachedWearPass,
    onOpenPassOnPhone: (String) -> Unit,
    onPageChanged: () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(DetailBackground),
    ) {
        val headerScrollState = rememberScrollState()
        val viewportHeight = maxHeight
        val showHeaderPage = pass.snapshot.format != CUSTOM_PASS_FORMAT
        val detailPageCount = if (showHeaderPage) 2 else 1
        val pagerState = rememberPagerState(pageCount = { detailPageCount })
        val pagerFlingBehavior = PagerDefaults.flingBehavior(
            state = pagerState,
            pagerSnapDistance = PagerSnapDistance.atMost(1),
        )
        val isQrPageCentered by remember(pagerState) {
            derivedStateOf { pagerState.isPageSettledAt(QR_PAGE_INDEX) }
        }
        val headerTouchScrollConnection = rememberWearPassHeaderTouchScrollConnection(
            pagerState = pagerState,
            headerScrollState = headerScrollState,
            showHeaderPage = showHeaderPage,
            headerPageIndex = HEADER_PAGE_INDEX,
        )
        val scrollHintProgress by remember(pagerState, showHeaderPage) {
            derivedStateOf {
                if (showHeaderPage) {
                    pagerState.pagePosition().coerceIn(0f, 1f)
                } else {
                    0f
                }
            }
        }

        KeepScreenBrightness(isEnabled = isQrPageCentered, brightness = 0.8f)
        KeepScreenOn(isEnabled = isQrPageCentered)
        CenteredPassPageChangeEffect(
            pagerState = pagerState,
            enabled = detailPageCount > 1,
            onPageChanged = onPageChanged,
        )

        VerticalPager(
            state = pagerState,
            flingBehavior = pagerFlingBehavior,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(headerTouchScrollConnection)
                .wearPassRotaryScrollable(
                    pagerState = pagerState,
                    headerScrollState = headerScrollState,
                    showHeaderPage = showHeaderPage,
                    headerPageIndex = HEADER_PAGE_INDEX,
                ),
            contentPadding = PaddingValues(),
        ) { page ->
            when (page) {
                QR_PAGE_INDEX -> ScalableWearPagerItem(
                    pagerState = pagerState,
                    page = QR_PAGE_INDEX,
                    modifier = Modifier.height(viewportHeight),
                ) {
                    PassQrCard(
                        pass = pass,
                        scrollHintProgress = scrollHintProgress,
                        showScrollHint = showHeaderPage,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                HEADER_PAGE_INDEX -> ScalableWearPagerItem(
                    pagerState = pagerState,
                    page = HEADER_PAGE_INDEX,
                    modifier = Modifier.height(viewportHeight),
                ) {
                    PassHeaderCard(
                        pass = pass,
                        fields = pass.snapshot.fields.filter { it.section in FrontCardFieldSections },
                        scrollState = headerScrollState,
                        onOpenPassOnPhone = { onOpenPassOnPhone(pass.snapshot.id) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        AutoHidingPagerScrollPositionIndicator(
            pagerState = pagerState,
            itemCount = detailPageCount,
            nestedScrollState = headerScrollState.takeIf { showHeaderPage },
            nestedScrollItemIndex = HEADER_PAGE_INDEX.takeIf { showHeaderPage },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

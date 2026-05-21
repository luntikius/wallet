package com.luntikius.wallet.wear.ui

import android.graphics.BitmapFactory
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.luntikius.wallet.corestrings.R
import com.luntikius.wallet.wear.data.CachedWearPass
import com.luntikius.wallet.wearsync.WearPassField
import com.luntikius.wallet.wearsync.WearPassFieldSection
import kotlin.math.abs

private val DetailCardSurface = Color.White
private val DetailCardText = Color(0xFF101010)
private val DetailCardMutedText = Color(0xFF5F6368)
private val FullscreenCardShape = RoundedCornerShape(32.dp)

@Composable
internal fun ScalableWearPagerItem(
    pagerState: PagerState,
    page: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val targetScale by remember(page, pagerState) {
        derivedStateOf {
            val distanceFraction = abs(pagerState.pageOffsetFrom(page)).coerceIn(0f, 1f)
            1f - (distanceFraction * 0.16f)
        }
    }
    val scale by animateFloatAsState(
        targetValue = targetScale,
        label = "wear-detail-card-scale",
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

@Composable
internal fun PassQrCard(
    pass: CachedWearPass,
    scrollHintProgress: Float,
    showScrollHint: Boolean,
    modifier: Modifier = Modifier,
) {
    val barcode = pass.snapshot.barcode

    BoxWithConstraints(
        modifier = modifier
            .clip(FullscreenCardShape)
            .background(Color.White),
    ) {
        val diameter = minOf(maxWidth, maxHeight)
        val horizontalPadding = diameter * 0.06f
        val verticalPadding = diameter * 0.07f
        val scrollHintBottomPadding = diameter * 0.04f

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = horizontalPadding,
                    top = verticalPadding,
                    end = horizontalPadding,
                    bottom = verticalPadding,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (barcode == null) {
                Text(
                    text = stringResource(R.string.no_code),
                    style = MaterialTheme.typography.titleMedium,
                    color = DetailCardText,
                )
            } else {
                val barcodeBitmap = remember(barcode.value, barcode.format) {
                    generateWearBarcodeBitmap(
                        value = barcode.value,
                        format = barcode.format,
                        rotateForWear = false,
                    )
                }

                if (barcodeBitmap == null) {
                    Text(
                        text = stringResource(R.string.code_unavailable),
                        style = MaterialTheme.typography.titleMedium,
                        color = DetailCardText,
                    )
                } else {
                    Image(
                        bitmap = barcodeBitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.pass_code),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
        }

        if (showScrollHint) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = scrollHintBottomPadding),
            ) {
                ScrollAffordance(
                    unfoldProgress = scrollHintProgress,
                    color = Color.Black,
                )
            }
        }
    }
}

@Composable
internal fun PassHeaderCard(
    pass: CachedWearPass,
    fields: List<WearPassField>,
    scrollState: ScrollState,
    onOpenPassOnPhone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardColor = remember(pass.snapshot.backgroundColor) {
        parseWearColor(pass.snapshot.backgroundColor, Color(0xFF0077B6))
    }
    val textColor = remember(pass.snapshot.foregroundColor, cardColor) {
        readableColor(
            foreground = parseWearColor(pass.snapshot.foregroundColor, Color.White),
            background = cardColor,
        )
    }
    val labelColor = remember(pass.snapshot.labelColor, textColor) {
        parseWearColor(pass.snapshot.labelColor, textColor)
    }

    BoxWithConstraints(
        modifier = modifier
            .clip(FullscreenCardShape)
            .background(cardColor),
    ) {
        val diameter = minOf(maxWidth, maxHeight)
        val headerFields = remember(fields) { fields.filter { it.section == WearPassFieldSection.HEADER } }
        val primaryFields = remember(fields) { fields.filter { it.section == WearPassFieldSection.PRIMARY } }
        val secondaryFields = remember(fields) { fields.filter { it.section == WearPassFieldSection.SECONDARY } }
        val auxiliaryFields = remember(fields) { fields.filter { it.section == WearPassFieldSection.AUXILIARY } }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(
                    state = scrollState,
                    enabled = false,
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = diameter * 0.12f, vertical = diameter * 0.13f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PassLogo(pass = pass, tint = textColor)

                FrontFieldsSection(
                    fields = headerFields,
                    labelColor = labelColor,
                    valueColor = textColor,
                    topSpacing = 14.dp,
                )
                FrontFieldsSection(
                    fields = primaryFields,
                    labelColor = labelColor,
                    valueColor = textColor,
                    topSpacing = 12.dp,
                )
                FrontFieldsSection(
                    fields = secondaryFields,
                    labelColor = labelColor,
                    valueColor = textColor,
                    topSpacing = 12.dp,
                )
                FrontFieldsSection(
                    fields = auxiliaryFields,
                    labelColor = labelColor,
                    valueColor = textColor,
                    topSpacing = 12.dp,
                )

                DigitalCodeText(
                    pass = pass,
                    color = textColor,
                )

                OpenOnPhoneTextButton(
                    textColor = textColor,
                    onClick = onOpenPassOnPhone,
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun OpenOnPhoneTextButton(textColor: Color, onClick: () -> Unit) {
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = stringResource(R.string.open_on_phone),
        style = MaterialTheme.typography.labelMedium,
        color = textColor,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(textColor.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    )
}

@Composable
private fun DigitalCodeText(pass: CachedWearPass, color: Color) {
    val readableCode = pass.snapshot.barcode?.let { barcode ->
        barcode.altText?.takeIf { it.isNotBlank() } ?: barcode.value
    } ?: return

    Spacer(modifier = Modifier.height(14.dp))
    Text(
        text = remember(readableCode) { readableCode.toWearPlainText() },
        style = MaterialTheme.typography.labelMedium,
        color = color.copy(alpha = 0.5f),
        fontWeight = FontWeight.SemiBold,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun FrontFieldsSection(
    fields: List<WearPassField>,
    labelColor: Color,
    valueColor: Color,
    topSpacing: androidx.compose.ui.unit.Dp,
) {
    if (fields.isEmpty()) return

    Spacer(modifier = Modifier.height(topSpacing))
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        fields.forEach { field ->
            FrontFieldText(
                field = field,
                labelColor = labelColor,
                valueColor = valueColor,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun FrontFieldText(field: WearPassField, labelColor: Color, valueColor: Color, modifier: Modifier = Modifier) {
    val label = field.label?.takeIf { it.isNotBlank() } ?: field.key

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = labelColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = remember(field.value) { field.value.toWearPlainText() },
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun InformationFieldCard(field: WearPassField, modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier = modifier
            .clip(FullscreenCardShape)
            .background(DetailCardSurface),
    ) {
        val diameter = minOf(maxWidth, maxHeight)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = diameter * 0.16f, vertical = diameter * 0.18f),
            contentAlignment = Alignment.Center,
        ) {
            DetailFieldText(field = field)
        }
    }
}

@Composable
internal fun EmptyInformationCard(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(FullscreenCardShape)
            .background(DetailCardSurface)
            .padding(18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.no_additional_information),
            style = MaterialTheme.typography.bodyMedium,
            color = DetailCardMutedText,
        )
    }
}

@Composable
private fun PassLogo(pass: CachedWearPass, tint: Color, modifier: Modifier = Modifier) {
    val bitmap = remember(pass.logoPath, pass.iconPath) {
        (pass.logoPath ?: pass.iconPath)?.let { BitmapFactory.decodeFile(it) }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 34.dp, max = 58.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = stringResource(R.string.pass_logo),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        } else {
            Text(
                text = pass.snapshot.title,
                style = MaterialTheme.typography.titleMedium,
                color = tint,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun DetailFieldText(
    field: WearPassField,
    labelColor: Color = DetailCardMutedText,
    valueColor: Color = DetailCardText,
) {
    val label = remember(field.label, field.key) {
        (field.label?.takeIf { it.isNotBlank() } ?: field.key).toWearPlainText()
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = labelColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = remember(field.value) { field.value.toWearPlainText() },
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            maxLines = 8,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

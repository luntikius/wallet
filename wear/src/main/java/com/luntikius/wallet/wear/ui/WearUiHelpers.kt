package com.luntikius.wallet.wear.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.luntikius.wallet.wear.R
import com.luntikius.wallet.wear.data.CachedWearPass
import com.luntikius.wallet.wearsync.WearPassFieldSection

@Composable
internal fun PassIcon(pass: CachedWearPass, tint: Color, background: Color, size: Dp) {
    val bitmap = remember(pass.iconPath, pass.logoPath) {
        val path = pass.iconPath ?: pass.logoPath
        path?.let { BitmapFactory.decodeFile(it) }
    }
    val customIconRes = remember(pass.snapshot.format, pass.snapshot.customIconName) {
        pass.snapshot.customIconName
            ?.takeIf { pass.snapshot.format == CUSTOM_PASS_FORMAT }
            ?.let(::customPassIconResource)
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(readableIconBackground(tint, background)),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Pass icon",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else if (customIconRes != null) {
            Image(
                painter = painterResource(id = customIconRes),
                contentDescription = "Pass icon",
                modifier = Modifier.size(size * 0.58f),
                colorFilter = ColorFilter.tint(tint),
                contentScale = ContentScale.Fit,
            )
        } else {
            Text(
                text = pass.snapshot.title.firstOrNull()?.uppercase() ?: "W",
                style = MaterialTheme.typography.titleSmall,
                color = tint,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private const val CUSTOM_PASS_FORMAT = "CUSTOM"

private fun customPassIconResource(name: String): Int = when (name) {
    "Like" -> R.drawable.like
    "Cart" -> R.drawable.cart
    "Gift" -> R.drawable.present
    "Food" -> R.drawable.food
    "Shop" -> R.drawable.shop
    "Flower" -> R.drawable.flower
    "Package" -> R.drawable.package_icon
    else -> R.drawable.star
}

internal val WearPassFieldSection.label: String
    get() = when (this) {
        WearPassFieldSection.HEADER -> "Header"
        WearPassFieldSection.PRIMARY -> "Primary"
        WearPassFieldSection.SECONDARY -> "Details"
        WearPassFieldSection.AUXILIARY -> "More"
        WearPassFieldSection.BACK -> "Information"
        WearPassFieldSection.METADATA -> "Metadata"
    }

internal fun parseWearColor(value: String?, fallback: Color): Color = runCatching {
    Color(android.graphics.Color.parseColor(value))
}.getOrDefault(fallback)

internal fun readableColor(foreground: Color, background: Color): Color {
    if (contrastRatio(foreground, background) >= 4.5f) return foreground

    val blackContrast = contrastRatio(Color.Black, background)
    val whiteContrast = contrastRatio(Color.White, background)
    return if (blackContrast >= whiteContrast) Color.Black else Color.White
}

private fun readableIconBackground(foreground: Color, background: Color): Color =
    if (contrastRatio(foreground, background) >= 4.5f) {
        foreground.copy(alpha = 0.16f)
    } else {
        readableColor(Color.White, background).copy(alpha = 0.18f)
    }

private fun contrastRatio(foreground: Color, background: Color): Float {
    val foregroundLuminance = foreground.luminance() + 0.05f
    val backgroundLuminance = background.luminance() + 0.05f
    return maxOf(foregroundLuminance, backgroundLuminance) / minOf(foregroundLuminance, backgroundLuminance)
}

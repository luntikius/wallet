package com.luntikius.wallet.settings

import androidx.annotation.StringRes
import com.luntikius.wallet.corestrings.R

enum class AppThemeMode(@StringRes val labelResId: Int) {
    LIGHT(R.string.theme_light),
    DARK(R.string.theme_dark),
    SYSTEM(R.string.theme_system),
}

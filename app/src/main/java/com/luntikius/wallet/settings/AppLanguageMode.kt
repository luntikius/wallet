package com.luntikius.wallet.settings

import androidx.annotation.StringRes
import com.luntikius.wallet.corestrings.R

enum class AppLanguageMode(val languageTag: String, @StringRes val labelResId: Int) {
    SYSTEM("", R.string.language_system),
    ENGLISH("en", R.string.language_english),
    RUSSIAN("ru", R.string.language_russian),
}

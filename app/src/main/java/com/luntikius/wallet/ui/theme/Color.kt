package com.luntikius.wallet.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Pastel colors for custom passes (backgrounds)
val WalletPastelRed = Color(0xFFFFADAD)
val WalletPastelOrange = Color(0xFFFFD6A5)
val WalletPastelYellow = Color(0xFFFDFFB6)
val WalletPastelGreen = Color(0xFFCAFFBF)
val WalletPastelCyan = Color(0xFF9BF6FF)
val WalletPastelBlue = Color(0xFFA0C4FF)
val WalletPastelPurple = Color(0xFFBDB2FF)
val WalletPastelPink = Color(0xFFFFC6FF)

// Darker variants for text (foreground)
val WalletDarkRed = Color(0xFFCC5A5A)
val WalletDarkOrange = Color(0xFFCC8952)
val WalletDarkYellow = Color(0xFFCACC63)
val WalletDarkGreen = Color(0xFF77CC6C)
val WalletDarkCyan = Color(0xFF48C3CC)
val WalletDarkBlue = Color(0xFF4D77CC)
val WalletDarkPurple = Color(0xFF6A5FCC)
val WalletDarkPink = Color(0xFFCC73CC)

/**
 * Preset colors for custom passes - soft pastel colors with darker text.
 * Each color is a pair of (backgroundColor, foregroundColor).
 */
val CustomPassColors = listOf(
    WalletPastelRed to WalletDarkRed,
    WalletPastelOrange to WalletDarkOrange,
    WalletPastelYellow to WalletDarkYellow,
    WalletPastelGreen to WalletDarkGreen,
    WalletPastelCyan to WalletDarkCyan,
    WalletPastelBlue to WalletDarkBlue,
    WalletPastelPurple to WalletDarkPurple,
    WalletPastelPink to WalletDarkPink,
)

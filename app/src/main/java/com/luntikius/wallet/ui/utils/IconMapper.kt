package com.luntikius.wallet.ui.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Utility object for mapping icon names to Material Icons.
 * Used for custom pass icon selection and rendering.
 */
object IconMapper {
    /**
     * List of available icons for custom pass selection.
     * Each pair contains: (iconName, ImageVector)
     */
    val availableIcons: List<Pair<String, ImageVector>> = listOf(
        "Star" to Icons.Outlined.Star,
        "ShoppingCart" to Icons.Outlined.ShoppingCart,
        "Favorite" to Icons.Outlined.Favorite,
        "ThumbUp" to Icons.Outlined.ThumbUp,
        "Check" to Icons.Outlined.Check,
        "AccountCircle" to Icons.Outlined.AccountCircle,
        "Home" to Icons.Outlined.Home,
        "Info" to Icons.Outlined.Info,
    )

    /**
     * Retrieves the Material Icon for a given icon name.
     *
     * @param name The icon name (e.g., "Star", "ShoppingCart")
     * @return The corresponding ImageVector, or Star as fallback if not found
     */
    fun getIconByName(name: String?): ImageVector {
        return availableIcons.find { it.first == name }?.second
            ?: Icons.Outlined.Star // Default fallback
    }
}

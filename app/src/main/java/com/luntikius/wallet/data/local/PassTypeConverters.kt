package com.luntikius.wallet.data.local

import androidx.room.TypeConverter
import com.luntikius.wallet.data.model.PassCategory
import com.luntikius.wallet.data.model.PassFormat

/**
 * Type converters for Room database to handle enum types.
 */
class PassTypeConverters {
    @TypeConverter
    fun fromPassFormat(value: PassFormat): String {
        return value.name
    }

    @TypeConverter
    fun toPassFormat(value: String): PassFormat {
        return PassFormat.valueOf(value)
    }

    @TypeConverter
    fun fromPassCategory(value: PassCategory): String {
        return value.name
    }

    @TypeConverter
    fun toPassCategory(value: String): PassCategory {
        return PassCategory.valueOf(value)
    }
}

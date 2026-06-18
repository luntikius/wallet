package com.luntikius.wallet.data.json

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
object WalletJson {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        allowTrailingComma = true
    }
}

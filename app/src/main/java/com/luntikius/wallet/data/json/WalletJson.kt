package com.luntikius.wallet.data.json

import kotlinx.serialization.json.Json

object WalletJson {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
}

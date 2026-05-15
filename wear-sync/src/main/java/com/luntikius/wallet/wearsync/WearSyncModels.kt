package com.luntikius.wallet.wearsync

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

const val WEAR_PASS_SCHEMA_VERSION = 1

object WearSyncPaths {
    const val PASSES_INDEX = "/wallet/passes/index"
    const val PASS_PREFIX = "/wallet/pass/"
    const val REQUEST_SYNC = "/wallet/request-sync"

    fun pass(passId: String): String = "$PASS_PREFIX$passId"

    fun passIdFromPath(path: String): String? = path
        .takeIf { it.startsWith(PASS_PREFIX) }
        ?.removePrefix(PASS_PREFIX)
        ?.takeIf { it.isNotBlank() }
}

object WearSyncDataKeys {
    const val INDEX_JSON = "index_json"
    const val SNAPSHOT_JSON = "snapshot_json"
    const val UPDATED_AT = "updated_at"
    const val ICON_ASSET = "icon_asset"
    const val LOGO_ASSET = "logo_asset"
}

object WearSyncJson {
    val json: Json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun encodeSnapshot(snapshot: WearPassSnapshot): String = json.encodeToString(snapshot)

    fun decodeSnapshot(value: String): WearPassSnapshot = json.decodeFromString(value)

    fun encodeIndex(index: WearPassIndex): String = json.encodeToString(index)

    fun decodeIndex(value: String): WearPassIndex = json.decodeFromString(value)
}

@Serializable
data class WearPassIndex(
    val schemaVersion: Int = WEAR_PASS_SCHEMA_VERSION,
    val passIds: List<String>,
    val updatedAt: Long,
)

@Serializable
data class WearPassSnapshot(
    val schemaVersion: Int = WEAR_PASS_SCHEMA_VERSION,
    val id: String,
    val format: String,
    val title: String,
    val subtitle: String,
    val category: String,
    val backgroundColor: String?,
    val foregroundColor: String?,
    val labelColor: String?,
    val displayOrder: Int,
    val barcode: WearPassBarcode?,
    val customIconName: String? = null,
    val fields: List<WearPassField> = emptyList(),
    val assets: List<WearPassAssetRef> = emptyList(),
)

@Serializable
data class WearPassBarcode(val value: String, val format: WearBarcodeFormat, val altText: String? = null)

@Serializable
enum class WearBarcodeFormat {
    QR_CODE,
    CODE_128,
    CODE_39,
    CODE_93,
    CODABAR,
    DATA_MATRIX,
    EAN_13,
    EAN_8,
    ITF,
    UPC_A,
    UPC_E,
    PDF_417,
    AZTEC,
}

@Serializable
data class WearPassField(val key: String, val label: String?, val value: String, val section: WearPassFieldSection)

@Serializable
enum class WearPassFieldSection {
    HEADER,
    PRIMARY,
    SECONDARY,
    AUXILIARY,
    BACK,
    METADATA,
}

@Serializable
data class WearPassAssetRef(val name: String, val type: WearPassAssetType)

@Serializable
enum class WearPassAssetType {
    ICON,
    LOGO,
}

package com.luntikius.wallet.wear

import com.luntikius.wallet.data.model.BarcodeFormatType
import com.luntikius.wallet.data.model.Pass
import com.luntikius.wallet.data.model.PassData
import com.luntikius.wallet.data.model.PassFormat
import com.luntikius.wallet.data.model.getPassData
import com.luntikius.wallet.data.parser.pkpass.LocalizationResolver
import com.luntikius.wallet.data.parser.pkpass.PKBarcode
import com.luntikius.wallet.data.parser.pkpass.PKField
import com.luntikius.wallet.data.parser.pkpass.PKPassJson
import com.luntikius.wallet.data.parser.pkpass.PKPassLocalizations
import com.luntikius.wallet.data.parser.pkpass.PKPassStructure
import com.luntikius.wallet.wearsync.WearBarcodeFormat
import com.luntikius.wallet.wearsync.WearPassAssetRef
import com.luntikius.wallet.wearsync.WearPassAssetType
import com.luntikius.wallet.wearsync.WearPassBarcode
import com.luntikius.wallet.wearsync.WearPassField
import com.luntikius.wallet.wearsync.WearPassFieldSection
import com.luntikius.wallet.wearsync.WearPassSnapshot
import java.io.File
import java.util.Locale

fun Pass.toWearPassSnapshot(): WearPassSnapshot? = runCatching {
    when (val passData = getPassData()) {
        is PassData.PKPass -> toPkPassSnapshot(passData.pkPassJson)
        is PassData.Custom -> WearPassSnapshot(
            id = id,
            format = format.name,
            title = organizationName,
            subtitle = description,
            category = category.name,
            backgroundColor = backgroundColor,
            foregroundColor = foregroundColor,
            labelColor = labelColor,
            displayOrder = displayOrder,
            barcode = WearPassBarcode(
                value = passData.customPassJson.barcodeValue,
                format = BarcodeFormatType.fromName(passData.customPassJson.barcodeFormat).toWearFormat(),
            ),
            customIconName = passData.customPassJson.iconName,
            fields = listOf(
                WearPassField(
                    key = "format",
                    label = "Format",
                    value = passData.customPassJson.barcodeFormat,
                    section = WearPassFieldSection.METADATA,
                ),
            ),
            assets = wearAssetRefs(),
        )
    }
}.getOrNull()

private fun Pass.toPkPassSnapshot(pkPassJson: PKPassJson): WearPassSnapshot {
    val structure = pkPassJson.passStructure()
    val barcode = (pkPassJson.barcodes?.firstOrNull() ?: pkPassJson.barcode)?.toWearBarcode()

    return WearPassSnapshot(
        id = id,
        format = PassFormat.PKPASS.name,
        title = pkPassJson.logoText?.takeIf { it.isNotBlank() } ?: organizationName,
        subtitle = description,
        category = category.name,
        backgroundColor = backgroundColor,
        foregroundColor = foregroundColor,
        labelColor = labelColor,
        displayOrder = displayOrder,
        barcode = barcode,
        fields = structure?.wearFields(pkPassJson.localizations).orEmpty(),
        assets = wearAssetRefs(),
    )
}

private fun PKPassJson.passStructure(): PKPassStructure? = boardingPass ?: eventTicket ?: coupon ?: storeCard ?: generic

private fun PKPassStructure.wearFields(localizations: PKPassLocalizations?): List<WearPassField> = buildList {
    addFields(headerFields, WearPassFieldSection.HEADER, localizations)
    addFields(primaryFields, WearPassFieldSection.PRIMARY, localizations)
    addFields(secondaryFields, WearPassFieldSection.SECONDARY, localizations)
    addFields(auxiliaryFields, WearPassFieldSection.AUXILIARY, localizations)
    addFields(backFields, WearPassFieldSection.BACK, localizations)
}

private fun MutableList<WearPassField>.addFields(
    fields: List<PKField>?,
    section: WearPassFieldSection,
    localizations: PKPassLocalizations?,
) {
    fields.orEmpty().forEach { field ->
        val value = field.value?.takeIf { it.isNotBlank() } ?: return@forEach
        add(
            WearPassField(
                key = field.key,
                label = field.label.localized(localizations),
                value = value.localized(localizations),
                section = section,
            ),
        )
    }
}

private fun String?.localized(localizations: PKPassLocalizations?): String = LocalizationResolver.resolveLocalizedValue(
    value = this,
    localizations = localizations,
    locale = Locale.getDefault(),
)

private fun PKBarcode.toWearBarcode(): WearPassBarcode? {
    val wearFormat = when (format) {
        "PKBarcodeFormatQR" -> WearBarcodeFormat.QR_CODE
        "PKBarcodeFormatPDF417" -> WearBarcodeFormat.PDF_417
        "PKBarcodeFormatAztec" -> WearBarcodeFormat.AZTEC
        "PKBarcodeFormatCode128" -> WearBarcodeFormat.CODE_128
        else -> null
    } ?: return null

    return WearPassBarcode(
        value = message,
        format = wearFormat,
        altText = altText,
    )
}

private fun BarcodeFormatType.toWearFormat(): WearBarcodeFormat = when (this) {
    BarcodeFormatType.QR_CODE -> WearBarcodeFormat.QR_CODE
    BarcodeFormatType.CODE_128 -> WearBarcodeFormat.CODE_128
    BarcodeFormatType.CODE_39 -> WearBarcodeFormat.CODE_39
    BarcodeFormatType.CODE_93 -> WearBarcodeFormat.CODE_93
    BarcodeFormatType.CODABAR -> WearBarcodeFormat.CODABAR
    BarcodeFormatType.DATA_MATRIX -> WearBarcodeFormat.DATA_MATRIX
    BarcodeFormatType.EAN_13 -> WearBarcodeFormat.EAN_13
    BarcodeFormatType.EAN_8 -> WearBarcodeFormat.EAN_8
    BarcodeFormatType.ITF -> WearBarcodeFormat.ITF
    BarcodeFormatType.UPC_A -> WearBarcodeFormat.UPC_A
    BarcodeFormatType.UPC_E -> WearBarcodeFormat.UPC_E
    BarcodeFormatType.PDF_417 -> WearBarcodeFormat.PDF_417
    BarcodeFormatType.AZTEC -> WearBarcodeFormat.AZTEC
}

internal fun Pass.assetFile(type: WearPassAssetType): File? = when (type) {
    WearPassAssetType.ICON -> iconPath
    WearPassAssetType.LOGO -> logoPath
}
    ?.takeIf { it.isNotBlank() }
    ?.let(::File)
    ?.takeIf { it.exists() && it.isFile }

private fun Pass.wearAssetRefs(): List<WearPassAssetRef> = WearPassAssetType.entries.mapNotNull { type ->
    assetFile(type)?.let { file ->
        WearPassAssetRef(
            name = "$id-${type.name.lowercase()}-${file.lastModified()}",
            type = type,
        )
    }
}

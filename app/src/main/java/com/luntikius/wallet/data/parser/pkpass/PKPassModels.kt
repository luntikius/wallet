package com.luntikius.wallet.data.parser.pkpass

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * PKPass JSON structure (pass.json)
 * Based on Apple Wallet Pass specification
 */
@Serializable
data class PKPassJson(
    val formatVersion: Int,
    val passTypeIdentifier: String,
    val serialNumber: String,
    val teamIdentifier: String? = null,
    val organizationName: String,
    val description: String,

    // Visual appearance
    val foregroundColor: String? = null,
    val backgroundColor: String? = null,
    val labelColor: String? = null,
    val logoText: String? = null,

    // Pass type (one of these will be present)
    val boardingPass: PKPassStructure? = null,
    val coupon: PKPassStructure? = null,
    val eventTicket: PKPassStructure? = null,
    val generic: PKPassStructure? = null,
    val storeCard: PKPassStructure? = null,

    // Barcode
    val barcodes: List<PKBarcode>? = null,
    val barcode: PKBarcode? = null,

    // Relevance
    val locations: List<PKLocation>? = null,
    val relevantDate: String? = null,
    val expirationDate: String? = null,
    val voided: Boolean? = null,

    // Web service for updates
    val webServiceURL: String? = null,
    val authenticationToken: String? = null,

    // Localization
    val localizations: PKPassLocalizations? = null,
)

/**
 * Pass structure containing fields for a specific pass type
 */
@Serializable
data class PKPassStructure(
    val transitType: String? = null,
    val headerFields: List<PKField>? = null,
    val primaryFields: List<PKField>? = null,
    val secondaryFields: List<PKField>? = null,
    val auxiliaryFields: List<PKField>? = null,
    val backFields: List<PKField>? = null,
)

/**
 * Individual field in a pass.
 * The [value] field accepts any JSON primitive (string, number, boolean) and stores it as String.
 */
@Serializable
data class PKField(
    val key: String,
    val label: String? = null,
    @Serializable(with = AnyJsonToStringSerializer::class)
    val value: String? = null,
    val textAlignment: String? = null,
    val dataDetectorTypes: List<String>? = null,
)

/**
 * Custom serializer that accepts any JSON primitive value and converts it to String.
 * This handles PKPass fields where values can be strings, numbers, or booleans.
 */
object AnyJsonToStringSerializer : KSerializer<String?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("AnyJsonToString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: String?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            encoder.encodeString(value)
        }
    }

    override fun deserialize(decoder: Decoder): String? {
        if (decoder is JsonDecoder) {
            return when (val element = decoder.decodeJsonElement()) {
                is JsonNull -> null
                is JsonPrimitive -> element.contentOrNull
                else -> element.toString()
            }
        }
        return try {
            decoder.decodeString()
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * Barcode information
 */
@Serializable
data class PKBarcode(val format: String, val message: String, val messageEncoding: String, val altText: String? = null)

/**
 * Location for relevance
 */
@Serializable
data class PKLocation(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val relevantText: String? = null,
)

/**
 * Localization data for PKPass
 * Contains locale-specific string translations from .lproj directories
 *
 * Format: Map of locale codes to translation maps (locale -> key -> value)
 */
@Serializable
data class PKPassLocalizations(
    val defaultLocale: String = "en",
    val localizations: Map<String, Map<String, String>> = emptyMap(),
)

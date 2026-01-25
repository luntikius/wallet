package com.luntikius.wallet.data.parser.pkpass

import com.google.gson.annotations.SerializedName

/**
 * PKPass JSON structure (pass.json)
 * Based on Apple Wallet Pass specification
 */
data class PKPassJson(
    val formatVersion: Int,
    val passTypeIdentifier: String,
    val serialNumber: String,
    val teamIdentifier: String?,
    val organizationName: String,
    val description: String,

    // Visual appearance
    val foregroundColor: String?,
    val backgroundColor: String?,
    val labelColor: String?,
    val logoText: String?,

    // Pass type (one of these will be present)
    val boardingPass: PKPassStructure?,
    val coupon: PKPassStructure?,
    val eventTicket: PKPassStructure?,
    val generic: PKPassStructure?,
    val storeCard: PKPassStructure?,

    // Barcode
    val barcodes: List<PKBarcode>?,
    val barcode: PKBarcode?,

    // Relevance
    val locations: List<PKLocation>?,
    val relevantDate: String?,
    val expirationDate: String?,
    val voided: Boolean?,

    // Web service for updates
    val webServiceURL: String?,
    val authenticationToken: String?
)

/**
 * Pass structure containing fields for a specific pass type
 */
data class PKPassStructure(
    val transitType: String?,
    val headerFields: List<PKField>?,
    val primaryFields: List<PKField>?,
    val secondaryFields: List<PKField>?,
    val auxiliaryFields: List<PKField>?,
    val backFields: List<PKField>?
)

/**
 * Individual field in a pass
 */
data class PKField(
    val key: String,
    val label: String?,
    val value: Any?,
    val textAlignment: String?,
    val dataDetectorTypes: List<String>?
)

/**
 * Barcode information
 */
data class PKBarcode(
    val format: String,
    val message: String,
    val messageEncoding: String,
    val altText: String?
)

/**
 * Location for relevance
 */
data class PKLocation(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double?,
    val relevantText: String?
)

package com.luntikius.wallet.data.exporter

import android.content.Context
import com.luntikius.wallet.data.exporter.pkpass.PKPassExporter
import com.luntikius.wallet.data.model.Pass

/**
 * Factory pattern registry for resolving the correct exporter based on pass format.
 * Enables easy addition of new export formats without modifying existing code.
 */
class ExporterRegistry(context: Context) {
    private val exporters: List<PassExporter> = listOf(
        PKPassExporter(context),
        // Future exporters can be added here:
        // GoogleWalletExporter(context),
    )

    /**
     * Resolve the appropriate exporter for the given pass.
     */
    fun resolveExporter(pass: Pass): PassExporter? = exporters.firstOrNull { it.canExport(pass) }

    /**
     * Get all registered exporters.
     */
    fun getAllExporters(): List<PassExporter> = exporters
}

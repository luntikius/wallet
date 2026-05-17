package com.luntikius.wallet.data.archive

import com.luntikius.wallet.data.builder.CustomPassBuilder
import com.luntikius.wallet.data.model.PassFormat
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class WalletArchiveTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `safe entry validation rejects traversal paths`() {
        assertTrue(WalletArchive.isSafeEntryName("manifest.json"))
        assertTrue(WalletArchive.isSafeEntryName("passes/pass-id/metadata.json"))

        assertFalse(WalletArchive.isSafeEntryName("../manifest.json"))
        assertFalse(WalletArchive.isSafeEntryName("passes/../metadata.json"))
        assertFalse(WalletArchive.isSafeEntryName("/manifest.json"))
        assertFalse(WalletArchive.isSafeEntryName("passes\\pass-id\\metadata.json"))
        assertFalse(WalletArchive.isSafeEntryName("passes//metadata.json"))
    }

    @Test
    fun `custom pass round trips through archive metadata`() {
        val pass = CustomPassBuilder.createCustomPass(
            cardName = "Coffee Card",
            barcodeValue = "1234567890",
            barcodeFormat = "QR_CODE",
            iconName = "Store",
            backgroundColor = "#0077B6",
        ).copy(displayOrder = 3)
        val outputFile = File(temporaryFolder.root, "wallet.zip")

        WalletArchive.export(listOf(pass), outputFile)
        val readResult = outputFile.inputStream().use { input ->
            WalletArchive.read(input)
        }

        assertEquals(1, readResult.passes.size)
        val payload = readResult.passes.single()
        assertEquals(pass.id, payload.metadata.id)
        assertEquals(PassFormat.CUSTOM, payload.metadata.format)
        assertEquals("Coffee Card", payload.metadata.organizationName)
        assertEquals(3, payload.metadata.displayOrder)
        assertEquals(pass.rawData, payload.metadata.rawData)
        assertNull(payload.pkPassBytes)
    }

    @Test
    fun `empty archive is rejected`() {
        val outputFile = File(temporaryFolder.root, "empty.zip")
        ZipOutputStream(FileOutputStream(outputFile)).close()

        val error = assertThrows(IllegalArgumentException::class.java) {
            outputFile.inputStream().use { input ->
                WalletArchive.read(input)
            }
        }

        assertNotNull(error.message)
    }

    @Test
    fun `archive with unsafe entry is rejected`() {
        val outputFile = File(temporaryFolder.root, "unsafe.zip")
        ZipOutputStream(FileOutputStream(outputFile)).use { zipOut ->
            zipOut.putNextEntry(ZipEntry("../manifest.json"))
            zipOut.write("{}".toByteArray())
            zipOut.closeEntry()
        }

        val error = assertThrows(IllegalArgumentException::class.java) {
            outputFile.inputStream().use { input ->
                WalletArchive.read(input)
            }
        }

        assertTrue(error.message?.contains("Unsafe wallet archive entry") == true)
    }

    @Test
    fun `import result summary includes imported replaced and failed counts`() {
        val result = WalletArchiveImportResult(
            importedCount = 2,
            replacedCount = 1,
            failedCount = 1,
        )

        assertEquals("Imported 2 passes (1 replaced), 1 failed", result.summaryMessage)
    }
}

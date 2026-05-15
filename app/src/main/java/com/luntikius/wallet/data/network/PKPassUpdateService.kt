package com.luntikius.wallet.data.network

import android.content.Context
import android.net.Uri
import android.util.Log
import com.luntikius.wallet.data.json.WalletJson
import com.luntikius.wallet.data.model.Pass
import com.luntikius.wallet.data.parser.pkpass.PKPassJson
import com.luntikius.wallet.data.parser.pkpass.PKPassParser
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.SocketTimeoutException
import retrofit2.Retrofit

/**
 * Service for updating PKPass cards via Apple Wallet update protocol.
 * Handles dynamic Retrofit instance creation and API communication.
 */
class PKPassUpdateService(private val context: Context, private val pkPassParser: PKPassParser) {

    /**
     * Result of a pass update operation.
     */
    sealed class UpdateResult {
        /** Pass was updated successfully with new data */
        data class Updated(val newPassJson: PKPassJson) : UpdateResult()

        /** Pass has not been modified (304) */
        data object NotModified : UpdateResult()

        /** Pass was deleted/voided on server (404) */
        data object Deleted : UpdateResult()

        /** Authentication failed (401) */
        data object Unauthorized : UpdateResult()

        /** Network error occurred */
        data class NetworkError(val message: String) : UpdateResult()

        /** Pass does not have web service configured */
        data object NoWebService : UpdateResult()
    }

    /**
     * Attempts to update a pass by fetching new data from its web service.
     *
     * @param pass The pass to update
     * @return UpdateResult indicating the outcome
     */
    suspend fun updatePass(pass: Pass): UpdateResult {
        return try {
            // Parse rawData JSON to extract web service info
            val passJson = WalletJson.json.decodeFromString<PKPassJson>(pass.rawData)

            // Check if pass has web service configured
            val webServiceURL = passJson.webServiceURL
            val authToken = passJson.authenticationToken
            val passTypeIdentifier = passJson.passTypeIdentifier
            val serialNumber = passJson.serialNumber

            if (webServiceURL == null || authToken == null) {
                return UpdateResult.NoWebService
            }

            // Ensure webServiceURL ends with / for proper URL construction
            val baseUrl = if (webServiceURL.endsWith("/")) {
                webServiceURL
            } else {
                "$webServiceURL/"
            }

            // Create dynamic Retrofit instance for this pass's web service
            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(NetworkModule.okHttpClient)
                .build()

            val apiService = retrofit.create(PKPassApiService::class.java)

            // Call API with Authorization header
            val authHeader = "ApplePass $authToken"
            val response = apiService.getPassUpdate(
                passTypeIdentifier = passTypeIdentifier,
                serialNumber = serialNumber,
                authToken = authHeader,
            )

            // Handle response codes
            when (response.code()) {
                200 -> {
                    // Pass updated successfully - server returns a .pkpass file (binary)
                    val responseBody = response.body()
                    if (responseBody != null) {
                        // Save response to a temporary file
                        val tempFile = File(context.cacheDir, "temp_update_${System.currentTimeMillis()}.pkpass")

                        try {
                            FileOutputStream(tempFile).use { outputStream ->
                                outputStream.write(responseBody.bytes())
                            }

                            // Parse the pkpass file
                            val tempUri = Uri.fromFile(tempFile)
                            val parseResult = pkPassParser.parse(tempUri)

                            if (parseResult != null) {
                                // Extract PKPassJson from rawData
                                val updatedPassJson =
                                    WalletJson.json.decodeFromString<PKPassJson>(parseResult.pass.rawData)

                                UpdateResult.Updated(updatedPassJson)
                            } else {
                                UpdateResult.NetworkError("Failed to parse updated pass")
                            }
                        } finally {
                            // Clean up temp file
                            tempFile.delete()
                        }
                    } else {
                        UpdateResult.NetworkError("Empty response body")
                    }
                }
                304 -> {
                    // Pass not modified
                    UpdateResult.NotModified
                }
                401 -> {
                    // Unauthorized
                    UpdateResult.Unauthorized
                }
                404 -> {
                    // Pass deleted/voided
                    UpdateResult.Deleted
                }
                else -> {
                    // Other error codes
                    UpdateResult.NetworkError("Server returned ${response.code()}: ${response.message()}")
                }
            }
        } catch (e: SocketTimeoutException) {
            Log.e("PKPassUpdate", "RequestTimedOut", e)
            UpdateResult.NetworkError("Request timed out")
        } catch (e: IOException) {
            Log.e("PKPassUpdate", "NoInternet", e)
            UpdateResult.NetworkError("No internet connection")
        } catch (e: Exception) {
            Log.e("PKPassUpdate", "Unknown error", e)
            UpdateResult.NetworkError("Update failed: ${e.message ?: "Unknown error"}")
        }
    }
}

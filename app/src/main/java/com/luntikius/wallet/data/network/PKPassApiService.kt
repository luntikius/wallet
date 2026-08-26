package com.luntikius.wallet.data.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Url

/**
 * Retrofit API service for PKPass update protocol.
 * Based on Apple Wallet Pass specification.
 */
interface PKPassApiService {

    /**
     * Gets pass update from server.
     *
     * Endpoint: either the provider's complete update URL or
     * GET {webServiceURL}/v1/passes/{passTypeIdentifier}/{serialNumber}
     * Header: Authorization: ApplePass {authenticationToken}
     *
     * Response codes:
     * - 200: Pass updated, returns new .pkpass file (binary data)
     * - 304: Pass not modified
     * - 401: Unauthorized (invalid token)
     * - 404: Pass deleted/voided
     *
     * @param updateUrl Resolved absolute URL for this pass update request
     * @param authToken The authentication token (format: "ApplePass {token}")
     * @return Response containing the updated pass file or appropriate status code
     */
    @GET
    suspend fun getPassUpdate(
        @Url updateUrl: String,
        @Header("Authorization") authToken: String,
    ): Response<ResponseBody>
}

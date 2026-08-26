package com.luntikius.wallet.data.network

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

internal object PKPassUpdateUrlResolver {
    fun resolve(webServiceUrl: String, passTypeIdentifier: String, serialNumber: String): String? {
        val parsedUrl = webServiceUrl.toHttpUrlOrNull() ?: return null

        // Some pass providers expose a complete refresh endpoint with request parameters instead
        // of the standard Apple Wallet base URL. Keep that endpoint intact.
        if (parsedUrl.query != null) {
            return webServiceUrl
        }

        val normalizedPath = parsedUrl.encodedPath.trimEnd('/') + "/"
        return parsedUrl.newBuilder()
            .encodedPath(normalizedPath)
            .addPathSegment("v1")
            .addPathSegment("passes")
            .addPathSegment(passTypeIdentifier)
            .addPathSegment(serialNumber)
            .build()
            .toString()
    }
}

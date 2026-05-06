package com.fitnessdatapuller.mobile

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class SyncClient(
    private val json: Json = Json { prettyPrint = true; explicitNulls = false },
) {
    fun preview(payload: HealthPayload): String = json.encodeToString(payload)

    suspend fun sync(settings: SyncSettings, payload: HealthPayload): String = withContext(Dispatchers.IO) {
        require(settings.apiBaseUrl.isNotBlank()) { "API Base URL is required." }
        require(settings.syncApiKey.isNotBlank()) { "SYNC_API_KEY is required." }

        val endpoint = URL("${settings.apiBaseUrl.trim().trimEnd('/')}/api/sync/samsung")
        val connection = (endpoint.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 30_000
            doOutput = true
            setRequestProperty("Authorization", "Bearer ${settings.syncApiKey.trim()}")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
        }

        try {
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(json.encodeToString(payload)) }
            val status = connection.responseCode
            val body = if (status in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            }
            if (status !in 200..299) error("Sync failed with HTTP $status: $body")
            "HTTP $status ${body.ifBlank { "OK" }}"
        } finally {
            connection.disconnect()
        }
    }
}

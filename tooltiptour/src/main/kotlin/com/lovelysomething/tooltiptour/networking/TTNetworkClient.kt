package com.lovelysomething.tooltiptour.networking

import com.lovelysomething.tooltiptour.models.TTConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

internal val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

/** Handles all API communication with the Tooltip Tour backend. */
internal class TTNetworkClient(val baseUrl: String) {

    /**
     * Prefetch all active tour configs for a site in one request.
     * Returns a map keyed by page pattern for instant lookup.
     */
    suspend fun fetchAllConfigs(siteKey: String): Map<String, TTConfig> =
        withContext(Dispatchers.IO) {
            val url = URL("$baseUrl/api/walkthrough/$siteKey?prefetch=true")
            val conn = url.openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "GET"
                conn.connectTimeout = 10_000
                conn.readTimeout    = 10_000
                if (conn.responseCode != 200) return@withContext emptyMap()
                val body = conn.inputStream.bufferedReader().readText()
                val configs = json.decodeFromString<List<TTConfig>>(body)
                configs.mapNotNull { c -> c.pagePattern?.let { it to c } }.toMap()
            } catch (_: Exception) {
                emptyMap()
            } finally {
                conn.disconnect()
            }
        }

    /** Fetch the config for a specific page (or null for global). */
    suspend fun fetchConfig(siteKey: String, page: String? = null): TTConfig? =
        withContext(Dispatchers.IO) {
            val encoded = page?.let { URLEncoder.encode(it, "UTF-8") }
            val urlStr  = if (encoded != null) "$baseUrl/api/walkthrough/$siteKey?page=$encoded"
                          else "$baseUrl/api/walkthrough/$siteKey"
            val conn = URL(urlStr).openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "GET"
                conn.connectTimeout = 10_000
                conn.readTimeout    = 10_000
                // 204 = no active tour, 402 = view limit — both mean no tour
                if (conn.responseCode != 200) return@withContext null
                val body = conn.inputStream.bufferedReader().readText()
                json.decodeFromString<TTConfig>(body)
            } catch (_: Exception) {
                null
            } finally {
                conn.disconnect()
            }
        }

    /** PATCH /api/inspector/sessions/{id} — writes captured element back to the dashboard. */
    suspend fun updateInspectorSession(id: String, identifier: String, displayName: String): Boolean =
        withContext(Dispatchers.IO) {
            val conn = URL("$baseUrl/api/inspector/sessions/$id").openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "PATCH"
                conn.doOutput      = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 10_000
                conn.readTimeout    = 10_000
                val body = """{"identifier":"$identifier","display_name":"$displayName"}"""
                conn.outputStream.write(body.toByteArray())
                conn.responseCode == 200
            } catch (_: Exception) {
                false
            } finally {
                conn.disconnect()
            }
        }
}

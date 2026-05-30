package com.geovision.mobile.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ElevationService {

    private val cache = mutableMapOf<String, CacheEntry>()
    private const val CACHE_TTL_MS = 300_000L

    private data class CacheEntry(
        val elevation: Double?,
        val timestamp: Long
    )

    suspend fun fetch(lat: Double, lon: Double): Double? = withContext(Dispatchers.IO) {
        val key = java.lang.String.format(java.util.Locale.US, "%.4f", lat) + "," + java.lang.String.format(java.util.Locale.US, "%.4f", lon)
        val now = System.currentTimeMillis()

        cache[key]?.let {
            if (now - it.timestamp < CACHE_TTL_MS) {
                return@withContext it.elevation
            }
        }

        try {
            val conn = URL("https://api.open-elevation.com/api/v1/lookup?locations=$lat,$lon")
                .openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            val body = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            val results = JSONObject(body).getJSONObject("results")
            val elevation = results.getJSONArray("elevation").optDouble(0).takeIf { !it.isNaN() }
            cache[key] = CacheEntry(elevation, now)
            elevation
        } catch (_: Exception) {
            cache[key]?.let {
                if (now - it.timestamp < CACHE_TTL_MS * 2) it.elevation
                else null
            }
        }
    }

    fun clearCache() {
        cache.clear()
    }
}

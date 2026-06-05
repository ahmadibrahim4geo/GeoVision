package com.geovision.mobile.data

import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ElevationService {

    // LRU cache: max 500 entries — replaces the old unbounded mutableMapOf
    private val cache = object : LruCache<String, CacheEntry>(500) {}
    private const val CACHE_TTL_MS = 300_000L

    private data class CacheEntry(
        val elevation: Double?,
        val timestamp: Long
    )

    suspend fun fetch(lat: Double, lon: Double): Double? = withContext(Dispatchers.IO) {
        val key = java.lang.String.format(java.util.Locale.US, "%.4f", lat) + "," +
                  java.lang.String.format(java.util.Locale.US, "%.4f", lon)
        val now = System.currentTimeMillis()

        cache.get(key)?.let {
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

            // FIXED: correct API structure — {"results": [{"latitude":X,"longitude":Y,"elevation":Z}]}
            val resultsArr = JSONObject(body).getJSONArray("results")
            val elevation = if (resultsArr.length() > 0) {
                resultsArr.getJSONObject(0).optDouble("elevation").takeIf { !it.isNaN() }
            } else null

            cache.put(key, CacheEntry(elevation, now))
            elevation
        } catch (_: Exception) {
            // On network error, return stale cache if within 2x TTL
            cache.get(key)?.let {
                if (now - it.timestamp < CACHE_TTL_MS * 2) it.elevation else null
            }
        }
    }

    fun clearCache() {
        cache.evictAll()
    }
}

package com.geovision.mobile.data

import android.graphics.Bitmap
import android.util.LruCache
import com.geovision.mobile.ui.screens.layers.FeatureRow

/**
 * LRU Cache for layers, features, and photo thumbnails.
 * Uses linked LruCache internally with configurable max sizes.
 */
object LayerCache {

    // Feature lists keyed by layerId — max 8 full layers in memory
    private val featureCache = object : LruCache<String, List<FeatureRow>>(8) {
        override fun sizeOf(key: String, value: List<FeatureRow>): Int = 1
    }

    // Photo thumbnails — max 4MB total assuming ~50KB per thumbnail
    private val thumbnailCache = object : LruCache<String, Bitmap>(4 * 1024 * 1024) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount.coerceAtLeast(1)
        }
    }

    // Layer style cache — max 16 entries
    private val styleCache = object : LruCache<String, Any>(16) {
        override fun sizeOf(key: String, value: Any): Int = 1
    }

    /**
     * Store features for a layer. Evicts oldest if at capacity.
     */
    fun putFeatures(layerId: String, features: List<FeatureRow>) {
        featureCache.put(layerId, features)
    }

    /**
     * Retrieve cached features. Returns null if evicted.
     */
    fun getFeatures(layerId: String): List<FeatureRow>? = featureCache.get(layerId)

    /**
     * Remove features for a specific layer.
     */
    fun evictFeatures(layerId: String) {
        featureCache.remove(layerId)
    }

    /**
     * Store a photo thumbnail by path key.
     */
    fun putThumbnail(key: String, bitmap: Bitmap) {
        thumbnailCache.put(key, bitmap)
    }

    /**
     * Retrieve a cached thumbnail. Returns null if evicted.
     */
    fun getThumbnail(key: String): Bitmap? = thumbnailCache.get(key)

    /**
     * Store a style object (e.g. KmlStyle, LayerStyle).
     */
    fun putStyle(key: String, style: Any) {
        styleCache.put(key, style)
    }

    /**
     * Retrieve a cached style object.
     */
    fun getStyle(key: String): Any? = styleCache.get(key)

    /**
     * Clear all caches entirely (e.g. on app cache clear).
     */
    fun clearAll() {
        featureCache.evictAll()
        thumbnailCache.evictAll()
        styleCache.evictAll()
    }

    /**
     * Current cache stats for debugging.
     */
    fun stats(): String {
        return "Features: ${featureCache.size()}/${featureCache.maxSize()} | " +
            "Thumbnails: ${thumbnailCache.size() / 1024}KB/${thumbnailCache.maxSize() / 1024}KB | " +
            "Styles: ${styleCache.size()}/${styleCache.maxSize()}"
    }
}

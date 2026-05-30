package com.geovision.mobile.data

import android.os.Handler
import android.os.Looper
import com.geovision.mobile.core.AppLogger
import com.geovision.mobile.core.LayerCatalog
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Manages cache eviction policies to reduce memory pressure:
 * 1. Evict features for layers not currently visible in the viewport
 * 2. Keep layers in LayerCatalog metadata always (memory negligible)
 * 3. Periodic cleanup with configurable interval
 * 4. GC-friendly: avoid allocations in hot paths
 */
object CacheEvictionManager {
    private const val CLEANUP_INTERVAL_MS = 30_000L // 30 seconds
    private val handler = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArrayList<() -> Unit>()

    private val cleanupRunnable = Runnable { performCleanup() }

    private var lastCleanupMs = 0L
    private var totalEvictions = 0

    /**
     * Start periodic cache eviction.
     */
    fun start() {
        handler.post(cleanupRunnable)
        AppLogger.d(AppLogger.Tags.PARSER, "Cache eviction started (interval=${CLEANUP_INTERVAL_MS}ms)")
    }

    /**
     * Stop periodic cache eviction.
     */
    fun stop() {
        handler.removeCallbacks(cleanupRunnable)
        AppLogger.d(AppLogger.Tags.PARSER, "Cache eviction stopped")
    }

    /**
     * Register a listener to be notified when cleanup occurs.
     */
    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    /**
     * Remove a previously registered listener.
     */
    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    /**
     * Perform an immediate cleanup cycle.
     */
    fun performCleanup() {
        val now = System.currentTimeMillis()
        lastCleanupMs = now

        val visibleLayerIds = LayerCatalog.getVisibleLayerIds()
        var evicted = 0

        // Evict features for invisible layers
        for (id in LayerCatalog.allLayerIds()) {
            if (id !in visibleLayerIds) {
                LayerCache.getFeatures(id)?.let {
                    LayerCache.evictFeatures(id)
                    evicted++
                }
            }
        }

        totalEvictions += evicted

        if (evicted > 0 || totalEvictions % 10 == 0) {
            AppLogger.d(AppLogger.Tags.PARSER, "Cleanup: evicted ${evicted} layer(s), total=${totalEvictions}, cache=${LayerCache.stats()}")
        }

        // Notify listeners
        for (listener in listeners) {
            try { listener() } catch (e: Exception) { AppLogger.w(AppLogger.Tags.PARSER, "Listener failed: ${e.message}") }
        }

        // Schedule next cleanup
        handler.removeCallbacks(cleanupRunnable)
        handler.postDelayed(cleanupRunnable, CLEANUP_INTERVAL_MS)
    }

    /**
     * Get simple cleanup stats.
     */
    fun stats(): String = "Evictions: $totalEvictions | Last: ${lastCleanupMs}ms | ${LayerCache.stats()}"
}

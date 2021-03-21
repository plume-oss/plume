package io.github.plume.oss.metrics

import java.util.concurrent.atomic.AtomicLong

/**
 * Tracks metrics surrounding the cache use.
 */
object CacheMetrics {

    private val cacheHits = AtomicLong(0)
    private val cacheMisses = AtomicLong(0)

    /**
     * Increments and returns the number of cache hits.
     */
    fun cacheHit() = cacheHits.incrementAndGet()

    /**
     * Increments and returns the number of cache misses.
     */
    fun cacheMiss() = cacheMisses.incrementAndGet()

    /**
     * Returns total cache hits.
     */
    fun getHits() = cacheHits.get()

    /**
     * Returns total cache misses.
     */
    fun getMisses() = cacheMisses.get()

    /**
     * Resets accumulated metrics.
     */
    fun reset() {
        cacheHits.set(0)
        cacheMisses.set(0)
    }

}
package io.github.plume.oss.options

import java.util.concurrent.TimeUnit

/**
 * Cache options to specify how Plume's caching policy is specified.
 */
object CacheOptions {

    /**
     * How long items should be kept in the cache (after write) before being evicted. Default 1 hour.
     */
    val cacheExpiry: Pair<Long, TimeUnit> = Pair(1L, TimeUnit.HOURS)

    /**
     * How many items may be kept in any given cache. Default 5000.
     */
    val cacheSize: Long = 5_000L
}
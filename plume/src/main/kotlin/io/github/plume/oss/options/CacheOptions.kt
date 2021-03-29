package io.github.plume.oss.options

/**
 * Cache options to specify how Plume's caching policy is specified.
 */
object CacheOptions {

    /**
     * How many items may be kept in total in the cache. Default 10 000.
     */
    var cacheSize: Long = 10_000L
}
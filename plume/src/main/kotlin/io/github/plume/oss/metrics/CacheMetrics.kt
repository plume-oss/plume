/*
 * Copyright 2021 Plume Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
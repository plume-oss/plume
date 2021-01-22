package io.github.plume.oss.util

import io.github.plume.oss.drivers.IDriver
import java.util.concurrent.ConcurrentHashMap

object PlumeKeyProvider {

    /**
     * The size of IDs to acquire per thread. Once this is finished, the thread will acquire another pool of IDs of this
     * size.
     */
    var keyPoolSize = 1000

    /**
     * This holds the key pools assigned to each thread. Writes will happen only on initialization or once a pool is
     * depleted. Reads happen whenever an ID is required. For this reason concurrent hash maps provide fast reads with
     * a negligible slow write.
     */
    private val keyPoolMap = ConcurrentHashMap<Thread, MutableList<Long>>()

    /**
     * Returns a new ID for the current thread to assign a vertex to.
     *
     *  @param d The driver to check for available IDs with.
     * @return a new ID.
     */
    fun getNewId(d: IDriver): Long {
        val t = Thread.currentThread()
        keyPoolMap.computeIfAbsent(t) { generateNewIdPool(d) }
        if (keyPoolMap[t]!!.isEmpty()) keyPoolMap[t] = generateNewIdPool(d)
        return keyPoolMap[t]?.removeFirstOrNull() ?: 1L
    }

    private fun generateNewIdPool(d: IDriver): MutableList<Long> {
        // Find the max ID among the pools
        val currentMax = keyPoolMap.values.flatten().maxOrNull() ?: 0
        val freeIds = mutableSetOf<Long>()
        while (freeIds.size < keyPoolSize) {
            // Choose a lower bound, the max among the pools or the max among the free IDs found
            val currentSetMax = (freeIds.maxOrNull() ?: -1L) + 1
            val lowerBound = if (currentMax > currentSetMax) currentMax else currentSetMax
            val upperBound = lowerBound + keyPoolSize
            // Take the difference of the range and taken IDs to add available IDs
            val xs = (lowerBound..upperBound).toSet()
            val takenIds = d.getVertexIds(lowerBound, upperBound)
            val availableIds = xs.minus(takenIds)
            freeIds.addAll(availableIds)
        }
        return freeIds.toMutableList()
    }

}
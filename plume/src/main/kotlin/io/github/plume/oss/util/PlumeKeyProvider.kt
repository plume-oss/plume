package io.github.plume.oss.util

import io.github.plume.oss.drivers.IOverridenIdDriver
import java.util.concurrent.ConcurrentHashMap

object PlumeKeyProvider {

    /**
     * The size of IDs to acquire per thread. Once this is finished, the thread will acquire another pool of IDs of this
     * size. This value can never be set to < 0 and if the value overflows it will default to 1000.
     */
    var keyPoolSize = 1000
        set(value) { if (value > 0) field = value; if (field < 0) field = 1000  }

    /**
     * This holds the key pools assigned to each thread. Writes will happen only on initialization or once a pool is
     * depleted. Reads happen whenever an ID is required. For this reason concurrent hash maps provide fast reads with
     * a negligible slow write.
     */
    private val keyPoolMap = ConcurrentHashMap<Thread, MutableList<Long>>()

    /**
     * Returns a new ID for the current thread to assign a vertex to.
     *
     * @param d The driver to check for available IDs with.
     * @return a new ID.
     */
    fun getNewId(d: IOverridenIdDriver): Long {
        val t = Thread.currentThread()
        keyPoolMap.computeIfAbsent(t) { generateNewIdPool(d) }
        if (keyPoolMap[t]!!.isEmpty()) keyPoolMap[t] = generateNewIdPool(d)
        return keyPoolMap[t]?.removeFirstOrNull() ?: 1L
    }

    private fun generateNewIdPool(d: IOverridenIdDriver): MutableList<Long> {
        // Find the max ID among the pools
        var currentMax = keyPoolMap.values.flatten().maxOrNull() ?: -1L
        val freeIds = mutableSetOf<Long>()
        var oldSize: Int
        while (freeIds.size < keyPoolSize) {
            // Choose a lower bound, the max among the pools or the max among the free IDs found
            val lowerBound = currentMax + 1
            val upperBound = lowerBound + keyPoolSize
            // Take the difference of the range and taken IDs to add available IDs
            val xs = (lowerBound..upperBound).toSet()
            val takenIds = d.getVertexIds(lowerBound, upperBound)
            val availableIds = xs.minus(takenIds)
            oldSize = freeIds.size
            freeIds.addAll(availableIds)
            if (freeIds.size == oldSize) currentMax += keyPoolSize + 1
        }
        return freeIds.toMutableList()
    }

    /**
     * In the case key pools need to be regenerated they can be cleared here.
     */
    fun clearKeyPools() = keyPoolMap.clear()

}
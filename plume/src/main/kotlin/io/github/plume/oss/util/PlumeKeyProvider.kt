package io.github.plume.oss.util

import io.github.plume.oss.drivers.IOverridenIdDriver

object PlumeKeyProvider {

    /**
     * The size of how many IDs to assign and hold in a pool at a given time. Default is 100 000.
     */
    var keyPoolSize = 100_000
        set(value) {
            if (value > 0) field = value
        }

    /**
     * This holds the keys. Writes will happen only on initialization or once a pool is
     * depleted. Reads happen whenever an ID is required.
     */
    private val keySet = mutableListOf<Long>()

    /**
     * Returns a new ID to assign a vertex to.
     *
     * @param d The driver to check for available IDs with.
     * @return a new ID.
     */
    fun getNewId(d: IOverridenIdDriver): Long {
        if (keySet.isEmpty()) keySet.addAll(generateNewIdPool(d))
        return keySet.removeFirst()
    }

    private fun generateNewIdPool(d: IOverridenIdDriver): MutableList<Long> {
        // Find the max ID among the pools
        var currentMax = keySet.maxOrNull() ?: -1L
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
    fun clearKeyPools() = keySet.clear()

}
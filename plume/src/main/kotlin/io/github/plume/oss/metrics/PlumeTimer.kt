package io.github.plume.oss.metrics

/**
 * Timer to track time elapsed for Plume operations.
 */
object PlumeTimer {

    private val totalTimes = mutableMapOf(
        ExtractorTimeKey.LOADING_AND_COMPILING to 0L,
        ExtractorTimeKey.UNIT_GRAPH_BUILDING to 0L,
        ExtractorTimeKey.DATABASE_WRITE to 0L,
        ExtractorTimeKey.DATABASE_READ to 0L,
        ExtractorTimeKey.SCPG_PASSES to 0L
    )

    private val stopwatch = mutableMapOf(
        ExtractorTimeKey.LOADING_AND_COMPILING to System.nanoTime(),
        ExtractorTimeKey.UNIT_GRAPH_BUILDING to System.nanoTime(),
        ExtractorTimeKey.DATABASE_WRITE to System.nanoTime(),
        ExtractorTimeKey.DATABASE_READ to System.nanoTime(),
        ExtractorTimeKey.SCPG_PASSES to System.nanoTime()
    )

    /**
     * Starts a timer for the given operation as per [ExtractorTimeKey].
     *
     * @param key The key(s) on which to start the timer on.
     */
    fun startTimerOn(vararg key: ExtractorTimeKey) = apply {
        key.forEach { stopwatch[it] = System.nanoTime() }
    }

    /**
     * Stops a timer for the given operation as per [ExtractorTimeKey].
     *
     * @param key The key(s) on which to stop the timer on.
     */
    fun stopTimerOn(vararg key: ExtractorTimeKey) = apply {
        key.forEach {
            totalTimes.computeIfPresent(it) { u, t ->
                val stopTime = stopwatch.getOrDefault(u, 0L)
                if (stopTime != 0L) t + (System.nanoTime() - stopTime)
                else t
            }
            stopwatch[it] = 0L
        }
    }

    /**
     * Measures the time the given function takes to complete. This wraps the function with [startTimerOn] and
     * [stopTimerOn] with all the given [ExtractorTimeKey]s.
     *
     * @param key The key(s) on which to measure.
     * @param f The function to measure.
     */
    fun measure(vararg key: ExtractorTimeKey, f: () -> Unit) {
        startTimerOn(*key)
        f()
        stopTimerOn(*key)
    }

    /**
     * Stops all timers.
     */
    fun stopAll() = apply {
        this.stopTimerOn(*ExtractorTimeKey.values())
    }

    /**
     * Resets all timers.
     */
    fun reset() = apply {
        ExtractorTimeKey.values().forEach { totalTimes[it] = 0L }
    }

    /**
     * Gets all the recorded times.
     */
    fun getTimes(): Map<ExtractorTimeKey, Long> = totalTimes.toMap()

}

enum class ExtractorTimeKey {
    LOADING_AND_COMPILING,
    UNIT_GRAPH_BUILDING,
    BASE_CPG_BUILDING,
    DATABASE_WRITE,
    DATABASE_READ,
    SCPG_PASSES
}


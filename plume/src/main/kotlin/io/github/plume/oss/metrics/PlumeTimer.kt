package io.github.plume.oss.metrics

/**
 * Timer to track time elapsed for Plume operations.
 */
object PlumeTimer {

    private val totalTimes = mutableMapOf(
        ExtractorTimeKey.COMPILING_AND_UNPACKING to 0L,
        ExtractorTimeKey.SOOT to 0L,
        ExtractorTimeKey.BASE_CPG_BUILDING to 0L,
        ExtractorTimeKey.PROGRAM_STRUCTURE_BUILDING to 0L,
        ExtractorTimeKey.DATABASE_WRITE to 0L,
        ExtractorTimeKey.DATABASE_READ to 0L,
        ExtractorTimeKey.DATA_FLOW_PASS to 0L
    )

    private val stopwatch = mutableMapOf(
        ExtractorTimeKey.COMPILING_AND_UNPACKING to System.nanoTime(),
        ExtractorTimeKey.SOOT to System.nanoTime(),
        ExtractorTimeKey.BASE_CPG_BUILDING to System.nanoTime(),
        ExtractorTimeKey.PROGRAM_STRUCTURE_BUILDING to System.nanoTime(),
        ExtractorTimeKey.DATABASE_WRITE to System.nanoTime(),
        ExtractorTimeKey.DATABASE_READ to System.nanoTime(),
        ExtractorTimeKey.DATA_FLOW_PASS to System.nanoTime()
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
    /**
     * This is how long it takes to compile .java files or unpack JAR files, and move all .class files to the temporary
     * build directory. This is wall clock time.
     */
    COMPILING_AND_UNPACKING,

    /**
     * Wall clock time taken by Soot to parse classes, build unit graphs, call graphs, and class hierarchy.
     */
    SOOT,

    /**
     * Wall clock time taken to build AST, CFG, PDG, and call graph onto the database.
     */
    BASE_CPG_BUILDING,

    /**
     * Wall clock time taken to build type, package, and file information.
     */
    PROGRAM_STRUCTURE_BUILDING,

    /**
     * CPU time spent on database writes.
     */
    DATABASE_WRITE,

    /**
     * CPU time spent on database reads.
     */
    DATABASE_READ,

    /**
     * Wall clock time spent running semantic code property graph passes from [io.shiftleft.dataflowengineoss.passes].
     */
    DATA_FLOW_PASS
}


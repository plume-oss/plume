package io.github.plume.oss.metrics

/**
 * Timer to track time elapsed for Plume operations.
 */
object PlumeTimer {

    private val totalTimes = mutableMapOf<ExtractorTimeKey, Long>()

    init {
        ExtractorTimeKey.values().forEach { totalTimes[it] = 0L }
    }

    /**
     * Measures the time the given function takes to complete.
     *
     * @param key The key(s) on which to measure.
     * @param f The function to measure.
     */
    fun measure(key: ExtractorTimeKey, f: () -> Unit) {
        val start = System.nanoTime()
        f()
        val totalTime = System.nanoTime() - start
        totalTimes[key] = totalTimes.getOrDefault(key, 0L) + totalTime
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


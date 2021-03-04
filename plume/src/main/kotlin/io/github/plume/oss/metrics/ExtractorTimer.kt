package io.github.plume.oss.metrics

object ExtractorTimer {

    private val totalTimes = mutableMapOf(
        ExtractorTimeKey.LOADING_AND_COMPILING to 0L,
        ExtractorTimeKey.UNIT_GRAPH_BUILDING to 0L,
        ExtractorTimeKey.DATABASE_WRITE to 0L,
        ExtractorTimeKey.DATABASE_READ to 0L,
        ExtractorTimeKey.CPG_PASSES to 0L
    )

    private val stopwatch = mutableMapOf(
        ExtractorTimeKey.LOADING_AND_COMPILING to System.nanoTime(),
        ExtractorTimeKey.UNIT_GRAPH_BUILDING to System.nanoTime(),
        ExtractorTimeKey.DATABASE_WRITE to System.nanoTime(),
        ExtractorTimeKey.DATABASE_READ to System.nanoTime(),
        ExtractorTimeKey.CPG_PASSES to System.nanoTime()
    )

    fun startTimerOn(vararg key: ExtractorTimeKey) = apply {
        key.forEach { stopwatch[it] = System.nanoTime() }
    }

    fun stopTimerOn(vararg key: ExtractorTimeKey) = apply {
        key.forEach {
            totalTimes.computeIfPresent(it) { u, t ->
                t + (System.nanoTime() - stopwatch.getOrDefault(u, System.nanoTime()))
            }
        }
    }

    fun stopAll() = apply {
        this.stopTimerOn(*ExtractorTimeKey.values())
    }

    fun reset() = apply {
        ExtractorTimeKey.values().forEach { totalTimes[it] = 0L }
    }

    fun getTimes(): Map<ExtractorTimeKey, Long> = totalTimes.toMap()

}

enum class ExtractorTimeKey {
    LOADING_AND_COMPILING,
    UNIT_GRAPH_BUILDING,
    BASE_CPG_BUILDING,
    DATABASE_WRITE,
    DATABASE_READ,
    CPG_PASSES
}


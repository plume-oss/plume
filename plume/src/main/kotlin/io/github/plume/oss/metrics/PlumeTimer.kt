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

/**
 * Timer to track time elapsed for Plume operations.
 */
object PlumeTimer {

    private val extractorTimes = mutableMapOf<ExtractorTimeKey, Long>()
    private val driverTimes = mutableMapOf<DriverTimeKey, Long>()

    init {
        reset()
    }

    private fun measureFunctionTime(f: () -> Unit): Long {
        val start = System.nanoTime()
        f()
        return System.nanoTime() - start
    }

    /**
     * Measures the time the given function takes to complete.
     *
     * @param key The key(s) on which to measure.
     * @param f The function to measure.
     */
    fun measure(key: ExtractorTimeKey, f: () -> Unit) {
        extractorTimes[key] = extractorTimes.getOrDefault(key, 0L) + measureFunctionTime(f)
    }

    /**
     * Measures the time the given function takes to complete.
     *
     * @param key The key(s) on which to measure.
     * @param f The function to measure.
     */
    fun measure(key: DriverTimeKey, f: () -> Unit) {
        driverTimes[key] = driverTimes.getOrDefault(key, 0L) + measureFunctionTime(f)
    }

    /**
     * Resets all timers.
     */
    fun reset() = apply {
        ExtractorTimeKey.values().forEach { extractorTimes[it] = 0L }
        DriverTimeKey.values().forEach { driverTimes[it] = 0L }
    }

    /**
     * Gets all the recorded extractor times.
     */
    fun getExtractorTimes(): Map<ExtractorTimeKey, Long> = extractorTimes.toMap()

    /**
     * Gets all the recorded driver times.
     */
    fun getDriverTimes(): Map<DriverTimeKey, Long> = driverTimes.toMap()

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
     * Wall clock time spent running semantic code property graph passes from [io.shiftleft.dataflowengineoss.passes].
     */
    DATA_FLOW_PASS
}

enum class DriverTimeKey {
    /**
     * CPU time spent on database writes.
     */
    DATABASE_WRITE,

    /**
     * CPU time spent on database reads.
     */
    DATABASE_READ,

    /**
     * The time spent opening a socket/connection when using a remote database or importing/creating a graph in memory
     * when using an embedded database.
     */
    CONNECT_DESERIALIZE,

    /**
     * The time spent closing a socket/connection when using a remote databse or exporting/freeing a graph from memory
     * when using an embedded database.
     */
    DISCONNECT_SERIALIZE
}

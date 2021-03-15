/*
 * Copyright 2020 David Baker Effendi
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
package io.github.plume.oss.options

/**
 * Provides options for configuring how Plume extracts the CPG from bytecode.
 */
object ExtractorOptions {

    /**
     * The call graph algorithm to specify for Soot to make use of during points-to-analysis. Choosing
     * [CallGraphAlg.NONE] will result in a intraprocedural CPG only.
     */
    var callGraphAlg = CallGraphAlg.CHA

    /**
     * A map to specify configuration options for Soot's SPARK call graph algorithm. An exhaustive list of these options
     * can be found [here](https://soot-build.cs.uni-paderborn.de/public/origin/develop/soot/soot-develop/options/soot_options.htm#phase_5_2).
     */
    val sparkOpts = mutableMapOf<String, String>()

    /**
     * Specifies how large each chunk should be when giving a thread method bodies to project into base CPGs.
     */
    var methodChunkSize = 25

    init {
        sparkOpts["verbose"] = "false"
        sparkOpts["propagator"] = "worklist"
        sparkOpts["simple-edges-bidirectional"] = "false"
        sparkOpts["on-fly-cg"] = "true"
        sparkOpts["set-impl"] = "double"
        sparkOpts["double-set-old"] = "hybrid"
        sparkOpts["double-set-new"] = "hybrid"
        sparkOpts["enabled"] = "true"
    }

    /**
     * The call graph algorithms Plume supports.
     */
    enum class CallGraphAlg {
        NONE,
        CHA,
        SPARK
    }
}
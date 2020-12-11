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
package io.github.plume.oss.graph

import soot.toolkits.graph.BriefUnitGraph
import io.github.plume.oss.domain.models.vertices.MethodVertex

/**
 * The interface for classes which build the code property graph from Soot data should implement.
 */
interface IGraphBuilder {
    /**
     * Builds the graph implementing the interface.
     *
     * @param graph The [BriefUnitGraph] of a method body to build the graph off of.
     * @return The [MethodVertex] at the root of the CPG generated from the given [BriefUnitGraph].
     */
    fun buildMethodBody(graph: BriefUnitGraph)
}
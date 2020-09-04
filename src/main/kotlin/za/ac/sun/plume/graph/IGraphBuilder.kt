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
package za.ac.sun.plume.graph

import soot.SootMethod
import soot.toolkits.graph.BriefUnitGraph
import za.ac.sun.plume.domain.models.PlumeVertex

/**
 * The interface for classes which build the code property graph from Soot data should implement.
 */
interface IGraphBuilder {
    /**
     * Builds the graph implementing the interface.
     *
     * @param mtd The [SootMethod] in order to obtain method head information from.
     * @param graph The [BriefUnitGraph] of a method body to build the graph off of.
     * @param sootToPlume A pointer to the map that keeps track of the Soot object to its respective [PlumeVertex].
     */
    fun build(mtd: SootMethod, graph: BriefUnitGraph, sootToPlume: MutableMap<Any, MutableList<PlumeVertex>>)
}
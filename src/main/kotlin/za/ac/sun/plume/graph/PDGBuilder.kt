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

import org.apache.logging.log4j.LogManager
import soot.Local
import soot.SootMethod
import soot.toolkits.graph.BriefUnitGraph
import za.ac.sun.plume.domain.enums.EdgeLabel
import za.ac.sun.plume.domain.models.PlumeVertex
import za.ac.sun.plume.domain.models.vertices.CallVertex
import za.ac.sun.plume.domain.models.vertices.IdentifierVertex
import za.ac.sun.plume.domain.models.vertices.LocalVertex
import za.ac.sun.plume.domain.models.vertices.MethodParameterInVertex
import za.ac.sun.plume.drivers.IDriver

class PDGBuilder(private val driver: IDriver) : IGraphBuilder {
    private val logger = LogManager.getLogger(PDGBuilder::javaClass)
    private lateinit var graph: BriefUnitGraph
    private lateinit var sootToPlume: MutableMap<Any, MutableList<PlumeVertex>>

    override fun build(mtd: SootMethod, graph: BriefUnitGraph, sootToPlume: MutableMap<Any, MutableList<PlumeVertex>>) {
        logger.debug("Building PDG for ${mtd.declaration}")
        this.graph = graph
        this.sootToPlume = sootToPlume
        (this.graph.body.parameterLocals + this.graph.body.locals).forEach { projectLocalVariable(it) }
    }

    private fun projectLocalVariable(local: Local) {
        val src = sootToPlume[local]?.first { it is LocalVertex || it is MethodParameterInVertex }
        val identifierVertices = sootToPlume[local]?.filter { it is IdentifierVertex || it is CallVertex }
        identifierVertices?.forEach {
            if (src != null) {
                driver.addEdge(src, it, EdgeLabel.REF)
            }
        }
    }

}
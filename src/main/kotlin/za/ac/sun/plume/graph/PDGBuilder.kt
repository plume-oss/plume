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
import soot.jimple.*
import soot.toolkits.graph.BriefUnitGraph
import za.ac.sun.plume.domain.enums.EdgeLabel
import za.ac.sun.plume.domain.models.PlumeVertex
import za.ac.sun.plume.domain.models.vertices.*
import za.ac.sun.plume.drivers.IDriver

/**
 * The [IGraphBuilder] that constructs the program dependence edges in the graph.
 *
 * @param driver The driver to build the PDG with.
 * @param sootToPlume A pointer to the map that keeps track of the Soot object to its respective [PlumeVertex].
 */
class PDGBuilder(private val driver: IDriver, private val sootToPlume: MutableMap<Any, MutableList<PlumeVertex>>) : IGraphBuilder {
    private val logger = LogManager.getLogger(PDGBuilder::javaClass)
    private lateinit var graph: BriefUnitGraph

    override fun build(mtd: SootMethod, graph: BriefUnitGraph) {
        logger.debug("Building PDG for ${mtd.declaration}")
        this.graph = graph
        // Identifier REF edges
        (this.graph.body.parameterLocals + this.graph.body.locals).forEach { projectLocalVariable(it) }
        // Operator and Cast ARGUMENT edges
        this.graph.body.units.filterIsInstance<AssignStmt>().map { projectCallArg(it); it.rightOp }.forEach {
            when (it) {
                is CastExpr -> projectCallArg(it)
                is BinopExpr -> projectCallArg(it)
                is InvokeExpr -> projectCallArg(it)
            }
        }
        // Control structure condition vertex ARGUMENT edges
        this.graph.body.units.filterIsInstance<IfStmt>().map { it.condition }.forEach(this::projectCallArg)
        // Invoke ARGUMENT edges
        this.graph.body.units
                .filterIsInstance<InvokeStmt>()
                .map { it.invokeExpr as InvokeExpr }
                .forEach { projectCallArg(it) }
    }

    private fun projectCallArg(value: Any) {
        val src = sootToPlume[value]?.firstOrNull { it is CallVertex }
        sootToPlume[value]?.filter { it != src }?.forEach {
            if (src != null) {
                driver.addEdge(src, it, EdgeLabel.ARGUMENT)
            }
        }
    }

    private fun projectLocalVariable(local: Local) {
        val src = sootToPlume[local]?.firstOrNull { it is LocalVertex || it is MethodParameterInVertex }
        val identifierVertices = sootToPlume[local]?.filterIsInstance<IdentifierVertex>()
        identifierVertices?.forEach {
            if (src != null) {
                driver.addEdge(it, src, EdgeLabel.REF)
            }
        }
    }

}
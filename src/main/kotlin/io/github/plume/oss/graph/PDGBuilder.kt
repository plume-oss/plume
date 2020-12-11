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

import org.apache.logging.log4j.LogManager
import soot.Local
import soot.jimple.*
import soot.toolkits.graph.BriefUnitGraph
import io.github.plume.oss.Extractor
import io.github.plume.oss.Extractor.Companion.getSootAssociation
import io.github.plume.oss.domain.enums.EdgeLabel
import io.github.plume.oss.domain.models.PlumeVertex
import io.github.plume.oss.domain.models.vertices.*
import io.github.plume.oss.drivers.IDriver

/**
 * The [IGraphBuilder] that constructs the program dependence edges in the graph.
 *
 * @param driver The driver to build the PDG with.
 */
class PDGBuilder(private val driver: IDriver) : IGraphBuilder {
    private val logger = LogManager.getLogger(PDGBuilder::javaClass)
    private lateinit var graph: BriefUnitGraph

    override fun buildMethodBody(graph: BriefUnitGraph) {
        val mtd = graph.body.method
        logger.debug("Building PDG for ${mtd.declaration}")
        this.graph = graph
        // Identifier REF edges
        (this.graph.body.parameterLocals + this.graph.body.locals).forEach(this::projectLocalVariable)
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
                .forEach(this::projectCallArg)
    }

    private fun projectCallArg(value: Any) {
        val src = getSootAssociation(value)?.firstOrNull { it is CallVertex }
        getSootAssociation(value)?.filter { it != src }?.forEach {
            if (src != null) {
                driver.addEdge(src, it, EdgeLabel.ARGUMENT)
            }
        }
    }

    private fun projectLocalVariable(local: Local) {
        getSootAssociation(local)?.let { assocVertices ->
            assocVertices.filterIsInstance<IdentifierVertex>().forEach { identifierV ->
                assocVertices.firstOrNull { it is LocalVertex || it is MethodParameterInVertex }?.let { src ->
                    driver.addEdge(identifierV, src, EdgeLabel.REF)
                }
            }
        }
    }

}
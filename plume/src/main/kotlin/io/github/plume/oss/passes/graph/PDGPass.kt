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
package io.github.plume.oss.passes.graph

import io.github.plume.oss.GlobalCache
import io.github.plume.oss.drivers.IDriver
import io.github.plume.oss.passes.IUnitGraphPass
import io.shiftleft.codepropertygraph.generated.EdgeTypes.ARGUMENT
import io.shiftleft.codepropertygraph.generated.EdgeTypes.REF
import io.shiftleft.codepropertygraph.generated.nodes.*
import org.apache.logging.log4j.LogManager
import soot.Local
import soot.jimple.*
import soot.toolkits.graph.BriefUnitGraph

/**
 * The [IUnitGraphPass] that constructs the program dependence edges in the graph.
 *
 * @param driver The driver to build the PDG with.
 */
class PDGPass(private val driver: IDriver) : IUnitGraphPass {
    private val logger = LogManager.getLogger(PDGPass::javaClass)
    private lateinit var graph: BriefUnitGraph

    override fun runPass(gs: List<BriefUnitGraph>) = gs.map(::runPassOnGraph)

    private fun runPassOnGraph(g: BriefUnitGraph): BriefUnitGraph {
        val mtd = g.body.method
        logger.debug("Building PDG for ${mtd.declaration}")
        this.graph = g
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
        return g
    }

    private fun projectCallArg(value: Any) {
        GlobalCache.getSootAssoc(value)?.firstOrNull { it is NewCallBuilder }?.let { src ->
            GlobalCache.getSootAssoc(value)?.filterNot { it == src || it is NewArrayInitializerBuilder }?.forEach { tgt ->
                runCatching {
                    driver.addEdge(src, tgt, ARGUMENT)
                }.onFailure { e -> logger.warn(e.message) }
            }
        }
    }

    private fun projectLocalVariable(local: Local) {
        GlobalCache.getSootAssoc(local)?.let { assocVertices ->
            assocVertices.filterIsInstance<NewIdentifierBuilder>().forEach { identifierV ->
                assocVertices.firstOrNull { it is NewLocalBuilder || it is NewMethodParameterInBuilder }?.let { src ->
                    runCatching {
                        driver.addEdge(identifierV, src, REF)
                    }.onFailure { e -> logger.warn(e.message) }
                }
            }
        }
    }
}
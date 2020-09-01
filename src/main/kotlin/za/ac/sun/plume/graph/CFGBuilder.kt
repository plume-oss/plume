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
import soot.SootMethod
import soot.Unit
import soot.jimple.*
import soot.toolkits.graph.BriefUnitGraph
import za.ac.sun.plume.domain.enums.EdgeLabel
import za.ac.sun.plume.domain.models.PlumeVertex
import za.ac.sun.plume.domain.models.vertices.BlockVertex
import za.ac.sun.plume.domain.models.vertices.JumpTargetVertex
import za.ac.sun.plume.drivers.IDriver
import za.ac.sun.plume.util.ExtractorConst.FALSE_TARGET
import za.ac.sun.plume.util.ExtractorConst.TRUE_TARGET

class CFGBuilder(private val driver: IDriver) : IGraphBuilder {
    private val logger = LogManager.getLogger(CFGBuilder::javaClass)
    private lateinit var graph: BriefUnitGraph
    private lateinit var sootToVertex: MutableMap<Any, MutableList<PlumeVertex>>

    override fun build(mtd: SootMethod, graph: BriefUnitGraph, sootToPlume: MutableMap<Any, MutableList<PlumeVertex>>) {
        logger.debug("Building CFG for ${mtd.declaration}")
        this.graph = graph
        this.sootToVertex = sootToPlume
        // Connect entrypoint to the first CFG vertex
        this.graph.heads.forEach { head ->
            graph.getSuccsOf(head).firstOrNull()?.let {
                driver.addEdge(
                        fromV = sootToPlume[mtd]?.first { mtdVertices -> mtdVertices is BlockVertex }!!,
                        toV = sootToPlume[it]?.first()!!,
                        edge = EdgeLabel.CFG
                )
            }
        }
        // Connect all units to their successors
        this.graph.body.units.forEach { projectUnit(it) }
    }

    private fun projectUnit(unit: Unit) {
        when (unit) {
            is GotoStmt -> projectUnit(unit.target)
            is IfStmt -> projectIfStatement(unit)
            is LookupSwitchStmt -> projectLookupSwitch(unit)
            is TableSwitchStmt -> projectTableSwitch(unit)
            else -> {
                val sourceUnit = if (unit is GotoStmt) unit.target else unit
                val sourceVertex = sootToVertex[sourceUnit]?.firstOrNull()
                graph.getSuccsOf(sourceUnit).forEach {
                    val targetUnit = if (it is GotoStmt) it.target else it
                    if (sourceVertex != null) {
                        sootToVertex[targetUnit]?.let { vList -> driver.addEdge(sourceVertex, vList.first(), EdgeLabel.CFG) }
                    }
                }
            }
        }
    }

    private fun projectTableSwitch(unit: TableSwitchStmt) {
        TODO("Not yet implemented")
    }

    private fun projectLookupSwitch(unit: LookupSwitchStmt) {
        TODO("Not yet implemented")
    }

    private fun projectIfStatement(unit: IfStmt) {
        val ifVertices = sootToVertex[unit]!!
        graph.getSuccsOf(unit).forEach {
            val srcVertex = if (it == unit.target) {
                ifVertices.first { vert -> vert is JumpTargetVertex && vert.name == FALSE_TARGET }
            } else {
                ifVertices.first { vert -> vert is JumpTargetVertex && vert.name == TRUE_TARGET }
            }
            sootToVertex[it]?.let { vList ->
                driver.addEdge(ifVertices.first(), srcVertex, EdgeLabel.CFG)
                driver.addEdge(srcVertex, vList.first(), EdgeLabel.CFG)
            }
        }
    }
}
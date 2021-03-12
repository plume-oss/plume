package io.github.plume.oss.passes.graph

import io.github.plume.oss.GlobalCache
import io.github.plume.oss.domain.model.DeltaGraph
import io.github.plume.oss.util.SootToPlumeUtil
import io.shiftleft.codepropertygraph.generated.EdgeTypes
import io.shiftleft.codepropertygraph.generated.nodes.NewMethodBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewNodeBuilder
import org.apache.logging.log4j.LogManager
import soot.toolkits.graph.BriefUnitGraph

class BaseCPGPass(private val g: BriefUnitGraph) {

    private val logger = LogManager.getLogger(BaseCPGPass::javaClass)
    private val builder = DeltaGraph.Builder()
    private var currentLine = -1
    private var currentCol = -1
    private val localCache = mutableMapOf<Any, List<NewNodeBuilder>>()

    fun runPass(): DeltaGraph {
        val mtd = g.body.method
        logger.debug("Building AST for ${mtd.declaration}")
        GlobalCache.getSootAssoc(mtd)?.let { mtdVs ->
            mtdVs.filterIsInstance<NewMethodBuilder>().firstOrNull()?.let { mtdVert ->
                GlobalCache.addSootAssoc(mtd, buildLocals(g, mtdVert))
            }
        }

        return builder.build()
    }

    private fun buildLocals(graph: BriefUnitGraph, mtdVertex: NewMethodBuilder): MutableList<NewNodeBuilder> {
        val localVertices = mutableListOf<NewNodeBuilder>()
        graph.body.parameterLocals
            .mapIndexed { i, local ->
                SootToPlumeUtil.projectMethodParameterIn(local, currentLine, currentCol, i + 1)
                    .apply { GlobalCache.addSootAssoc(local, this) }
            }
            .forEach {
                runCatching {
                    builder.addEdge(mtdVertex, it, EdgeTypes.AST); localVertices.add(it)
                }.onFailure { e -> logger.warn(e.message) }
            }
        graph.body.locals
            .filter { !graph.body.parameterLocals.contains(it) }
            .mapIndexed { i, local ->
                SootToPlumeUtil.projectLocalVariable(local, currentLine, currentCol, i)
                    .apply { GlobalCache.addSootAssoc(local, this) }
            }
            .forEach {
                runCatching {
                    builder.addEdge(mtdVertex, it, EdgeTypes.AST); localVertices.add(it)
                }.onFailure { e -> logger.warn(e.message) }
            }
        return localVertices
    }

}
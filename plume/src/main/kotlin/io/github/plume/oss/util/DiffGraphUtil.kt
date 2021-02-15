package io.github.plume.oss.util

import io.github.plume.oss.domain.mappers.VertexMapper
import io.github.plume.oss.drivers.IDriver
import io.shiftleft.codepropertygraph.generated.nodes.NewNode
import io.shiftleft.passes.DiffGraph
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import overflowdb.Node

/**
 * Utility class to process [DiffGraph] changes.
 */
object DiffGraphUtil {

    private val logger: Logger = LogManager.getLogger(DiffGraphUtil::class.java)

    /**
     * Processes [DiffGraph] operations and applies them to the given [IDriver].
     *
     * @param driver The driver to which the changes are to be applied.
     * @param df The [DiffGraph] from which to extract the changes from.
     */
    fun processDiffGraph(driver: IDriver, df: DiffGraph) {
        df.iterator().foreach { change: DiffGraph.Change ->
            when (change) {
                is io.shiftleft.passes.`DiffGraph$Change$CreateNode` ->
                    driver.addVertex(VertexMapper.mapToVertex(change.node()))
                is io.shiftleft.passes.`DiffGraph$Change$RemoveNode` ->
                    driver.deleteVertex(change.nodeId())
                is io.shiftleft.passes.`DiffGraph$Change$RemoveEdge` -> {
                    val edge = change.edge()
                    driver.deleteEdge(
                        src = VertexMapper.mapToVertex(edge.outNode() as Node),
                        tgt = VertexMapper.mapToVertex(edge.inNode() as Node),
                        edge = edge.label()
                    )
                }
                is io.shiftleft.passes.`DiffGraph$Change$CreateEdge` ->
                    driver.addEdge(
                        src = VertexMapper.mapToVertex(change.src() as NewNode),
                        tgt = VertexMapper.mapToVertex(change.dst() as NewNode),
                        edge = change.label()
                    )
                else -> logger.warn("Unsupported DiffGraph operation ${change.javaClass} encountered.")
            }
        }
    }

}

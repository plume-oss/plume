package io.github.plume.oss.util

import io.github.plume.oss.domain.mappers.VertexMapper
import io.github.plume.oss.drivers.IDriver
import io.shiftleft.passes.DiffGraph

object DiffGraphUtil {

    fun processDiffGraph(driver: IDriver, df: DiffGraph) {
        df.iterator().foreach { change: DiffGraph.Change ->
            println(change.javaClass)
            when (change) {
                is io.shiftleft.passes.`DiffGraph$Change$CreateNode` ->
                    driver.addVertex(VertexMapper.mapToVertex(change.node()))
                is io.shiftleft.passes.`DiffGraph$Change$RemoveNode` ->
                    driver.deleteVertex(change.nodeId())
                else -> Unit
            }
        }
    }

}

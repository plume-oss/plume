package com.github.plume.oss.passes.base

import com.github.plume.oss.drivers.IDriver
import com.github.plume.oss.passes.PlumeConcurrentCpgPass.concurrentCreateApply
import io.joern.x2cpg.passes.base.ContainsEdgePass
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.codepropertygraph.generated.nodes.AstNode

object PlumeContainsEdgePass {
  private val producerQueueCapacity = 2 + 4 * Runtime.getRuntime.availableProcessors()
}

class PlumeContainsEdgePass(cpg: Cpg) extends ContainsEdgePass(cpg) with PlumeCpgPassBase {

  override def createAndApply(driver: IDriver): Unit = {
    createApplySerializeAndStore(driver) // Apply to driver
  }

  def createApplySerializeAndStore(driver: IDriver): Unit = {
    import PlumeContainsEdgePass.producerQueueCapacity
    try {
      init()
      concurrentCreateApply[AstNode](
        producerQueueCapacity,
        driver,
        name,
        baseLogger,
        generateParts(),
        cpg,
        (x: DiffGraphBuilder, y: AstNode) => runOnPart(x, y),
        None,
        (newDiff: Int) => {
          nDiffT = newDiff
          nDiffT
        }
      )
    } finally {
      finish()
    }
  }
}

package com.github.plume.oss.passes.concurrent

import com.github.plume.oss.drivers.IDriver
import com.github.plume.oss.passes.PlumeCpgPassBase
import com.github.plume.oss.passes.concurrent.PlumeConcurrentCpgPass.concurrentCreateApply
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.codepropertygraph.generated.nodes.AstNode
import io.shiftleft.passes.DiffGraph
import io.shiftleft.semanticcpg.passes.base.ContainsEdgePass

object PlumeContainsEdgePass {
  private val producerQueueCapacity = 2 + 4 * Runtime.getRuntime.availableProcessors()
}

class PlumeContainsEdgePass(cpg: Cpg) extends ContainsEdgePass(cpg) with PlumeCpgPassBase {

  override def createAndApply(driver: IDriver): Unit = {
    createApplySerializeAndStore(driver) // Apply to driver
  }

  def createApplySerializeAndStore(driver: IDriver): Unit = {
    import PlumeContainsEdgePass.producerQueueCapacity
    concurrentCreateApply[AstNode](
      producerQueueCapacity,
      driver,
      name,
      baseLogger,
      _ => init(),
      _ => generateParts(),
      cpg,
      (x: DiffGraph.Builder, y: AstNode) => runOnPart(x, y),
      _ => finish()
    )
  }
}

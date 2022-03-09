package com.github.plume.oss.passes.reachingdef

import com.github.plume.oss.drivers.IDriver
import com.github.plume.oss.passes.PlumeForkJoinParallelCpgPass.forkJoinSerializeAndStore
import com.github.plume.oss.passes.base.PlumeCpgPassBase
import io.joern.dataflowengineoss.passes.reachingdef.ReachingDefPass
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.codepropertygraph.generated.nodes.Method
import io.shiftleft.passes.KeyPool
import io.shiftleft.semanticcpg.language._

class PlumeReachingDefPass(
    cpg: Cpg,
    keyPool: Option[KeyPool] = None,
    unchangedTypes: Set[String] = Set.empty[String]
) extends ReachingDefPass(cpg)
    with PlumeCpgPassBase {

  override def generateParts(): Array[Method] = cpg.method.internal.iterator.filterNot { m =>
    val typeFullName = m.fullName.substring(0, m.fullName.lastIndexOf('.'))
    unchangedTypes.contains(typeFullName)
  }.toArray

  def createAndApply(driver: IDriver): Unit = {
    createApplySerializeAndStore(driver) // Apply to driver
  }

  def createApplySerializeAndStore(driver: IDriver): Unit = {
    try {
      init()
      forkJoinSerializeAndStore(
        driver,
        name,
        cpg,
        baseLogger,
        generateParts(),
        (builder: DiffGraphBuilder, part: Method) => runOnPart(builder, part),
        keyPool
      )
    } finally {
      finish()
    }
  }

}

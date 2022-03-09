package com.github.plume.oss.passes.controlflow.cfgdominator

import com.github.plume.oss.drivers.IDriver
import com.github.plume.oss.passes.PlumeForkJoinParallelCpgPass.forkJoinSerializeAndStore
import com.github.plume.oss.passes.base.PlumeCpgPassBase
import io.joern.x2cpg.passes.controlflow.cfgdominator.CfgDominatorPass
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.codepropertygraph.generated.nodes.Method
import io.shiftleft.passes.KeyPool

class PlumeCfgDominatorPass(cpg: Cpg, keyPool: Option[KeyPool] = None)
    extends CfgDominatorPass(cpg)
    with PlumeCpgPassBase {

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

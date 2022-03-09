package com.github.plume.oss.passes.controlflow.codepencegraph

import com.github.plume.oss.drivers.IDriver
import com.github.plume.oss.passes.PlumeForkJoinParallelCpgPass.forkJoinSerializeAndStore
import com.github.plume.oss.passes.base.PlumeCpgPassBase
import io.joern.x2cpg.passes.controlflow.codepencegraph.CdgPass
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.codepropertygraph.generated.nodes.Method
import io.shiftleft.passes.KeyPool

class PlumeCdgPass(cpg: Cpg, keyPool: Option[KeyPool] = None)
    extends CdgPass(cpg)
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

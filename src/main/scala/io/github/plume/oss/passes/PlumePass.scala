package io.github.plume.oss.passes

import io.github.plume.oss.drivers.IDriver
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.codepropertygraph.generated.nodes.Method
import io.shiftleft.passes.{DiffGraph, KeyPool}
import io.shiftleft.semanticcpg.passes.cfgcreation.CfgCreator
import io.shiftleft.semanticcpg.passes.{CfgCreationPass, FileCreationPass}
import io.shiftleft.semanticcpg.passes.metadata.MetaDataPass
import io.shiftleft.semanticcpg.passes.namespacecreator.NamespaceCreator

class PlumeMetaDataPass(cpg: Cpg, language: String, keyPool: Option[KeyPool]) extends MetaDataPass(cpg, language, keyPool) {

  def createAndApply(driver: IDriver): Unit = {
    withStartEndTimesLogged {
      run().map(diffGraph => DiffGraph.Applier.applyDiff(diffGraph, cpg, undoable = false, keyPool)).foreach(driver.bulkTx)
    }
  }

}

class PlumeNamespaceCreator(cpg: Cpg, keyPool: Option[KeyPool]) extends NamespaceCreator(cpg) {

  def createAndApply(driver: IDriver): Unit = {
    withStartEndTimesLogged {
      run().map(diffGraph => DiffGraph.Applier.applyDiff(diffGraph, cpg, undoable = false, keyPool)).foreach(driver.bulkTx)
    }
  }

}

class PlumeFileCreationPass(cpg: Cpg, keyPool: Option[KeyPool]) extends FileCreationPass(cpg) {

  def createAndApply(driver: IDriver): Unit = {
    withStartEndTimesLogged {
      run().map(diffGraph => DiffGraph.Applier.applyDiff(diffGraph, cpg, undoable = false, keyPool)).foreach(driver.bulkTx)
    }
  }

}
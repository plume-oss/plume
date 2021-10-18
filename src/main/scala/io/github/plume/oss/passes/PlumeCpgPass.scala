package io.github.plume.oss.passes

import io.github.plume.oss.drivers.IDriver
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.codepropertygraph.generated.nodes.Method
import io.shiftleft.passes.{DiffGraph, KeyPool}
import io.shiftleft.semanticcpg.passes.cfgcreation.CfgCreator
import io.shiftleft.semanticcpg.passes.languagespecific.fuzzyc.MethodStubCreator
import io.shiftleft.semanticcpg.passes.{CfgCreationPass, FileCreationPass}
import io.shiftleft.semanticcpg.passes.metadata.MetaDataPass
import io.shiftleft.semanticcpg.passes.methoddecorations.MethodDecoratorPass
import io.shiftleft.semanticcpg.passes.namespacecreator.NamespaceCreator
import io.shiftleft.semanticcpg.passes.typenodes.{TypeDeclStubCreator, TypeNodePass}

class PlumeMetaDataPass(cpg: Cpg, language: String, keyPool: Option[KeyPool])
    extends MetaDataPass(cpg, language, keyPool) {

  def createAndApply(driver: IDriver): Unit = {
    withStartEndTimesLogged {
      run()
        .map(diffGraph => DiffGraph.Applier.applyDiff(diffGraph, cpg, undoable = false, keyPool))
        .foreach(driver.bulkTx)
    }
  }

}

class PlumeNamespaceCreator(cpg: Cpg, keyPool: Option[KeyPool]) extends NamespaceCreator(cpg) {

  def createAndApply(driver: IDriver): Unit = {
    withStartEndTimesLogged {
      run()
        .map(diffGraph => DiffGraph.Applier.applyDiff(diffGraph, cpg, undoable = false, keyPool))
        .foreach(driver.bulkTx)
    }
  }

}

class PlumeFileCreationPass(cpg: Cpg, keyPool: Option[KeyPool]) extends FileCreationPass(cpg) {

  def createAndApply(driver: IDriver): Unit = {
    withStartEndTimesLogged {
      run()
        .map(diffGraph => DiffGraph.Applier.applyDiff(diffGraph, cpg, undoable = false, keyPool))
        .foreach(driver.bulkTx)
    }
  }

}

class PlumeTypeNodePass(usedTypes: List[String], cpg: Cpg, keyPool: Option[KeyPool])
    extends TypeNodePass(usedTypes, cpg, keyPool) {
  def createAndApply(driver: IDriver): Unit = {
    withStartEndTimesLogged {
      run()
        .map(diffGraph => DiffGraph.Applier.applyDiff(diffGraph, cpg, undoable = false, keyPool))
        .foreach(driver.bulkTx)
    }
  }
}

class PlumeTypeDeclStubCreator(cpg: Cpg, keyPool: Option[KeyPool]) extends TypeDeclStubCreator(cpg) {
  def createAndApply(driver: IDriver): Unit = {
    withStartEndTimesLogged {
      run()
        .map(diffGraph => DiffGraph.Applier.applyDiff(diffGraph, cpg, undoable = false, keyPool))
        .foreach(driver.bulkTx)
    }
  }
}

class PlumeMethodDecoratorPass(cpg: Cpg, keyPool: Option[KeyPool]) extends MethodDecoratorPass(cpg) {
  def createAndApply(driver: IDriver): Unit = {
    withStartEndTimesLogged {
      run()
        .map(diffGraph => DiffGraph.Applier.applyDiff(diffGraph, cpg, undoable = false, keyPool))
        .foreach(driver.bulkTx)
    }
  }
}
package io.github.plume.oss.passes

import io.github.plume.oss.drivers.IDriver
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.codepropertygraph.generated.PropertyNames
import io.shiftleft.codepropertygraph.generated.nodes.{AbstractNode, NewNode, StoredNode}
import io.shiftleft.passes.DiffGraph.Change
import io.shiftleft.passes.{DiffGraph, KeyPool}
import io.shiftleft.semanticcpg.passes.FileCreationPass
import io.shiftleft.semanticcpg.passes.metadata.MetaDataPass
import io.shiftleft.semanticcpg.passes.methoddecorations.MethodDecoratorPass
import io.shiftleft.semanticcpg.passes.namespacecreator.NamespaceCreator
import io.shiftleft.semanticcpg.passes.typenodes.{TypeDeclStubCreator, TypeNodePass}

class PlumeMetaDataPass(
    cpg: Cpg,
    language: String,
    keyPool: Option[KeyPool],
    blacklist: Set[String] = Set()
) extends MetaDataPass(cpg, language, keyPool)
    with PlumeCpgPassBase {

  override def createAndApply(driver: IDriver): Unit = {
    withStartEndTimesLogged {
      run()
        .map(dg => PlumeCpgPass.filterDiffGraph(dg, PropertyNames.LANGUAGE, blacklist))
        .map(diffGraph => DiffGraph.Applier.applyDiff(diffGraph, cpg, undoable = false, keyPool))
        .foreach(driver.bulkTx)
    }
  }

}

class PlumeNamespaceCreator(cpg: Cpg, keyPool: Option[KeyPool], blacklist: Set[String] = Set())
    extends NamespaceCreator(cpg)
    with PlumeCpgPassBase {

  override def createAndApply(driver: IDriver): Unit = {
    withStartEndTimesLogged {
      run()
        .map(dg => PlumeCpgPass.filterDiffGraph(dg, PropertyNames.NAME, blacklist))
        .map(diffGraph => DiffGraph.Applier.applyDiff(diffGraph, cpg, undoable = false, keyPool))
        .foreach(driver.bulkTx)
    }
  }

}

class PlumeFileCreationPass(cpg: Cpg, keyPool: Option[KeyPool], blacklist: Set[String] = Set())
    extends FileCreationPass(cpg)
    with PlumeCpgPassBase {

  override def createAndApply(driver: IDriver): Unit = {
    withStartEndTimesLogged {
      run()
        .map(dg => PlumeCpgPass.filterDiffGraph(dg, PropertyNames.NAME, blacklist))
        .map(diffGraph => DiffGraph.Applier.applyDiff(diffGraph, cpg, undoable = false, keyPool))
        .foreach(driver.bulkTx)
    }
  }

}

class PlumeTypeNodePass(
    usedTypes: List[String],
    cpg: Cpg,
    keyPool: Option[KeyPool],
    blacklist: Set[String] = Set()
) extends TypeNodePass(usedTypes, cpg, keyPool)
    with PlumeCpgPassBase {

  override def createAndApply(driver: IDriver): Unit = {
    withStartEndTimesLogged {
      run()
        .map(dg => PlumeCpgPass.filterDiffGraph(dg, PropertyNames.FULL_NAME, blacklist))
        .map(diffGraph => DiffGraph.Applier.applyDiff(diffGraph, cpg, undoable = false, keyPool))
        .foreach(driver.bulkTx)
    }
  }
}

class PlumeTypeDeclStubCreator(cpg: Cpg, keyPool: Option[KeyPool], blacklist: Set[String] = Set())
    extends TypeDeclStubCreator(cpg)
    with PlumeCpgPassBase {

  override def createAndApply(driver: IDriver): Unit = {
    withStartEndTimesLogged {
      run()
        .map(dg => PlumeCpgPass.filterDiffGraph(dg, PropertyNames.FULL_NAME, blacklist))
        .map(diffGraph => DiffGraph.Applier.applyDiff(diffGraph, cpg, undoable = false, keyPool))
        .foreach(driver.bulkTx)
    }
  }
}

class PlumeMethodDecoratorPass(cpg: Cpg, keyPool: Option[KeyPool], blacklist: Set[String] = Set())
    extends MethodDecoratorPass(cpg)
    with PlumeCpgPassBase {

  override def createAndApply(driver: IDriver): Unit = {
    withStartEndTimesLogged {
      run()
        .map(dg =>
          PlumeCpgPass
            .filterDiffGraph(dg, PropertyNames.FULL_NAME, blacklist, rejectAllOnFail = true)
        )
        .map(diffGraph => DiffGraph.Applier.applyDiff(diffGraph, cpg, undoable = false, keyPool))
        .foreach(driver.bulkTx)
    }
  }
}

object PlumeCpgPass {

  def filterDiffGraph(
      dg: DiffGraph,
      key: String,
      blacklist: Set[String],
      rejectAllOnFail: Boolean = false
  ): DiffGraph = {
    val newDg = DiffGraph.newBuilder
    dg.iterator.foreach {
      case Change.CreateNode(node) =>
        if (!blacklist.contains(node.properties.getOrElse(key, "").toString))
          newDg.addNode(node)
      case Change.CreateEdge(src, dst, x, _) =>
        val srcProperty = getPropertyFromAbstractNode[String](src, key)
        val dstProperty = getPropertyFromAbstractNode[String](dst, key)
        if (!blacklist.contains(srcProperty) && !blacklist.contains(dstProperty)) {
          newDg.addEdge(src, dst, x)
        } else if (rejectAllOnFail) {
          return DiffGraph.newBuilder.build()
        }
      case _ =>
    }
    newDg.build()
  }

  private def getPropertyFromAbstractNode[T](node: AbstractNode, key: String): T = {
    node match {
      case x: NewNode    => x.properties.getOrElse(key, "").asInstanceOf[T]
      case x: StoredNode => x.propertiesMap().getOrDefault(key, "").asInstanceOf[T]
      case _             => "".asInstanceOf[T]
    }
  }

}

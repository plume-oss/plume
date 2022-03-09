package com.github.plume.oss.passes.base

import com.github.plume.oss.drivers.IDriver
import com.github.plume.oss.passes.PlumeForkJoinParallelCpgPass.forkJoinSerializeAndStore
import com.github.plume.oss.util.BatchedUpdateUtil
import io.joern.x2cpg.passes.base._
import io.joern.x2cpg.passes.frontend._
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.codepropertygraph.generated.PropertyNames
import io.shiftleft.codepropertygraph.generated.nodes.{AbstractNode, Method, NewNode, StoredNode}
import io.shiftleft.passes.DiffGraph.Change
import io.shiftleft.passes.{DiffGraph, ForkJoinParallelCpgPass, KeyPool}
import overflowdb.BatchedUpdate.DiffGraphBuilder
import overflowdb.traversal.jIteratortoTraversal
import overflowdb.{BatchedUpdate, DetachedNodeData, DetachedNodeGeneric, Node, NodeOrDetachedNode}

abstract class PlumeSimpleCpgPass(cpg: Cpg, outName: String = "", keyPool: Option[KeyPool] = None)
    extends ForkJoinParallelCpgPass[AnyRef](cpg, outName, keyPool) {

  def run(builder: overflowdb.BatchedUpdate.DiffGraphBuilder): Unit

  final override def generateParts(): Array[_ <: AnyRef] = Array[AnyRef](null)

  final override def runOnPart(
      builder: overflowdb.BatchedUpdate.DiffGraphBuilder,
      part: AnyRef
  ): Unit =
    run(builder)
}

class PlumeMetaDataPass(
    cpg: Cpg,
    language: String,
    keyPool: Option[KeyPool],
    blacklist: Set[String] = Set()
) extends MetaDataPass(cpg, language, keyPool)
    with PlumeCpgPassBase {

  def createAndApply(driver: IDriver): Unit = {
    if (blacklist.isEmpty)                 // If not empty then do not generate duplicate meta data nodes
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

class PlumeNamespaceCreator(cpg: Cpg, keyPool: Option[KeyPool], blacklist: Set[String] = Set())
    extends NamespaceCreator(cpg)
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
        keyPool,
        blacklist,
        PropertyNames.NAME
      )
    } finally {
      finish()
    }
  }

}

class PlumeFileCreationPass(cpg: Cpg, keyPool: Option[KeyPool])
    extends FileCreationPass(cpg)
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

class PlumeTypeNodePass(
    usedTypes: List[String],
    cpg: Cpg,
    keyPool: Option[KeyPool],
    blacklist: Set[String] = Set()
) extends TypeNodePass(usedTypes, cpg, keyPool)
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
        keyPool,
        blacklist,
        PropertyNames.FULL_NAME
      )
    } finally {
      finish()
    }
  }

}

class PlumeTypeDeclStubCreator(cpg: Cpg, keyPool: Option[KeyPool], blacklist: Set[String] = Set())
    extends TypeDeclStubCreator(cpg)
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
        keyPool,
        blacklist,
        PropertyNames.FULL_NAME
      )
    } finally {
      finish()
    }
  }

}

class PlumeMethodDecoratorPass(cpg: Cpg, keyPool: Option[KeyPool], blacklist: Set[String] = Set())
    extends MethodDecoratorPass(cpg)
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
        keyPool,
        blacklist,
        PropertyNames.TYPE_FULL_NAME,
        blacklistRejectOnFail = true
      )
    } finally {
      finish()
    }
  }

}

object PlumeCpgPass {

  def filterBatchedDiffGraph(
      dg: BatchedUpdate.DiffGraph,
      key: String,
      blacklist: Set[String],
      rejectAllOnFail: Boolean = false
  ): BatchedUpdate.DiffGraph = {
    val newDg = new DiffGraphBuilder
    dg.iterator.foreach {
      case c: DetachedNodeData =>
        val properties = c match {
          case generic: DetachedNodeGeneric =>
            BatchedUpdateUtil.propertiesFromObjectArray(generic.keyvalues)
          case node: NewNode => node.properties
          case _             => Map.empty[String, Object]
        }
        if (!blacklist.contains(properties.getOrElse(key, "").toString))
          newDg.addNode(c)
      case c: BatchedUpdate.CreateEdge =>
        val srcProperty = getPropertyFromAbstractNode[String](c.src, key)
        val dstProperty = getPropertyFromAbstractNode[String](c.dst, key)
        if (!blacklist.contains(srcProperty) && !blacklist.contains(dstProperty)) {
          newDg.addEdge(c.src, c.dst, c.label)
        } else if (rejectAllOnFail) {
          return (new DiffGraphBuilder).build()
        }
      case _ =>
    }
    newDg.build()
  }

  def filterDiffGraph(
      dg: DiffGraph,
      key: String,
      blacklist: Set[String],
      rejectAllOnFail: Boolean = false
  ): DiffGraph = {
    val newDg = DiffGraph.newBuilder
    dg.iterator.foreach {
      case Change.CreateNode(node)
          if !blacklist.contains(node.properties.getOrElse(key, "").toString) =>
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

  private def getPropertyFromAbstractNode[T](node: NodeOrDetachedNode, key: String): T = {
    node match {
      case generic: DetachedNodeGeneric =>
        BatchedUpdateUtil.propertiesFromObjectArray(generic.keyvalues)(key).asInstanceOf[T]
      case node: NewNode => node.properties(key).asInstanceOf[T]
      case node: Node    => node.property(key).asInstanceOf[T]
    }
  }

}

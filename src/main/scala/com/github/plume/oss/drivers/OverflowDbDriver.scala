package com.github.plume.oss.drivers

import com.github.plume.oss.domain.{SerialReachableByResult, deserializeResultTable}
import com.github.plume.oss.drivers.OverflowDbDriver.newOverflowGraph
import com.github.plume.oss.passes.PlumeDynamicCallLinker
import io.joern.dataflowengineoss.language.toExtendedCfgNode
import io.joern.dataflowengineoss.queryengine._
import io.joern.dataflowengineoss.semanticsloader.{Parser, Semantics}
import io.shiftleft.codepropertygraph.generated._
import io.shiftleft.codepropertygraph.generated.nodes._
import io.shiftleft.codepropertygraph.{Cpg => CPG}
import io.shiftleft.passes.AppliedDiffGraph
import io.shiftleft.passes.DiffGraph.{Change, PackedProperties}
import org.slf4j.LoggerFactory
import overflowdb.traversal.{Traversal, jIteratortoTraversal}
import overflowdb.{Config, Edge, Node}

import java.io.{
  FileInputStream,
  FileOutputStream,
  ObjectInputStream,
  ObjectOutputStream,
  File => JFile
}
import java.nio.file.{Files, Path, Paths}
import java.util.concurrent.ConcurrentHashMap
import scala.collection.mutable
import scala.io.{BufferedSource, Source}
import scala.jdk.CollectionConverters.IteratorHasAsScala
import scala.util.{Failure, Success, Try, Using}

/** Driver to create an OverflowDB database file.
  */
final case class OverflowDbDriver(
    storageLocation: Option[String] = Option(
      JFile.createTempFile("plume-", ".odb").getAbsolutePath
    ),
    heapPercentageThreshold: Int = 80,
    serializationStatsEnabled: Boolean = false,
    dataFlowCacheFile: Path = Paths.get("dataFlowCache.bin")
) extends IDriver {

  private val logger = LoggerFactory.getLogger(classOf[OverflowDbDriver])

  private val odbConfig = Config
    .withDefaults()
    .withHeapPercentageThreshold(heapPercentageThreshold)
  storageLocation match {
    case Some(path) => odbConfig.withStorageLocation(path)
    case None       => odbConfig.disableOverflow()
  }
  if (serializationStatsEnabled) odbConfig.withSerializationStatsEnabled()

  /** A direct pointer to the code property graph object.
    */
  val cpg: Cpg = newOverflowGraph(odbConfig)

  private val semanticsParser = new Parser()
  private val defaultSemantics: Try[BufferedSource] = Try(
    Source.fromInputStream(getClass.getClassLoader.getResourceAsStream("default.semantics"))
  )

  /** This stores results from the table property in ReachableByResult.
    */
  private val table: ConcurrentHashMap[Long, Vector[SerialReachableByResult]] =
    if (Files.isRegularFile(dataFlowCacheFile))
      Using.resource(new ObjectInputStream(new FileInputStream(dataFlowCacheFile.toFile))) { ois =>
        ois.readObject.asInstanceOf[ConcurrentHashMap[Long, Vector[SerialReachableByResult]]]
      }
    else
      new ConcurrentHashMap[Long, Vector[SerialReachableByResult]]()

  private implicit var context: EngineContext =
    EngineContext(
      Semantics.fromList(List()),
      EngineConfig(initialTable = deserializeResultTable(table, cpg))
    )

  /** Save dataflow paths on shutdown
    */
  Runtime.getRuntime.addShutdownHook(new Thread() {
    override def run(): Unit = {
      Using.resource(new ObjectOutputStream(new FileOutputStream(dataFlowCacheFile.toFile))) {
        oos =>
          oos.writeObject(table)
      }
    }
  })

  /** Sets the context for the data-flow engine when performing [[nodesReachableBy()]] queries.
    *
    * @param maxCallDepth the new method call depth.
    * @param methodSemantics the file containing method semantics for external methods.
    * @param initialCache an initializer for the data-flow cache containing pre-calculated paths.
    */
  def setDataflowContext(
      maxCallDepth: Int,
      methodSemantics: Option[BufferedSource] = None,
      initialCache: Option[ResultTable] = None
  ): Unit = {
    context = if (methodSemantics.isDefined) {
      EngineContext(
        Semantics.fromList(semanticsParser.parse(methodSemantics.get.getLines().mkString("\n"))),
        EngineConfig(maxCallDepth, initialCache)
      )
    } else if (defaultSemantics.isSuccess) {
      logger.info(
        "No specified method semantics file given. Using default semantics."
      )
      EngineContext(
        Semantics.fromList(semanticsParser.parse(defaultSemantics.get.getLines().mkString("\n"))),
        EngineConfig(maxCallDepth, initialCache)
      )
    } else {
      logger.warn(
        "No \"default.semantics\" file found under resources - data flow tracking may not perform correctly."
      )
      EngineContext(
        Semantics.fromList(List()),
        EngineConfig(maxCallDepth, initialCache)
      )
    }
  }

  override def isConnected: Boolean = !cpg.graph.isClosed

  override def close(): Unit = {
    Try(cpg.close()) match {
      case Success(_) => // nothing
      case Failure(e) =>
        logger.warn("Exception thrown while attempting to close graph.", e)
    }
  }

  override def clear(): Unit = cpg.graph.nodes.asScala.foreach(_.remove())

  override def exists(nodeId: Long): Boolean = cpg.graph.node(nodeId) != null

  override def exists(srcId: Long, dstId: Long, edge: String): Boolean =
    cpg.graph.node(srcId).out(edge).asScala.exists { dst => dst.id() == dstId }

  override def bulkTx(dg: AppliedDiffGraph): Unit = {
    // Do node operations first
    dg.diffGraph.iterator.foreach {
      case Change.RemoveNode(nodeId) =>
        cpg.graph.node(nodeId).remove()
      case Change.RemoveNodeProperty(nodeId, propertyKey) =>
        cpg.graph.nodes(nodeId).next().removeProperty(propertyKey)
      case Change.CreateNode(node) =>
        val newNode = cpg.graph.addNode(dg.nodeToGraphId(node), node.label)
        node.properties.foreach { case (k, v) => newNode.setProperty(k, v) }
      case Change.SetNodeProperty(node, key, value) =>
        cpg.graph.nodes(node.id()).next().setProperty(key, value)
      case _ => // do nothing
    }
    // Now that all nodes are in, connect/remove edges
    dg.diffGraph.iterator.foreach {
      case Change.RemoveEdge(edge) =>
        cpg.graph
          .nodes(edge.outNode().id())
          .next()
          .outE(edge.label())
          .forEachRemaining(e => if (e.inNode().id() == edge.inNode().id()) e.remove())
      case Change.CreateEdge(src, dst, label, packedProperties) =>
        val srcId: Long = id(src, dg).asInstanceOf[Long]
        val dstId: Long = id(dst, dg).asInstanceOf[Long]
        val e: overflowdb.Edge =
          cpg.graph.nodes(srcId).next().addEdge(label, cpg.graph.nodes(dstId).next())
        PackedProperties.unpack(packedProperties).foreach { case (k: String, v: Any) =>
          e.setProperty(k, v)
        }
      case _ => // do nothing
    }
  }

  private def dfsDelete(
      n: Node,
      visitedNodes: mutable.Set[Node],
      edgeToFollow: String*
  ): Unit = {
    if (!visitedNodes.contains(n)) {
      visitedNodes.add(n)
      n.out(edgeToFollow: _*).forEachRemaining(dfsDelete(_, visitedNodes, edgeToFollow: _*))
    }
    n.remove()
  }

  override def removeSourceFiles(filenames: String*): Unit = {
    val fs = filenames.toSet
    cpg.graph
      .nodes(NodeTypes.FILE)
      .filter { f =>
        fs.contains(f.property(PropertyNames.NAME).toString)
      }
      .foreach { f =>
        val fileChildren    = f.in(EdgeTypes.SOURCE_FILE).asScala.toList
        val typeDecls       = fileChildren.collect { case x: TypeDecl => x }
        val namespaceBlocks = fileChildren.collect { case x: NamespaceBlock => x }
        // Remove TYPE nodes
        typeDecls.flatMap(_.in(EdgeTypes.REF)).foreach(_.remove())
        // Remove NAMESPACE_BLOCKs and their AST children (TYPE_DECL, METHOD, etc.)
        val visitedNodes = mutable.Set.empty[Node]
        namespaceBlocks.foreach(dfsDelete(_, visitedNodes, EdgeTypes.AST, EdgeTypes.CONDITION))
        // Finally remove FILE node
        f.remove()
      }
  }

  override def propertyFromNodes(nodeType: String, keys: String*): List[Map[String, Any]] =
    cpg.graph
      .nodes(nodeType)
      .asScala
      .map { n =>
        keys.map { k =>
          k -> n.propertiesMap().getOrDefault(k, null)
        }.toMap + ("id" -> n.id())
      }
      .toList

  override def idInterval(lower: Long, upper: Long): Set[Long] = cpg.graph.nodes.asScala
    .filter(n => n.id() >= lower - 1 && n.id() <= upper)
    .map(_.id())
    .toSet

  override def linkAstNodes(
      srcLabels: List[String],
      edgeType: String,
      dstNodeMap: mutable.Map[String, Any],
      dstFullNameKey: String,
      dstNodeType: String
  ): Unit = {
    Traversal(cpg.graph.nodes(srcLabels: _*)).foreach { srcNode =>
      srcNode
        .propertyOption(dstFullNameKey)
        .filter {
          case Seq(_*) => true
          case dstFullName =>
            srcNode.propertyDefaultValue(dstFullNameKey) != null &&
              !srcNode.propertyDefaultValue(dstFullNameKey).equals(dstFullName)
        }
        .ifPresent { x =>
          (x match {
            case dstFullName: String  => Seq(dstFullName)
            case dstFullNames: Seq[_] => dstFullNames
            case _                    => Seq()
          }).collect { case x: String => x }
            .filter(dstNodeMap.contains)
            .flatMap { dstFullName: String =>
              dstNodeMap(dstFullName) match {
                case x: Long => Some(x)
                case _       => None
              }
            }
            .foreach { dstNodeId: Long =>
              srcNode match {
                case src: StoredNode =>
                  val dst = cpg.graph.nodes(dstNodeId).next()
                  if (!src.out(edgeType).asScala.contains(dst))
                    src.addEdge(edgeType, dst)
              }
            }
        }
    }
  }

  override def staticCallLinker(): Unit = {
    cpg.graph
      .nodes(NodeTypes.CALL)
      .collect { case x: Call if x.dispatchType == DispatchTypes.STATIC_DISPATCH => x }
      .foreach { c: Call =>
        methodFullNameToNode.get(c.methodFullName) match {
          case Some(dstId) if cpg.graph.nodes(dstId.asInstanceOf[Long]).hasNext =>
            c.addEdge(EdgeTypes.CALL, cpg.graph.nodes(dstId.asInstanceOf[Long]).next())
          case _ =>
        }
      }
  }

  override def dynamicCallLinker(): Unit =
    new PlumeDynamicCallLinker(CPG(cpg.graph)).createAndApply()

  def nodesReachableBy(source: Traversal[CfgNode], sink: Traversal[CfgNode]): List[CfgNode] = {
    val results: List[ReachableByResult] = sink.reachableByDetailed(source)
//    captureResultsGraph(results)
    captureResultsBlob(results)
    results.map(_.path.head.node).distinct
  }

  /** Primitive storage solution for saving path results. This will capture results in memory until shutdown after
    * which everything will be saved to a blob.
    * @param results a list of ReachableByResult paths calculated when calling reachableBy.
    */
  private def captureResultsBlob(results: List[ReachableByResult]): Unit = {
    results
      .map { x => x.table }
      .foreach { t: ResultTable =>
        t.keys.foreach { n: StoredNode =>
          t.get(n) match {
            case Some(v) =>
              table.put(n.id(), v.map { rbr => SerialReachableByResult.apply(rbr, table) })
            case None =>
          }
        }
      }
  }

  /** TODO: Figure out how to store this in a way that will deserialize correctly.
    * @param results a list of ReachableByResult paths calculated when calling reachableBy.
    */
  private def captureResultsGraph(results: List[ReachableByResult]): Unit = {
    val CALL_SITE = "CALL_SITE"
    // Save these paths to the CPG
    results
      .filter { x => x.path.size > 1 }
      .map { x => (x, x.path.map { x => cpg.graph.node(x.node.id()) }) }
      .foreach { case (result: ReachableByResult, resolvedPath: Seq[Node]) =>
        resolvedPath.reduceLeft { (l: Node, r: Node) =>
          val dfe: Edge =
            if (l.out(EdgeTypes.DATA_FLOW).hasNext)
              l.outE(EdgeTypes.DATA_FLOW).filter(_.inNode() == r).next()
            else
              l.addEdge(EdgeTypes.DATA_FLOW, r)
          val cs: Seq[Call] = (dfe.property(CALL_SITE, Seq()) :+ result.callSite).flatten
          dfe.setProperty(CALL_SITE, cs)
          r
        }
      }
  }

}

object OverflowDbDriver {
  def newOverflowGraph(odbConfig: Config = Config.withDefaults()): Cpg = Cpg.withConfig(odbConfig)
}

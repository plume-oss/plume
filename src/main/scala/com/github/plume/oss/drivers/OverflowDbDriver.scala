package com.github.plume.oss.drivers

import com.github.plume.oss.PlumeStatistics
import com.github.plume.oss.domain.{
  SerialReachableByResult,
  compressToFile,
  decompressFile,
  deserializeResultTable
}
import com.github.plume.oss.drivers.OverflowDbDriver.newOverflowGraph
import com.github.plume.oss.passes.callgraph.PlumeDynamicCallLinker
import com.github.plume.oss.util.BatchedUpdateUtil._
import io.joern.dataflowengineoss.language.toExtendedCfgNode
import io.joern.dataflowengineoss.queryengine._
import io.joern.dataflowengineoss.semanticsloader.{Parser, Semantics}
import io.shiftleft.codepropertygraph.generated._
import io.shiftleft.codepropertygraph.generated.nodes._
import io.shiftleft.codepropertygraph.{Cpg => CPG}
import io.shiftleft.passes.AppliedDiffGraph
import io.shiftleft.passes.DiffGraph.{Change, PackedProperties}
import org.apache.commons.lang.StringEscapeUtils
import org.slf4j.LoggerFactory
import overflowdb.BatchedUpdate.AppliedDiff
import overflowdb.traversal.{Traversal, jIteratortoTraversal}
import overflowdb.{BatchedUpdate, Config, DetachedNodeData, Edge, Node}

import java.io.{FileOutputStream, OutputStreamWriter, File => JFile}
import java.nio.file.{Files, Path, Paths}
import java.util.concurrent.ConcurrentHashMap
import scala.collection.mutable
import scala.io.{BufferedSource, Source}
import scala.jdk.CollectionConverters.{ConcurrentMapHasAsScala, IteratorHasAsScala, MapHasAsScala}
import scala.util.{Failure, Success, Try, Using}

/** Driver to create an OverflowDB database file.
  * @param storageLocation where the database will serialize to and deserialize from.
  * @param heapPercentageThreshold the percentage of the JVM heap from when the database will begin swapping to disk.
  * @param serializationStatsEnabled enables saving of serialization statistics.
  * @param dataFlowCacheFile the path to the cache file where data-flow paths are saved to. If None then data flow
  *                          results will not be saved.
  */
final case class OverflowDbDriver(
    storageLocation: Option[String] = Option(
      JFile.createTempFile("plume-", ".odb").getAbsolutePath
    ),
    heapPercentageThreshold: Int = 80,
    serializationStatsEnabled: Boolean = false,
    dataFlowCacheFile: Option[Path] = Some(Paths.get("dataFlowCache.json.gzip"))
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
  val cpg: Cpg =
    PlumeStatistics.time(PlumeStatistics.TIME_OPEN_DRIVER, { newOverflowGraph(odbConfig) })

  private val semanticsParser = new Parser()
  private val defaultSemantics: Try[BufferedSource] = Try(
    Source.fromInputStream(getClass.getClassLoader.getResourceAsStream("default.semantics"))
  )

  /** This stores results from the table property in ReachableByResult.
    */
  private val table: Option[ConcurrentHashMap[Long, Vector[SerialReachableByResult]]] =
    dataFlowCacheFile match {
      case Some(filePath) =>
        if (Files.isRegularFile(filePath))
          Some(decompressFile[ConcurrentHashMap[Long, Vector[SerialReachableByResult]]](filePath))
        else
          Some(new ConcurrentHashMap[Long, Vector[SerialReachableByResult]]())
      case None => None
    }

  private implicit var context: EngineContext =
    EngineContext(
      Semantics.fromList(List()),
      EngineConfig(initialTable = deserializeResultTable(table, cpg))
    )

  private def saveDataflowCache(): Unit = dataFlowCacheFile match {
    case Some(filePath) => compressToFile(table, filePath)
    case None           => // Do nothing
  }

  /** Sets the context for the data-flow engine when performing [[nodesReachableBy]] queries.
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
    val cache = if (initialCache.isDefined) initialCache else deserializeResultTable(table, cpg)

    context = if (methodSemantics.isDefined) {
      EngineContext(
        Semantics.fromList(semanticsParser.parse(methodSemantics.get.getLines().mkString("\n"))),
        EngineConfig(maxCallDepth, cache)
      )
    } else if (defaultSemantics.isSuccess) {
      logger.info(
        "No specified method semantics file given. Using default semantics."
      )
      EngineContext(
        Semantics.fromList(semanticsParser.parse(defaultSemantics.get.getLines().mkString("\n"))),
        EngineConfig(maxCallDepth, cache)
      )
    } else {
      logger.warn(
        "No \"default.semantics\" file found under resources - data flow tracking may not perform correctly."
      )
      EngineContext(
        Semantics.fromList(List()),
        EngineConfig(maxCallDepth, cache)
      )
    }
  }

  override def isConnected: Boolean = !cpg.graph.isClosed

  override def close(): Unit = PlumeStatistics.time(
    PlumeStatistics.TIME_CLOSE_DRIVER, {
      Try(cpg.close()) match {
        case Success(_) => saveDataflowCache()
        case Failure(e) =>
          logger.warn("Exception thrown while attempting to close graph.", e)
      }
    }
  )

  override def clear(): Unit = {
    cpg.graph.nodes.asScala.foreach(safeRemove)
    dataFlowCacheFile match {
      case Some(filePath) => filePath.toFile.delete()
      case None           => // Do nothing
    }
  }

  override def exists(nodeId: Long): Boolean = cpg.graph.node(nodeId) != null

  override def exists(srcId: Long, dstId: Long, edge: String): Boolean =
    cpg.graph.node(srcId).out(edge).asScala.exists { dst => dst.id() == dstId }

  override def bulkTx(dg: AppliedDiffGraph): Unit = {
    // Do node operations first
    dg.diffGraph.iterator.foreach {
      case Change.RemoveNode(nodeId) =>
        safeRemove(cpg.graph.node(nodeId))
      case Change.RemoveNodeProperty(nodeId, propertyKey) =>
        cpg.graph.node(nodeId).removeProperty(propertyKey)
      case Change.CreateNode(node) =>
        val newNode = cpg.graph.addNode(dg.nodeToGraphId(node), node.label)
        node.properties.foreach { case (k, v) => newNode.setProperty(k, v) }
      case Change.SetNodeProperty(node, key, value) =>
        cpg.graph.node(node.id()).setProperty(key, value)
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
          cpg.graph.node(srcId).addEdge(label, cpg.graph.node(dstId))
        PackedProperties.unpack(packedProperties).foreach { case (k: String, v: Any) =>
          e.setProperty(k, v)
        }
      case _ => // do nothing
    }
  }

  override def bulkTx(dg: AppliedDiff): Unit = {
    dg.getDiffGraph.iterator.collect { case x: DetachedNodeData => x }.foreach { node =>
      val id      = idFromNodeData(node)
      val newNode = cpg.graph.addNode(id, node.label)
      propertiesFromNodeData(node).foreach { case (k, v) => newNode.setProperty(k, v) }
    }
    dg.getDiffGraph.iterator.filterNot(_.isInstanceOf[DetachedNodeData]).foreach {
      case c: BatchedUpdate.CreateEdge =>
        val srcId = idFromNodeData(c.src)
        val dstId = idFromNodeData(c.dst)
        val e     = cpg.graph.node(srcId).addEdge(c.label, cpg.graph.node(dstId))
        Option(c.propertiesAndKeys) match {
          case Some(edgeKeyValues) =>
            propertiesFromObjectArray(edgeKeyValues).foreach { case (k, v) => e.setProperty(k, v) }
          case None =>
        }
      case c: BatchedUpdate.RemoveNode => safeRemove(cpg.graph.node(c.node.id()))
      case c: BatchedUpdate.SetNodeProperty =>
        cpg.graph.node(c.node.id()).setProperty(c.label, c.value)
    }
  }

  private def accumNodesToDelete(
      n: Node,
      visitedNodes: mutable.Set[Node],
      edgeToFollow: String*
  ): Unit = {
    if (!visitedNodes.contains(n)) {
      visitedNodes.add(n)
      n.out(edgeToFollow: _*)
        .forEachRemaining(accumNodesToDelete(_, visitedNodes, edgeToFollow: _*))
    }
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
        typeDecls.flatMap(_.in(EdgeTypes.REF)).foreach(safeRemove)
        // Remove NAMESPACE_BLOCKs and their AST children (TYPE_DECL, METHOD, etc.)
        val nodesToDelete = mutable.Set.empty[Node]
        namespaceBlocks.foreach(
          accumNodesToDelete(_, nodesToDelete, EdgeTypes.AST, EdgeTypes.CONDITION)
        )
        nodesToDelete.foreach(safeRemove)
        // Finally remove FILE node
        safeRemove(f)
      }
  }

  private def safeRemove(n: Node): Unit = Try(if (n != null) {
    n.inE().forEachRemaining(_.remove())
    n.outE().forEachRemaining(_.remove())
    n.remove()
  }) match {
    case Failure(e) if cpg.graph.node(n.id()) != null =>
      logger.warn(
        s"Unable to delete node due to error '${e.getMessage}': [${n.id()}] ${n.label()}"
      )
    case Failure(e) =>
      logger.warn(
        s"Exception '${e.getMessage}' occurred while attempting to delete node: [${n.id()}] ${n.label()}"
      )
    case _ =>
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
            c.addEdge(EdgeTypes.CALL, cpg.graph.node(dstId.asInstanceOf[Long]))
          case _ =>
        }
      }
  }

  override def dynamicCallLinker(): Unit =
    new PlumeDynamicCallLinker(CPG(cpg.graph)).createAndApply()

  /** Lists all of the source nodes that reach the given sinks. These results are cached between analysis runs.
    *
    * @param source the source query to match.
    * @param sink the sink query to match.
    * @param sanitizers a set of full method names to filter paths out with.
    * @return the source nodes whose data flows to the given sinks uninterrupted.
    */
  def nodesReachableBy(
      source: Traversal[CfgNode],
      sink: Traversal[CfgNode],
      sanitizers: Set[String] = Set.empty[String]
  ): List[ReachableByResult] =
    PlumeStatistics.time(
      PlumeStatistics.TIME_REACHABLE_BY_QUERYING, {
        val results: List[ReachableByResult] = sink.reachableByDetailed(source)
        // TODO: Right now the results are saved in a serializable format ready for a binary blob. We should look into
        // storing these reliably on the graph.
        captureResultsBlob(results)
        results
          // Filter out paths that run through sanitizers
          .filterNot { r =>
            r.path
              .flatMap(n => Vector(n.node) ++ n.node._astIn)
              .collect { case x: Call => x }
              .exists { x =>
                sanitizers.contains(x.methodFullName)
              }
          }
      }
    )

  /** Primitive storage solution for saving path results. This will capture results in memory until shutdown after
    * which everything will be saved to a blob.
    * @param results a list of ReachableByResult paths calculated when calling reachableBy.
    */
  private def captureResultsBlob(results: List[ReachableByResult]): Unit = {
    table match {
      case Some(tab) =>
        results
          .map { x => x.table }
          .foreach { t: ResultTable =>
            t.keys().foreach { n: StoredNode =>
              t.get(n) match {
                case Some(v) =>
                  tab.put(n.id(), v.map(SerialReachableByResult.apply))
                case None =>
              }
            }
          }
      case None => // Do nothing
    }
  }

  /** Will traverse the deserialized cache and remove nodes belonging to types not in the given set of unchanged types.
    * @param unchangedTypes the list of types which have not changed since the last graph database update.
    */
  def removeExpiredPathsFromCache(unchangedTypes: Set[String]): Unit = {
    table match {
      case Some(tab) =>
        tab.asScala
          .filter { case (k: Long, _) => isNodeUnderTypes(k, unchangedTypes) }
          .foreach { case (k: Long, v: Vector[SerialReachableByResult]) =>
            val filteredPaths = v.filterNot { srbr =>
              // Filter out paths where there exists a path element that is not under unchanged types
              srbr.path.exists { spe => !isNodeUnderTypes(spe.nodeId, unchangedTypes) }
            }
            tab.put(k, filteredPaths)
          }
      case None => // Do nothing
    }
  }

  /** Will determine if the node ID is under the set of unchanged type full names based on method full name signature.
    * @param nodeId the node ID to check.
    * @param unchangedTypes a list of unchanged types.
    * @return True if the ID is under a method contained by the list of unchanged types and false if otherwise. If the
    *         given node ID is not in the database false will be returned.
    */
  private def isNodeUnderTypes(nodeId: Long, unchangedTypes: Set[String]): Boolean = {
    cpg.graph.nodes(nodeId).nextOption() match {
      case Some(n) if n != null =>
        n.in(EdgeTypes.CONTAINS, EdgeTypes.AST).collect { case x: Method => x }.nextOption() match {
          case Some(m) if m != null =>
            val typeFullName = m.fullName.substring(0, m.fullName.lastIndexOf('.'))
            unchangedTypes.contains(typeFullName)
          case _ => false
        }
      case _ => false
    }
  }

  /** Serializes the graph in the OverflowDB instance to the
    * [[http://graphml.graphdrawing.org/specification/dtd.html GraphML]]
    * format to the given OutputStreamWriter. This format is supported by
    * [[https://tinkerpop.apache.org/docs/current/reference/#graphml TinkerGraph]] and
    * [[http://manual.cytoscape.org/en/stable/Supported_Network_File_Formats.html#graphml Cytoscape]].
    * @param exportPath the path to write the GraphML representation of the graph to.
    */
  def exportAsGraphML(exportPath: java.io.File): Unit = {
    val g = cpg.graph
    Using.resource(new OutputStreamWriter(new FileOutputStream(exportPath))) { osw =>
      // Write header
      osw.write("<?xml version=\"1.0\" ?>")
      osw.write("<graphml ")
      osw.write("xmlns=\"http://graphml.graphdrawing.org/xmlns\" ")
      osw.write("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ")
      osw.write(
        "xsi:schemaLocation=\"http://graphml.graphdrawing.org/xmlns http://graphml.graphdrawing.org/xmlns/1.1/graphml.xsd\">"
      )
      // Write keys
      osw.write("<key id=\"labelV\" for=\"node\" attr.name=\"labelV\" attr.type=\"string\"></key>")
      osw.write("<key id=\"labelE\" for=\"edge\" attr.name=\"labelE\" attr.type=\"string\"></key>")
      g.nodes()
        .flatMap(_.propertiesMap().asScala)
        .map { case (k: String, v: Object) =>
          k -> (v match {
            case _: java.lang.Integer => "int"
            case _: java.lang.Boolean => "boolean"
            case _                    => "string"
          })
        }
        .foreach { case (k: String, t: String) =>
          osw.write("<key ")
          osw.write("id=\"" + k + "\" ")
          osw.write("for=\"node\" ")
          osw.write("attr.name=\"" + k + "\" ")
          osw.write("attr.type=\"" + t + "\">")
          osw.write("</key>")
        }
      // Write graph
      osw.write("<graph id=\"G\" edgedefault=\"directed\">")
      // Write vertices
      g.nodes().foreach { (n: Node) =>
        osw.write("<node id=\"" + n.id + "\">")
        osw.write("<data key=\"labelV\">" + n.label() + "</data>")
        serializeLists(n.propertiesMap().asScala.toMap).foreach { case (k, v) =>
          osw.write(
            "<data key=\"" + k + "\">" + StringEscapeUtils.escapeXml(v.toString) + "</data>"
          )
        }
        osw.write("</node>")
      }
      // Write edges
      g.edges().zipWithIndex.foreach { case (e: Edge, i: Int) =>
        osw.write("<edge id=\"" + i + "\" ")
        osw.write("source=\"" + e.outNode().id() + "\" ")
        osw.write("target=\"" + e.inNode().id() + "\">")
        osw.write("<data key=\"labelE\">" + e.label() + "</data>")
        osw.write("</edge>")
      }
      // Close graph tags
      osw.write("</graph>")
      osw.write("</graphml>")
    }
  }

}

object OverflowDbDriver {
  def newOverflowGraph(odbConfig: Config = Config.withDefaults()): Cpg = Cpg.withConfig(odbConfig)
}

package com.github.plume.oss.drivers

import com.github.plume.oss.drivers.ISchemaSafeDriver
import io.shiftleft.codepropertygraph.generated.NodeTypes
import overflowdb.BatchedUpdate

class SouffleDriver extends ISchemaSafeDriver {

  override def buildSchema(): Unit = ???

  override def buildSchemaPayload(): String =
    """
      |.type var <: symbol
      |.type obj <: symbol
      |.type field <: symbol
      |""".stripMargin

  private def VERTICES: String = {
    NodeTypes.ALL
  }

  override def bulkTx(dg: BatchedUpdate.DiffOrBuilder): Int = ???

  override def clear(): Unit = ???

  override def exists(nodeId: Long): Boolean = ???

  override def exists(srcId: Long, dstId: Long, edge: String): Boolean = ???

  override def propertyFromNodes(nodeType: String, keys: String*): List[Map[String, Any]] = ???

  override def isConnected: Boolean = ???

  override def removeSourceFiles(filenames: String*): Unit = ???

  override def close(): Unit = ???
}

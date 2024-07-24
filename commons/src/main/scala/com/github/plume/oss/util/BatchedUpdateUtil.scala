package com.github.plume.oss.util

import io.shiftleft.codepropertygraph.generated.nodes.NewNode
import flatgraph.{DNodeOrNode, GNode, DNode}

import scala.collection.mutable

/** Tools to extract information from new BatchedUpdate API.
  */
object BatchedUpdateUtil {

  private def idFromRefOrId(refOrId: Object): Long = {
    refOrId match {
      case n: GNode          => n.id()
      case i: java.lang.Long => i.longValue()
    }
  }

  /** By determines what kind of node object is given, will extract its label.
    * @param data
    *   either detached node data or node object.
    * @return
    *   the node ID.
    */
  def labelFromNodeData(data: Any): String =
    data match {
      case generic: DNode => "<boop>"
      case node: NewNode  => node.label
      case node: GNode    => node.label
    }

  /** By determines what kind of node object is given, will extract its ID.
    * @param data
    *   either detached node data or node object.
    * @return
    *   the node ID.
    */
  def idFromNodeData(data: Any): Long =
    data match {
      case generic: DNode => generic.storedRef.map(idFromRefOrId).getOrElse(-1L)
      case node: GNode    => node.id()
    }

  /** Extracts properties from detached node data.
    * @param data
    *   node data from which to determine properties from.
    * @return
    *   a map of key-value pairs.
    */
  def propertiesFromNodeData(data: DNodeOrNode): Seq[(String, AnyRef)] = {
    data match {
//      case generic: DNodeOrNode => propertiesFromObjectArray(generic)
      case node: NewNode => node.properties.collect { case (k: String, v: AnyRef) => k -> v }.toSeq
      case _ =>
        println("ahh")
        Seq.empty[(String, AnyRef)]
    }
  }

  /** Extracts a property key-value pairs as a map from an object array.
    * @param arr
    *   the object array where key-values are stored as pairs.
    * @return
    *   a map of key-value pairs.
    */
  def propertiesFromObjectArray(arr: Array[Object]): Seq[(String, AnyRef)] = {
    val props = mutable.ListBuffer.empty[(String, AnyRef)]
    for {
      i <- arr.indices by 2
    } {
      val key   = arr(i).asInstanceOf[String]
      val value = arr(i + 1)
      props.addOne(key -> value)
    }
    props.toSeq
  }

}

package com.github.plume.oss.util

import io.shiftleft.codepropertygraph.generated.nodes.NewNode
import overflowdb.{DetachedNodeData, DetachedNodeGeneric, Node}

import scala.collection.mutable

/**
  * Tools to extract information from new BatchedUpdate API.
  */
object BatchedUpdateUtil {

  private def idFromRefOrId(refOrId: Object): Long = {
    refOrId match {
      case n: Node           => n.id()
      case i: java.lang.Long => i.longValue()
    }
  }

  /** By determines what kind of node object is given, will extract its label.
    * @param data either detached node data or node object.
    * @return the node ID.
    */
  def labelFromNodeData(data: Any): String =
    data match {
      case generic: DetachedNodeGeneric => generic.label()
      case node: NewNode                => node.label()
      case node: Node                   => node.label()
    }

  /** By determines what kind of node object is given, will extract its ID.
    * @param data either detached node data or node object.
    * @return the node ID.
    */
  def idFromNodeData(data: Any): Long =
    data match {
      case generic: DetachedNodeGeneric => idFromRefOrId(generic.getRefOrId)
      case node: NewNode                => idFromRefOrId(node.getRefOrId)
      case node: Node                   => node.id()
    }

  /** Extracts properties from detached node data.
    * @param data node data from which to determine properties from.
    * @return a map of key-value pairs.
    */
  def propertiesFromNodeData(data: DetachedNodeData): Map[String, Any] = {
    data match {
      case generic: DetachedNodeGeneric => propertiesFromObjectArray(generic.keyvalues)
      case node: NewNode                => node.properties
      case _                            => Map.empty[String, Any]
    }
  }

  /** Extracts a property key-value pairs as a map from an object array.
    * @param arr the object array where key-values are stored as pairs.
    * @return a map of key-value pairs.
    */
  def propertiesFromObjectArray(arr: Array[Object]): Map[String, Any] = {
    val props = mutable.HashMap.empty[String, Object]
    for {
      i <- arr.indices by 2
    } {
      val key   = arr(i).asInstanceOf[String]
      val value = arr(i + 1)
      props.put(key, value)
    }
    props.toMap
  }


}

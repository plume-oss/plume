package za.ac.sun.plume

import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.codepropertygraph.generated.nodes.HasOrder
import io.shiftleft.semanticcpg.language._
import overflowdb.Graph

object Traversals {

  def maxOrder(graph : Graph) : Integer = {
    Cpg(graph).all.collect{ case x : HasOrder => x.order }.maxOption.getOrElse(0)
  }

}

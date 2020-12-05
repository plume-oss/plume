package za.ac.sun.plume

import io.shiftleft.codepropertygraph.generated.nodes
import io.shiftleft.codepropertygraph.generated.nodes.NewMethod

object CpgDomainObjCreator {

  def method(fullName : String): NewMethod =
    nodes.NewMethod(fullName = fullName)

}

package za.ac.sun.plume

import io.shiftleft.codepropertygraph.generated.nodes

object CpgDomainObjCreator {

  def metaData(language : String, version : String): nodes.NewMetaData =
    nodes.NewMetaData(language = language, version = version)

  def arrayInitializer(order : Int): nodes.NewArrayInitializer = {
    nodes.NewArrayInitializer(order = order)

  }

  def binding(name : String, signature : String): nodes.NewBinding =
    nodes.NewBinding(name = name, signature = signature)

  def method(code : String, name: String, fullName : String, signature : String, order : Int): nodes.NewMethod =
    nodes.NewMethod(
      code = code,
      name = name,
      fullName = fullName,
      signature = signature,
      order = order
    )

}

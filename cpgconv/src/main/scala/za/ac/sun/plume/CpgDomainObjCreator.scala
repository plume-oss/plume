package za.ac.sun.plume

import io.shiftleft.codepropertygraph.generated.nodes

object CpgDomainObjCreator {


  def arrayInitializer(order : Int): nodes.NewArrayInitializer =
    nodes.NewArrayInitializer(order = order)

  def binding(name : String, signature : String): nodes.NewBinding =
    nodes.NewBinding(name = name, signature = signature)

  def controlStructure(code : String, columnNumber : Int, lineNumber : Int, order : Int) : nodes.NewControlStructure =
    nodes.NewControlStructure(code = code, columnNumber = Some(columnNumber), lineNumber = Some(lineNumber), order = order)

  def file(name : String, order : Int) : nodes.NewFile =
    nodes.NewFile(name = name, order = order)

  def jumpTarget(code : String, name : String, columnNumber : Int, lineNumber : Int, order : Int) : nodes.NewJumpTarget =
    nodes.NewJumpTarget(code = code, name = name, columnNumber = Some(columnNumber), lineNumber = Some(lineNumber), order = order)

  def local(code : String, name : String, columnNumber : Int, lineNumber : Int, order : Int, typeFullName : String) : nodes.NewLocal =
    nodes.NewLocal(code = code, name = name, columnNumber = Some(columnNumber), lineNumber = Some(lineNumber), order = order, typeFullName = typeFullName)

  def metaData(language : String, version : String): nodes.NewMetaData =
    nodes.NewMetaData(language = language, version = version)

  def method(code : String, name: String, fullName : String, signature : String, order : Int): nodes.NewMethod =
    nodes.NewMethod(
      code = code,
      name = name,
      fullName = fullName,
      signature = signature,
      order = order
    )

}

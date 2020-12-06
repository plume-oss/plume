package za.ac.sun.plume

import io.shiftleft.codepropertygraph.generated.nodes
import io.shiftleft.codepropertygraph.generated.nodes.HasDynamicTypeHintFullName

object CpgDomainObjCreator {


  def arrayInitializer(order : Int): nodes.NewArrayInitializer =
    nodes.NewArrayInitializer(order = order)

  def binding(name : String, signature : String): nodes.NewBinding =
    nodes.NewBinding(name = name, signature = signature)

  def block(code : String, columnNumber : Int, lineNumber : Int, order : Int, typeFullName : String, argumentIndex : Int) : nodes.NewBlock =
    nodes.NewBlock(code = code, columnNumber = Some(columnNumber), lineNumber = Some(lineNumber), order = order, typeFullName = typeFullName, argumentIndex = argumentIndex)

  def call(code : String, name : String, columnNumber : Int, lineNumber : Int, order : Int, methodFullName : String, argumentIndex : Int, signature : String, dispatchType : String,
           dynamicTypeHintFullName: String) : nodes.NewCall =
    nodes.NewCall(code = code, name = name, columnNumber = Some(columnNumber), lineNumber = Some(lineNumber), order = order,
      methodFullName = methodFullName, argumentIndex = argumentIndex, signature = signature, dispatchType = dispatchType, dynamicTypeHintFullName = List(dynamicTypeHintFullName))

  def controlStructure(code : String, columnNumber : Int, lineNumber : Int, order : Int) : nodes.NewControlStructure =
    nodes.NewControlStructure(code = code, columnNumber = Some(columnNumber), lineNumber = Some(lineNumber), order = order)

  def file(name : String, order : Int) : nodes.NewFile =
    nodes.NewFile(name = name, order = order)

  def jumpTarget(code : String, name : String, columnNumber : Int, lineNumber : Int, order : Int) : nodes.NewJumpTarget =
    nodes.NewJumpTarget(code = code, name = name, columnNumber = Some(columnNumber), lineNumber = Some(lineNumber), order = order)

  def identifier(code : String, name : String, columnNumber : Int, lineNumber : Int, order : Int, typeFullName : String, argumentIndex : Int) : nodes.NewIdentifier = nodes.NewIdentifier(
    code = code, name = name, columnNumber = Some(columnNumber), lineNumber = Some(lineNumber), order = order, typeFullName = typeFullName, argumentIndex = argumentIndex
  )

  def literal(code : String, columnNumber : Int, lineNumber : Int, order : Int, typeFullName : String, argumentIndex : Int) : nodes.NewLiteral =
    nodes.NewLiteral(code = code, columnNumber = Some(columnNumber), lineNumber = Some(lineNumber), order = order, typeFullName = typeFullName, argumentIndex = argumentIndex)

  def local(code : String, name : String, columnNumber : Int, lineNumber : Int, order : Int, typeFullName : String) : nodes.NewLocal =
    nodes.NewLocal(code = code, name = name, columnNumber = Some(columnNumber), lineNumber = Some(lineNumber), order = order, typeFullName = typeFullName)

  def metaData(language : String, version : String): nodes.NewMetaData =
    nodes.NewMetaData(language = language, version = version)

  def method(code : String, name: String, fullName : String, signature : String, order : Int, columnNumber : Int, lineNumber : Int): nodes.NewMethod =
    nodes.NewMethod(
      code = code,
      name = name,
      fullName = fullName,
      signature = signature,
      order = order,
      columnNumber = Some(columnNumber),
      lineNumber = Some(lineNumber)
    )

  def methodParameter(code : String, name : String, lineNumber : Int, order : Int, evaluationStrategy : String, typeFullName : String) : nodes.NewMethodParameterIn =
    nodes.NewMethodParameterIn(code = code, name = name, lineNumber = Some(lineNumber), order = order, evaluationStrategy = evaluationStrategy, typeFullName = typeFullName)

  def methodReturn(code : String, columnNumber : Int, lineNumber : Int, order : Int, typeFullName : String, evaluationStrategy : String): nodes.NewMethodReturn =
    nodes.NewMethodReturn(code = code, columnNumber = Some(columnNumber), lineNumber = Some(lineNumber), order = order,
      typeFullName = typeFullName, evaluationStrategy = evaluationStrategy)

  def namespaceBlock(name : String, fullName : String, order : Int) : nodes.NewNamespaceBlock = nodes.NewNamespaceBlock(name = name, fullName = fullName, order = order)

  def returnNode(code : String, lineNumber : Int, order : Int, argumentIndex : Int) : nodes.NewReturn =
    nodes.NewReturn(code = code, lineNumber = Some(lineNumber), order = order, argumentIndex = argumentIndex)

  def typeArgument(order : Int) : nodes.NewTypeArgument =
    nodes.NewTypeArgument(order = order)

  def typeDecl(name : String, fullName : String, order : Int) : nodes.NewTypeDecl =
    nodes.NewTypeDecl(name = name, fullName = fullName, order = order)

  def typeParameter(name : String, order : Int) : nodes.NewTypeParameter =
    nodes.NewTypeParameter(name = name, order = order)

}

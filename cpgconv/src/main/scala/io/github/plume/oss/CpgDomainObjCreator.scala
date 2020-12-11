package io.github.plume.oss

import io.shiftleft.codepropertygraph.generated.nodes

object CpgDomainObjCreator {

  def arrayInitializer(order: Int): nodes.NewArrayInitializer =
    nodes.NewArrayInitializer(order = order)

  def binding(name: String, signature: String): nodes.NewBinding =
    nodes.NewBinding(name = name, signature = signature)

  def block(
    code: String,
    columnNumber: Int,
    lineNumber: Int,
    order: Int,
    typeFullName: String,
    argumentIndex: Int
  ): nodes.NewBlock =
    nodes.NewBlock(
      code = code,
      columnNumber = Some(columnNumber),
      lineNumber = Some(lineNumber),
      order = order,
      typeFullName = typeFullName,
      argumentIndex = argumentIndex
    )

  def call(
    code: String,
    name: String,
    columnNumber: Int,
    lineNumber: Int,
    order: Int,
    methodFullName: String,
    argumentIndex: Int,
    signature: String,
    dispatchType: String,
    dynamicTypeHintFullName: String,
    typeFullName: String
  ): nodes.NewCall =
    nodes.NewCall(
      code = code,
      name = name,
      columnNumber = Some(columnNumber),
      lineNumber = Some(lineNumber),
      order = order,
      methodFullName = methodFullName,
      argumentIndex = argumentIndex,
      signature = signature,
      dispatchType = dispatchType,
      dynamicTypeHintFullName = List(dynamicTypeHintFullName),
      typeFullName = typeFullName
    )

  def controlStructure(code: String, columnNumber: Int, lineNumber: Int, order: Int, argumentIndex: Int):
    nodes.NewControlStructure = nodes.NewControlStructure(
      code = code,
      columnNumber = Some(columnNumber),
      lineNumber = Some(lineNumber),
      order = order,
      argumentIndex = argumentIndex
    )

  def fieldIdentifier(
    canonicalName: String,
    code: String,
    argumentIndex: Int,
    lineNumber: Int,
    columnNumber: Int,
    order: Int
  ): nodes.NewFieldIdentifier = nodes.NewFieldIdentifier(
    canonicalName = canonicalName,
    code = code,
    argumentIndex = argumentIndex,
    lineNumber = Some(lineNumber),
    columnNumber = Some(columnNumber),
    order = order
  )

  def file(name: String, hash: String, order: Int): nodes.NewFile =
    nodes.NewFile(name = name, hash = Some(hash), order = order)

  def jumpTarget(code: String, name: String, columnNumber: Int, lineNumber: Int, order: Int, argumentIndex: Int):
    nodes.NewJumpTarget =
      nodes.NewJumpTarget(
        code = code,
        name = name,
        columnNumber = Some(columnNumber),
        lineNumber = Some(lineNumber),
        order = order,
        argumentIndex = argumentIndex
      )

  def identifier(
    code: String,
    name: String,
    columnNumber: Int,
    lineNumber: Int,
    order: Int,
    typeFullName: String,
    argumentIndex: Int
  ): nodes.NewIdentifier = nodes.NewIdentifier(
    code = code,
    name = name,
    columnNumber = Some(columnNumber),
    lineNumber = Some(lineNumber),
    order = order,
    typeFullName = typeFullName,
    argumentIndex = argumentIndex
  )

  def literal(
    code: String,
    columnNumber: Int,
    lineNumber: Int,
    order: Int,
    typeFullName: String,
    argumentIndex: Int
  ): nodes.NewLiteral =
    nodes.NewLiteral(
      code = code,
      columnNumber = Some(columnNumber),
      lineNumber = Some(lineNumber),
      order = order,
      typeFullName = typeFullName,
      argumentIndex = argumentIndex
    )

  def local(
    code: String,
    name: String,
    columnNumber: Int,
    lineNumber: Int,
    order: Int,
    typeFullName: String
  ): nodes.NewLocal =
    nodes.NewLocal(
      code = code,
      name = name,
      columnNumber = Some(columnNumber),
      lineNumber = Some(lineNumber),
      order = order,
      typeFullName = typeFullName
    )

  def member(code: String, name: String, typeFullName: String, order: Int): nodes.NewMember =
    nodes.NewMember(code = code, name = name, typeFullName = typeFullName, order = order)

  def metaData(language: String, version: String): nodes.NewMetaData =
    nodes.NewMetaData(language = language, version = version)

  def method(
    code: String,
    name: String,
    fullName: String,
    signature: String,
    order: Int,
    columnNumber: Int,
    lineNumber: Int
  ): nodes.NewMethod =
    nodes.NewMethod(
      code = code,
      name = name,
      fullName = fullName,
      signature = signature,
      order = order,
      columnNumber = Some(columnNumber),
      lineNumber = Some(lineNumber)
    )

  def methodParameterIn(
    code: String,
    name: String,
    lineNumber: Int,
    order: Int,
    evaluationStrategy: String,
    typeFullName: String
  ): nodes.NewMethodParameterIn =
    nodes.NewMethodParameterIn(
      code = code,
      name = name,
      lineNumber = Some(lineNumber),
      order = order,
      evaluationStrategy = evaluationStrategy,
      typeFullName = typeFullName
    )

  def methodRef(
    methodInstFullName: String,
    methodFullName: String,
    code: String,
    order: Int,
    argumentIndex: Int,
    lineNumber: Int,
    columnNumber: Int
  ): nodes.NewMethodRef =
    nodes.NewMethodRef(
      methodInstFullName = Some(methodInstFullName),
      methodFullName = methodFullName,
      code = code,
      order = order,
      argumentIndex = argumentIndex,
      lineNumber = Some(lineNumber),
      columnNumber = Some(columnNumber)
    )

  def methodReturn(
    code: String,
    columnNumber: Int,
    lineNumber: Int,
    order: Int,
    typeFullName: String,
    evaluationStrategy: String
  ): nodes.NewMethodReturn =
    nodes.NewMethodReturn(
      code = code,
      columnNumber = Some(columnNumber),
      lineNumber = Some(lineNumber),
      order = order,
      typeFullName = typeFullName,
      evaluationStrategy = evaluationStrategy
    )

  def modifier(name: String, order: Int): nodes.NewModifier =
    nodes.NewModifier(modifierType = name, order = order)

  def namespaceBlock(name: String, fullName: String, order: Int): nodes.NewNamespaceBlock =
    nodes.NewNamespaceBlock(name = name, fullName = fullName, order = order)

  def returnNode(code: String, lineNumber: Int, order: Int, argumentIndex: Int): nodes.NewReturn =
    nodes.NewReturn(code = code, lineNumber = Some(lineNumber), order = order, argumentIndex = argumentIndex)

  def typeArgument(order: Int): nodes.NewTypeArgument =
    nodes.NewTypeArgument(order = order)

  def typeDecl(name: String, fullName: String, order: Int, typeDeclFullName: String): nodes.NewTypeDecl =
    nodes.NewTypeDecl(name = name, fullName = fullName, order = order, astParentFullName = typeDeclFullName)

  def typeParameter(name: String, order: Int): nodes.NewTypeParameter =
    nodes.NewTypeParameter(name = name, order = order)

  def typeRef(
    typeFullName: String,
    dynamicTypeFullName: String,
    code: String,
    argumentIndex: Int,
    lineNumber: Int,
    columnNumber: Int,
    order: Int
  ): nodes.NewTypeRef = nodes.NewTypeRef(
    typeFullName = typeFullName,
    dynamicTypeHintFullName = List(dynamicTypeFullName),
    code = code,
    argumentIndex = argumentIndex,
    lineNumber = Some(lineNumber),
    columnNumber = Some(columnNumber),
    order = order
  )

  def unknown(
    typeFullName: String,
    code: String,
    order: Int,
    argumentIndex: Int,
    lineNumber: Int,
    columnNumber: Int
  ): nodes.NewUnknown = nodes.NewUnknown(
    typeFullName = typeFullName,
    code = code,
    order = order,
    argumentIndex = argumentIndex,
    lineNumber = Some(lineNumber),
    columnNumber = Some(columnNumber)
  )
}

package io.github.plume.oss.domain

import io.github.plume.oss.TestDomainResources.Companion.vertices
import io.github.plume.oss.domain.enums.VertexLabel
import io.github.plume.oss.domain.mappers.VertexMapper
import io.github.plume.oss.domain.models.vertices.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test


class MapperTest {

    @Test
    fun propertiesToMapTest() {
        vertices.forEach { v ->
            val map = VertexMapper.vertexToMap(v)
            val lbl = map.remove("label") as String
            when (VertexLabel.valueOf(lbl)) {
                VertexLabel.ARRAY_INITIALIZER -> {
                    v as ArrayInitializerVertex
                    assertEquals(v.order, map["order"])
                }
                VertexLabel.BINDING -> {
                    v as BindingVertex
                    assertEquals(v.name, map["name"])
                    assertEquals(v.signature, map["signature"])
                }
                VertexLabel.BLOCK -> {
                    v as BlockVertex
                    assertEquals(v.typeFullName, map["typeFullName"])
                    assertEquals(v.order, map["order"])
                    assertEquals(v.argumentIndex, map["argumentIndex"])
                    assertEquals(v.code, map["code"])
                    assertEquals(v.columnNumber, map["columnNumber"])
                    assertEquals(v.lineNumber, map["lineNumber"])
                }
                VertexLabel.CALL -> {
                    v as CallVertex
                    assertEquals(v.typeFullName, map["typeFullName"])
                    assertEquals(v.order, map["order"])
                    assertEquals(v.argumentIndex, map["argumentIndex"])
                    assertEquals(v.code, map["code"])
                    assertEquals(v.columnNumber, map["columnNumber"])
                    assertEquals(v.lineNumber, map["lineNumber"])
                    assertEquals(v.dispatchType.name, map["dispatchType"])
                    assertEquals(v.dynamicTypeHintFullName, map["dynamicTypeHintFullName"])
                    assertEquals(v.methodFullName, map["methodFullName"])
                    assertEquals(v.signature, map["signature"])
                    assertEquals(v.name, map["name"])

                }
                VertexLabel.CONTROL_STRUCTURE -> {
                    v as ControlStructureVertex
                    assertEquals(v.order, map["order"])
                    assertEquals(v.argumentIndex, map["argumentIndex"])
                    assertEquals(v.code, map["code"])
                    assertEquals(v.columnNumber, map["columnNumber"])
                    assertEquals(v.lineNumber, map["lineNumber"])
                }
                VertexLabel.FIELD_IDENTIFIER -> {
                    v as FieldIdentifierVertex
                    assertEquals(v.order, map["order"])
                    assertEquals(v.argumentIndex, map["argumentIndex"])
                    assertEquals(v.code, map["code"])
                    assertEquals(v.columnNumber, map["columnNumber"])
                    assertEquals(v.lineNumber, map["lineNumber"])
                    assertEquals(v.canonicalName, map["canonicalName"])
                }
                VertexLabel.FILE -> {
                    v as FileVertex
                    assertEquals(v.order, map["order"])
                    assertEquals(v.name, map["name"])
                    assertEquals(v.hash, map["hash"])
                }
                VertexLabel.IDENTIFIER -> {
                    v as IdentifierVertex
                    assertEquals(v.typeFullName, map["typeFullName"])
                    assertEquals(v.order, map["order"])
                    assertEquals(v.argumentIndex, map["argumentIndex"])
                    assertEquals(v.code, map["code"])
                    assertEquals(v.columnNumber, map["columnNumber"])
                    assertEquals(v.lineNumber, map["lineNumber"])
                    assertEquals(v.name, map["name"])
                }
                VertexLabel.JUMP_TARGET -> {
                    v as JumpTargetVertex
                    assertEquals(v.order, map["order"])
                    assertEquals(v.argumentIndex, map["argumentIndex"])
                    assertEquals(v.code, map["code"])
                    assertEquals(v.columnNumber, map["columnNumber"])
                    assertEquals(v.lineNumber, map["lineNumber"])
                    assertEquals(v.name, map["name"])
                }
                VertexLabel.LITERAL -> {
                    v as LiteralVertex
                    assertEquals(v.typeFullName, map["typeFullName"])
                    assertEquals(v.order, map["order"])
                    assertEquals(v.argumentIndex, map["argumentIndex"])
                    assertEquals(v.code, map["code"])
                    assertEquals(v.columnNumber, map["columnNumber"])
                    assertEquals(v.lineNumber, map["lineNumber"])
                }
                VertexLabel.LOCAL -> {
                    v as LocalVertex
                    assertEquals(v.typeFullName, map["typeFullName"])
                    assertEquals(v.order, map["order"])
                    assertEquals(v.code, map["code"])
                    assertEquals(v.columnNumber, map["columnNumber"])
                    assertEquals(v.lineNumber, map["lineNumber"])
                    assertEquals(v.name, map["name"])
                }
                VertexLabel.MEMBER -> {
                    v as MemberVertex
                    assertEquals(v.typeFullName, map["typeFullName"])
                    assertEquals(v.order, map["order"])
                    assertEquals(v.code, map["code"])
                    assertEquals(v.name, map["name"])
                }
                VertexLabel.META_DATA -> {
                    v as MetaDataVertex
                    assertEquals(v.language, map["language"])
                    assertEquals(v.language, map["version"])
                }
                VertexLabel.METHOD_PARAMETER_IN -> {
                    v as MethodParameterInVertex
                    assertEquals(v.typeFullName, map["typeFullName"])
                    assertEquals(v.order, map["order"])
                    assertEquals(v.code, map["code"])
                    assertEquals(v.lineNumber, map["lineNumber"])
                    assertEquals(v.name, map["name"])
                    assertEquals(v.evaluationStrategy.name, map["evaluationStrategy"])
                }
                VertexLabel.METHOD_REF -> {
                    v as MethodRefVertex
                    assertEquals(v.order, map["order"])
                    assertEquals(v.argumentIndex, map["argumentIndex"])
                    assertEquals(v.code, map["code"])
                    assertEquals(v.columnNumber, map["columnNumber"])
                    assertEquals(v.lineNumber, map["lineNumber"])
                    assertEquals(v.methodFullName, map["methodFullName"])
                    assertEquals(v.methodInstFullName, map["methodInstFullName"])
                }
                VertexLabel.METHOD_RETURN -> {
                    v as MethodReturnVertex
                    assertEquals(v.typeFullName, map["typeFullName"])
                    assertEquals(v.order, map["order"])
                    assertEquals(v.code, map["code"])
                    assertEquals(v.columnNumber, map["columnNumber"])
                    assertEquals(v.lineNumber, map["lineNumber"])
                    assertEquals(v.evaluationStrategy.name, map["evaluationStrategy"])
                }
                VertexLabel.METHOD -> {
                    v as MethodVertex
                    assertEquals(v.order, map["order"])
                    assertEquals(v.code, map["code"])
                    assertEquals(v.columnNumber, map["columnNumber"])
                    assertEquals(v.lineNumber, map["lineNumber"])
                    assertEquals(v.signature, map["signature"])
                    assertEquals(v.name, map["name"])
                    assertEquals(v.fullName, map["fullName"])
                }
                VertexLabel.MODIFIER -> {
                    v as ModifierVertex
                    assertEquals(v.order, map["order"])
                    assertEquals(v.modifierType.name, map["modifierType"])
                }
                VertexLabel.NAMESPACE_BLOCK -> {
                    v as NamespaceBlockVertex
                    assertEquals(v.order, map["order"])
                    assertEquals(v.name, map["name"])
                    assertEquals(v.fullName, map["fullName"])
                }
                VertexLabel.RETURN -> {
                    v as ReturnVertex
                    assertEquals(v.order, map["order"])
                    assertEquals(v.argumentIndex, map["argumentIndex"])
                    assertEquals(v.code, map["code"])
                    assertEquals(v.columnNumber, map["columnNumber"])
                    assertEquals(v.lineNumber, map["lineNumber"])
                }
                VertexLabel.TYPE_ARGUMENT -> {
                    v as TypeArgumentVertex
                    assertEquals(v.order, map["order"])
                }
                VertexLabel.TYPE_DECL -> {
                    v as TypeDeclVertex
                    assertEquals(v.order, map["order"])
                    assertEquals(v.name, map["name"])
                    assertEquals(v.fullName, map["fullName"])
                    assertEquals(v.typeDeclFullName, map["typeDeclFullName"])
                }
                VertexLabel.TYPE_PARAMETER -> {
                    v as TypeParameterVertex
                    assertEquals(v.order, map["order"])
                    assertEquals(v.name, map["name"])
                }
                VertexLabel.TYPE_REF -> {
                    v as TypeRefVertex
                    assertEquals(v.typeFullName, map["typeFullName"])
                    assertEquals(v.order, map["order"])
                    assertEquals(v.argumentIndex, map["argumentIndex"])
                    assertEquals(v.code, map["code"])
                    assertEquals(v.columnNumber, map["columnNumber"])
                    assertEquals(v.lineNumber, map["lineNumber"])
                    assertEquals(v.dynamicTypeFullName, map["dynamicTypeFullName"])
                }
                VertexLabel.TYPE -> {
                    v as TypeVertex
                    assertEquals(v.name, map["name"])
                    assertEquals(v.fullName, map["fullName"])
                    assertEquals(v.typeDeclFullName, map["typeDeclFullName"])
                }
                VertexLabel.UNKNOWN -> {
                    v as UnknownVertex
                    assertEquals(v.typeFullName, map["typeFullName"])
                    assertEquals(v.order, map["order"])
                    assertEquals(v.argumentIndex, map["argumentIndex"])
                    assertEquals(v.code, map["code"])
                    assertEquals(v.columnNumber, map["columnNumber"])
                    assertEquals(v.lineNumber, map["lineNumber"])
                }
            }
        }
    }

    @Test
    fun mapToVertexTest() {
        vertices.forEach { v ->
            val map = VertexMapper.vertexToMap(v)
            assertEquals(v, VertexMapper.mapToVertex(map))
        }
    }

}
package io.github.plume.oss.domain

import io.github.plume.oss.TestDomainResources.Companion.vertices
import io.github.plume.oss.domain.enums.EdgeLabel
import io.github.plume.oss.domain.enums.VertexLabel
import io.github.plume.oss.domain.mappers.VertexMapper
import io.github.plume.oss.domain.models.vertices.*
import io.shiftleft.codepropertygraph.generated.nodes.ArrayInitializer
import io.shiftleft.codepropertygraph.generated.nodes.NewArrayInitializer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test


class MapperTest {

    @Test
    fun propertiesToMapTest() {
        vertices.forEach { v ->
            val map = VertexMapper.vertexToMap(v)
            val lbl = map.remove("label") as String
            when (VertexLabel.valueOf(lbl)) {
                VertexLabel.ARRAY_INITIALIZER -> {
                    v as NewArrayInitializer
                    assertEquals(v.order, map["order"])
                }
                VertexLabel.BINDING -> {
                    v as NewBinding
                    assertEquals(v.name, map["name"])
                    assertEquals(v.signature, map["signature"])
                }
                VertexLabel.BLOCK -> {
                    v as NewBlock
                    assertEquals(v.typeFullName, map["typeFullName"])
                    assertEquals(v.order, map["order"])
                    assertEquals(v.argumentIndex, map["argumentIndex"])
                    assertEquals(v.code, map["code"])
                    assertEquals(v.columnNumber, map["columnNumber"])
                    assertEquals(v.lineNumber, map["lineNumber"])
                }
                VertexLabel.CALL -> {
                    v as Call
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
                    v as ControlStructure
                    assertEquals(v.order, map["order"])
                    assertEquals(v.argumentIndex, map["argumentIndex"])
                    assertEquals(v.code, map["code"])
                    assertEquals(v.columnNumber, map["columnNumber"])
                    assertEquals(v.lineNumber, map["lineNumber"])
                }
                VertexLabel.FIELD_IDENTIFIER -> {
                    v as FieldIdentifier
                    assertEquals(v.order, map["order"])
                    assertEquals(v.argumentIndex, map["argumentIndex"])
                    assertEquals(v.code, map["code"])
                    assertEquals(v.columnNumber, map["columnNumber"])
                    assertEquals(v.lineNumber, map["lineNumber"])
                    assertEquals(v.canonicalName, map["canonicalName"])
                }
                VertexLabel.FILE -> {
                    v as File
                    assertEquals(v.order, map["order"])
                    assertEquals(v.name, map["name"])
                    assertEquals(v.hash, map["hash"])
                }
                VertexLabel.IDENTIFIER -> {
                    v as Identifier
                    assertEquals(v.typeFullName, map["typeFullName"])
                    assertEquals(v.order, map["order"])
                    assertEquals(v.argumentIndex, map["argumentIndex"])
                    assertEquals(v.code, map["code"])
                    assertEquals(v.columnNumber, map["columnNumber"])
                    assertEquals(v.lineNumber, map["lineNumber"])
                    assertEquals(v.name, map["name"])
                }
                VertexLabel.JUMP_TARGET -> {
                    v as JumpTarget
                    assertEquals(v.order, map["order"])
                    assertEquals(v.argumentIndex, map["argumentIndex"])
                    assertEquals(v.code, map["code"])
                    assertEquals(v.columnNumber, map["columnNumber"])
                    assertEquals(v.lineNumber, map["lineNumber"])
                    assertEquals(v.name, map["name"])
                }
                VertexLabel.LITERAL -> {
                    v as Literal
                    assertEquals(v.typeFullName, map["typeFullName"])
                    assertEquals(v.order, map["order"])
                    assertEquals(v.argumentIndex, map["argumentIndex"])
                    assertEquals(v.code, map["code"])
                    assertEquals(v.columnNumber, map["columnNumber"])
                    assertEquals(v.lineNumber, map["lineNumber"])
                }
                VertexLabel.LOCAL -> {
                    v as Local
                    assertEquals(v.typeFullName, map["typeFullName"])
                    assertEquals(v.order, map["order"])
                    assertEquals(v.code, map["code"])
                    assertEquals(v.columnNumber, map["columnNumber"])
                    assertEquals(v.lineNumber, map["lineNumber"])
                    assertEquals(v.name, map["name"])
                }
                VertexLabel.MEMBER -> {
                    v as Member
                    assertEquals(v.typeFullName, map["typeFullName"])
                    assertEquals(v.order, map["order"])
                    assertEquals(v.code, map["code"])
                    assertEquals(v.name, map["name"])
                }
                VertexLabel.META_DATA -> {
                    v as MetaData
                    assertEquals(v.language, map["language"])
                    assertEquals(v.language, map["version"])
                }
                VertexLabel.METHOD_PARAMETER_IN -> {
                    v as MethodParameterIn
                    assertEquals(v.typeFullName, map["typeFullName"])
                    assertEquals(v.order, map["order"])
                    assertEquals(v.code, map["code"])
                    assertEquals(v.lineNumber, map["lineNumber"])
                    assertEquals(v.name, map["name"])
                    assertEquals(v.evaluationStrategy.name, map["evaluationStrategy"])
                }
                VertexLabel.METHOD_REF -> {
                    v as MethodRef
                    assertEquals(v.order, map["order"])
                    assertEquals(v.argumentIndex, map["argumentIndex"])
                    assertEquals(v.code, map["code"])
                    assertEquals(v.columnNumber, map["columnNumber"])
                    assertEquals(v.lineNumber, map["lineNumber"])
                    assertEquals(v.methodFullName, map["methodFullName"])
                    assertEquals(v.methodInstFullName, map["methodInstFullName"])
                }
                VertexLabel.METHOD_RETURN -> {
                    v as MethodReturn
                    assertEquals(v.typeFullName, map["typeFullName"])
                    assertEquals(v.order, map["order"])
                    assertEquals(v.code, map["code"])
                    assertEquals(v.columnNumber, map["columnNumber"])
                    assertEquals(v.lineNumber, map["lineNumber"])
                    assertEquals(v.evaluationStrategy.name, map["evaluationStrategy"])
                }
                VertexLabel.METHOD -> {
                    v as Method
                    assertEquals(v.order, map["order"])
                    assertEquals(v.code, map["code"])
                    assertEquals(v.columnNumber, map["columnNumber"])
                    assertEquals(v.lineNumber, map["lineNumber"])
                    assertEquals(v.signature, map["signature"])
                    assertEquals(v.name, map["name"])
                    assertEquals(v.fullName, map["fullName"])
                }
                VertexLabel.MODIFIER -> {
                    v as Modifier
                    assertEquals(v.order, map["order"])
                    assertEquals(v.modifierType.name, map["modifierType"])
                }
                VertexLabel.NAMESPACE_BLOCK -> {
                    v as NamespaceBlock
                    assertEquals(v.order, map["order"])
                    assertEquals(v.name, map["name"])
                    assertEquals(v.fullName, map["fullName"])
                }
                VertexLabel.RETURN -> {
                    v as Return
                    assertEquals(v.order, map["order"])
                    assertEquals(v.argumentIndex, map["argumentIndex"])
                    assertEquals(v.code, map["code"])
                    assertEquals(v.columnNumber, map["columnNumber"])
                    assertEquals(v.lineNumber, map["lineNumber"])
                }
                VertexLabel.TYPE_ARGUMENT -> {
                    v as TypeArgument
                    assertEquals(v.order, map["order"])
                }
                VertexLabel.TYPE_DECL -> {
                    v as TypeDecl
                    assertEquals(v.order, map["order"])
                    assertEquals(v.name, map["name"])
                    assertEquals(v.fullName, map["fullName"])
                    assertEquals(v.typeDeclFullName, map["typeDeclFullName"])
                }
                VertexLabel.TYPE_PARAMETER -> {
                    v as TypeParameter
                    assertEquals(v.order, map["order"])
                    assertEquals(v.name, map["name"])
                }
                VertexLabel.TYPE_REF -> {
                    v as TypeRef
                    assertEquals(v.typeFullName, map["typeFullName"])
                    assertEquals(v.order, map["order"])
                    assertEquals(v.argumentIndex, map["argumentIndex"])
                    assertEquals(v.code, map["code"])
                    assertEquals(v.columnNumber, map["columnNumber"])
                    assertEquals(v.lineNumber, map["lineNumber"])
                    assertEquals(v.dynamicTypeFullName, map["dynamicTypeFullName"])
                }
                VertexLabel.TYPE -> {
                    v as Type
                    assertEquals(v.name, map["name"])
                    assertEquals(v.fullName, map["fullName"])
                    assertEquals(v.typeDeclFullName, map["typeDeclFullName"])
                }
                VertexLabel.UNKNOWN -> {
                    v as Unknown
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

    @Test
    fun vertexConstraintsTest() {
        vertices.forEach { src ->
            when (src) {
                is ArrayInitializerVertex -> {
                    val clazz = ArrayInitializerVertex
                    EdgeLabel.values().forEach { e ->
                        VertexLabel.values().forEach { tgt ->
                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            } else {
                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            }
                        }
                    }
                }
                is BindingVertex -> {
                    val clazz = BindingVertex
                    EdgeLabel.values().forEach { e ->
                        VertexLabel.values().forEach { tgt ->
                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            } else {
                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            }
                        }
                    }
                }
                is BlockVertex -> {
                    val clazz = BlockVertex
                    EdgeLabel.values().forEach { e ->
                        VertexLabel.values().forEach { tgt ->
                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            } else {
                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            }
                        }
                    }
                }
                is CallVertex -> {
                    val clazz = CallVertex
                    EdgeLabel.values().forEach { e ->
                        VertexLabel.values().forEach { tgt ->
                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            } else {
                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            }
                        }
                    }
                }
                is ControlStructureVertex -> {
                    val clazz = ControlStructureVertex
                    EdgeLabel.values().forEach { e ->
                        VertexLabel.values().forEach { tgt ->
                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            } else {
                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            }
                        }
                    }
                }
                is FieldIdentifierVertex -> {
                    val clazz = FieldIdentifierVertex
                    EdgeLabel.values().forEach { e ->
                        VertexLabel.values().forEach { tgt ->
                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            } else {
                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            }
                        }
                    }
                }
                is FileVertex -> {
                    val clazz = FileVertex
                    EdgeLabel.values().forEach { e ->
                        VertexLabel.values().forEach { tgt ->
                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            } else {
                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            }
                        }
                    }
                }
                is IdentifierVertex -> {
                    val clazz = IdentifierVertex
                    EdgeLabel.values().forEach { e ->
                        VertexLabel.values().forEach { tgt ->
                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            } else {
                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            }
                        }
                    }
                }
                is JumpTargetVertex -> {
                    val clazz = JumpTargetVertex
                    EdgeLabel.values().forEach { e ->
                        VertexLabel.values().forEach { tgt ->
                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            } else {
                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            }
                        }
                    }
                }
                is LiteralVertex -> {
                    val clazz = LiteralVertex
                    EdgeLabel.values().forEach { e ->
                        VertexLabel.values().forEach { tgt ->
                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            } else {
                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            }
                        }
                    }
                }
                is LocalVertex -> {
                    val clazz = LocalVertex
                    EdgeLabel.values().forEach { e ->
                        VertexLabel.values().forEach { tgt ->
                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            } else {
                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            }
                        }
                    }
                }
                is MemberVertex -> {
                    val clazz = MemberVertex
                    EdgeLabel.values().forEach { e ->
                        VertexLabel.values().forEach { tgt ->
                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            } else {
                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            }
                        }
                    }
                }
                is MetaDataVertex -> {
                    val clazz = MetaDataVertex
                    EdgeLabel.values().forEach { e ->
                        VertexLabel.values().forEach { tgt ->
                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            } else {
                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            }
                        }
                    }
                }
                is MethodParameterInVertex -> {
                    val clazz = MethodParameterInVertex
                    EdgeLabel.values().forEach { e ->
                        VertexLabel.values().forEach { tgt ->
                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            } else {
                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            }
                        }
                    }
                }
                is MethodRefVertex -> {
                    val clazz = MethodRefVertex
                    EdgeLabel.values().forEach { e ->
                        VertexLabel.values().forEach { tgt ->
                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            } else {
                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            }
                        }
                    }
                }
                is MethodReturnVertex -> {
                    val clazz = MethodReturnVertex
                    EdgeLabel.values().forEach { e ->
                        VertexLabel.values().forEach { tgt ->
                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            } else {
                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            }
                        }
                    }
                }
                is MethodVertex -> {
                    val clazz = MethodVertex
                    EdgeLabel.values().forEach { e ->
                        VertexLabel.values().forEach { tgt ->
                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            } else {
                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            }
                        }
                    }
                }
                is ModifierVertex -> {
                    val clazz = ModifierVertex
                    EdgeLabel.values().forEach { e ->
                        VertexLabel.values().forEach { tgt ->
                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            } else {
                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            }
                        }
                    }
                }
                is NamespaceBlockVertex -> {
                    val clazz = NamespaceBlockVertex
                    EdgeLabel.values().forEach { e ->
                        VertexLabel.values().forEach { tgt ->
                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            } else {
                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            }
                        }
                    }
                }
                is ReturnVertex -> {
                    val clazz = ReturnVertex
                    EdgeLabel.values().forEach { e ->
                        VertexLabel.values().forEach { tgt ->
                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            } else {
                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            }
                        }
                    }
                }
                is TypeArgumentVertex -> {
                    val clazz = TypeArgumentVertex
                    EdgeLabel.values().forEach { e ->
                        VertexLabel.values().forEach { tgt ->
                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            } else {
                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            }
                        }
                    }
                }
                is TypeDeclVertex -> {
                    val clazz = TypeDeclVertex
                    EdgeLabel.values().forEach { e ->
                        VertexLabel.values().forEach { tgt ->
                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            } else {
                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            }
                        }
                    }
                }
                is TypeParameterVertex -> {
                    val clazz = TypeParameterVertex
                    EdgeLabel.values().forEach { e ->
                        VertexLabel.values().forEach { tgt ->
                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            } else {
                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            }
                        }
                    }
                }
                is TypeRefVertex -> {
                    val clazz = TypeRefVertex
                    EdgeLabel.values().forEach { e ->
                        VertexLabel.values().forEach { tgt ->
                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            } else {
                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            }
                        }
                    }
                }
                is TypeVertex -> {
                    val clazz = TypeVertex
                    EdgeLabel.values().forEach { e ->
                        VertexLabel.values().forEach { tgt ->
                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            } else {
                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            }
                        }
                    }
                }
                is UnknownVertex -> {
                    val clazz = UnknownVertex
                    EdgeLabel.values().forEach { e ->
                        VertexLabel.values().forEach { tgt ->
                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            } else {
                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
                            }
                        }
                    }
                }
            }
        }
    }

}
package io.github.plume.oss.domain

import io.github.plume.oss.TestDomainResources.Companion.vertices
import io.github.plume.oss.domain.enums.VertexLabel
import io.github.plume.oss.domain.mappers.VertexMapper
import io.shiftleft.codepropertygraph.generated.nodes.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test


class MapperTest {

    @Test
    fun propertiesToMapTest() {
        vertices.forEach { v ->
            val map = VertexMapper.vertexToMap(v)
            val lbl = map.remove("label") as String
            val node = v.build()
            when (VertexLabel.valueOf(lbl)) {
                VertexLabel.ARRAY_INITIALIZER -> {
                    node as NewArrayInitializer
                    assertEquals(node.order(), map["order"])
                }
                VertexLabel.BINDING -> {
                    node as NewBinding
                    assertEquals(node.name(), map["name"])
                    assertEquals(node.signature(), map["signature"])
                }
                VertexLabel.BLOCK -> {
                    node as NewBlock
                    assertEquals(node.typeFullName(), map["typeFullName"])
                    assertEquals(node.order(), map["order"])
                    assertEquals(node.argumentIndex(), map["argumentIndex"])
                    assertEquals(node.code(), map["code"])
                    assertEquals(node.columnNumber().get(), map["columnNumber"])
                    assertEquals(node.lineNumber().get(), map["lineNumber"])
                }
                VertexLabel.CALL -> {
                    node as NewCall
                    assertEquals(node.typeFullName(), map["typeFullName"])
                    assertEquals(node.order(), map["order"])
                    assertEquals(node.argumentIndex(), map["argumentIndex"])
                    assertEquals(node.code(), map["code"])
                    assertEquals(node.columnNumber().get(), map["columnNumber"])
                    assertEquals(node.lineNumber().get(), map["lineNumber"])
                    assertEquals(node.dispatchType(), map["dispatchType"])
                    assertEquals(node.dynamicTypeHintFullName(), map["dynamicTypeHintFullName"])
                    assertEquals(node.methodFullName(), map["methodFullName"])
                    assertEquals(node.signature(), map["signature"])
                    assertEquals(node.name(), map["name"])

                }
                VertexLabel.CONTROL_STRUCTURE -> {
                    node as NewControlStructure
                    assertEquals(node.order(), map["order"])
                    assertEquals(node.argumentIndex(), map["argumentIndex"])
                    assertEquals(node.code(), map["code"])
                    assertEquals(node.columnNumber().get(), map["columnNumber"])
                    assertEquals(node.lineNumber().get(), map["lineNumber"])
                }
                VertexLabel.FIELD_IDENTIFIER -> {
                    node as NewFieldIdentifier
                    assertEquals(node.order(), map["order"])
                    assertEquals(node.argumentIndex(), map["argumentIndex"])
                    assertEquals(node.code(), map["code"])
                    assertEquals(node.columnNumber().get(), map["columnNumber"])
                    assertEquals(node.lineNumber().get(), map["lineNumber"])
                    assertEquals(node.canonicalName(), map["canonicalName"])
                }
                VertexLabel.FILE -> {
                    node as NewFile
                    assertEquals(node.order(), map["order"])
                    assertEquals(node.name(), map["name"])
                    assertEquals(node.hash().get(), map["hash"])
                }
                VertexLabel.IDENTIFIER -> {
                    node as NewIdentifier
                    assertEquals(node.typeFullName(), map["typeFullName"])
                    assertEquals(node.order(), map["order"])
                    assertEquals(node.argumentIndex(), map["argumentIndex"])
                    assertEquals(node.code(), map["code"])
                    assertEquals(node.columnNumber().get(), map["columnNumber"])
                    assertEquals(node.lineNumber().get(), map["lineNumber"])
                    assertEquals(node.name(), map["name"])
                }
                VertexLabel.JUMP_TARGET -> {
                    node as NewJumpTarget
                    assertEquals(node.order(), map["order"])
                    assertEquals(node.argumentIndex(), map["argumentIndex"])
                    assertEquals(node.code(), map["code"])
                    assertEquals(node.columnNumber().get(), map["columnNumber"])
                    assertEquals(node.lineNumber().get(), map["lineNumber"])
                    assertEquals(node.name(), map["name"])
                }
                VertexLabel.LITERAL -> {
                    node as NewLiteral
                    assertEquals(node.typeFullName(), map["typeFullName"])
                    assertEquals(node.order(), map["order"])
                    assertEquals(node.argumentIndex(), map["argumentIndex"])
                    assertEquals(node.code(), map["code"])
                    assertEquals(node.columnNumber().get(), map["columnNumber"])
                    assertEquals(node.lineNumber().get(), map["lineNumber"])
                }
                VertexLabel.LOCAL -> {
                    node as NewLocal
                    assertEquals(node.typeFullName(), map["typeFullName"])
                    assertEquals(node.order(), map["order"])
                    assertEquals(node.code(), map["code"])
                    assertEquals(node.columnNumber().get(), map["columnNumber"])
                    assertEquals(node.lineNumber().get(), map["lineNumber"])
                    assertEquals(node.name(), map["name"])
                }
                VertexLabel.MEMBER -> {
                    node as NewMember
                    assertEquals(node.typeFullName(), map["typeFullName"])
                    assertEquals(node.order(), map["order"])
                    assertEquals(node.code(), map["code"])
                    assertEquals(node.name(), map["name"])
                }
                VertexLabel.META_DATA -> {
                    node as NewMetaData
                    assertEquals(node.language(), map["language"])
                    assertEquals(node.version(), map["version"])
                }
                VertexLabel.METHOD_PARAMETER_IN -> {
                    node as NewMethodParameterIn
                    assertEquals(node.typeFullName(), map["typeFullName"])
                    assertEquals(node.order(), map["order"])
                    assertEquals(node.code(), map["code"])
                    assertEquals(node.lineNumber().get(), map["lineNumber"])
                    assertEquals(node.name(), map["name"])
                    assertEquals(node.evaluationStrategy(), map["evaluationStrategy"])
                }
                VertexLabel.METHOD_REF -> {
                    node as NewMethodRef
                    assertEquals(node.order(), map["order"])
                    assertEquals(node.argumentIndex(), map["argumentIndex"])
                    assertEquals(node.code(), map["code"])
                    assertEquals(node.columnNumber().get(), map["columnNumber"])
                    assertEquals(node.lineNumber().get(), map["lineNumber"])
                    assertEquals(node.methodFullName(), map["methodFullName"])
                    assertEquals(node.methodInstFullName(), map["methodInstFullName"])
                }
                VertexLabel.METHOD_RETURN -> {
                    node as NewMethodReturn
                    assertEquals(node.typeFullName(), map["typeFullName"])
                    assertEquals(node.order(), map["order"])
                    assertEquals(node.code(), map["code"])
                    assertEquals(node.columnNumber().get(), map["columnNumber"])
                    assertEquals(node.lineNumber().get(), map["lineNumber"])
                    assertEquals(node.evaluationStrategy(), map["evaluationStrategy"])
                }
                VertexLabel.METHOD -> {
                    node as NewMethod
                    assertEquals(node.order(), map["order"])
                    assertEquals(node.code(), map["code"])
                    assertEquals(node.columnNumber().get(), map["columnNumber"])
                    assertEquals(node.lineNumber().get(), map["lineNumber"])
                    assertEquals(node.signature(), map["signature"])
                    assertEquals(node.name(), map["name"])
                    assertEquals(node.fullName(), map["fullName"])
                }
                VertexLabel.MODIFIER -> {
                    node as NewModifier
                    assertEquals(node.order(), map["order"])
                    assertEquals(node.modifierType(), map["modifierType"])
                }
                VertexLabel.NAMESPACE_BLOCK -> {
                    node as NewNamespaceBlock
                    assertEquals(node.order(), map["order"])
                    assertEquals(node.name(), map["name"])
                    assertEquals(node.fullName(), map["fullName"])
                }
                VertexLabel.RETURN -> {
                    node as NewReturn
                    assertEquals(node.order(), map["order"])
                    assertEquals(node.argumentIndex(), map["argumentIndex"])
                    assertEquals(node.code(), map["code"])
                    assertEquals(node.columnNumber().get(), map["columnNumber"])
                    assertEquals(node.lineNumber().get(), map["lineNumber"])
                }
                VertexLabel.TYPE_ARGUMENT -> {
                    node as NewTypeArgument
                    assertEquals(node.order(), map["order"])
                }
                VertexLabel.TYPE_DECL -> {
                    node as NewTypeDecl
                    assertEquals(node.order(), map["order"])
                    assertEquals(node.name(), map["name"])
                    assertEquals(node.fullName(), map["fullName"])
                }
                VertexLabel.TYPE_PARAMETER -> {
                    node as NewTypeParameter
                    assertEquals(node.order(), map["order"])
                    assertEquals(node.name(), map["name"])
                }
                VertexLabel.TYPE_REF -> {
                    node as NewTypeRef
                    assertEquals(node.typeFullName(), map["typeFullName"])
                    assertEquals(node.order(), map["order"])
                    assertEquals(node.argumentIndex(), map["argumentIndex"])
                    assertEquals(node.code(), map["code"])
                    assertEquals(node.columnNumber().get(), map["columnNumber"])
                    assertEquals(node.lineNumber().get(), map["lineNumber"])
                    assertEquals(node.dynamicTypeHintFullName(), map["dynamicTypeHintFullName"])
                }
                VertexLabel.TYPE -> {
                    node as NewType
                    assertEquals(node.name(), map["name"])
                    assertEquals(node.fullName(), map["fullName"])
                    assertEquals(node.typeDeclFullName(), map["typeDeclFullName"])
                }
                VertexLabel.UNKNOWN -> {
                    node as NewUnknown
                    assertEquals(node.typeFullName(), map["typeFullName"])
                    assertEquals(node.order(), map["order"])
                    assertEquals(node.argumentIndex(), map["argumentIndex"])
                    assertEquals(node.code(), map["code"])
                    assertEquals(node.columnNumber().get(), map["columnNumber"])
                    assertEquals(node.lineNumber().get(), map["lineNumber"])
                }
            }
        }
    }

    @Test
    fun mapToVertexTest() {
        vertices.forEach { v ->
            // Only check properties currently used by Plume
            val map = VertexMapper.vertexToMap(v)
            val expectedProperties = v.build().properties()
            val actualProperties = VertexMapper.mapToVertex(map).build().properties()
            val excludedKeys = expectedProperties.keySet().diff(actualProperties.keySet())
            excludedKeys.foreach { expectedProperties.`$minus`(it) }
            assertEquals(expectedProperties, actualProperties)
        }
    }

    @Test
    fun vertexConstraintsTest() {
        println("hell0o")
//        vertices.forEach { src ->
//            when (src) {
//                is ArrayInitializerVertex -> {
//                    val clazz = ArrayInitializerVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//                is BindingVertex -> {
//                    val clazz = BindingVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//                is BlockVertex -> {
//                    val clazz = BlockVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//                is CallVertex -> {
//                    val clazz = CallVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//                is ControlStructureVertex -> {
//                    val clazz = ControlStructureVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//                is FieldIdentifierVertex -> {
//                    val clazz = FieldIdentifierVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//                is FileVertex -> {
//                    val clazz = FileVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//                is IdentifierVertex -> {
//                    val clazz = IdentifierVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//                is JumpTargetVertex -> {
//                    val clazz = JumpTargetVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//                is LiteralVertex -> {
//                    val clazz = LiteralVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//                is LocalVertex -> {
//                    val clazz = LocalVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//                is MemberVertex -> {
//                    val clazz = MemberVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//                is MetaDataVertex -> {
//                    val clazz = MetaDataVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//                is MethodParameterInVertex -> {
//                    val clazz = MethodParameterInVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//                is MethodRefVertex -> {
//                    val clazz = MethodRefVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//                is MethodReturnVertex -> {
//                    val clazz = MethodReturnVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//                is MethodVertex -> {
//                    val clazz = MethodVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//                is ModifierVertex -> {
//                    val clazz = ModifierVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//                is NamespaceBlockVertex -> {
//                    val clazz = NamespaceBlockVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//                is ReturnVertex -> {
//                    val clazz = ReturnVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//                is TypeArgumentVertex -> {
//                    val clazz = TypeArgumentVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//                is TypeDeclVertex -> {
//                    val clazz = TypeDeclVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//                is TypeParameterVertex -> {
//                    val clazz = TypeParameterVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//                is TypeRefVertex -> {
//                    val clazz = TypeRefVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//                is TypeVertex -> {
//                    val clazz = TypeVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//                is UnknownVertex -> {
//                    val clazz = UnknownVertex
//                    EdgeLabel.values().forEach { e ->
//                        VertexLabel.values().forEach { tgt ->
//                            if (clazz.VALID_OUT_EDGES.containsKey(e) && clazz.VALID_OUT_EDGES[e]!!.contains(tgt)) {
//                                assertTrue(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            } else {
//                                assertFalse(VertexMapper.checkSchemaConstraints(clazz.LABEL, e, tgt))
//                            }
//                        }
//                    }
//                }
//            }
//        }
    }

}
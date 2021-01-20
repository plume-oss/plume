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
            when (VertexLabel.valueOf(lbl)) {
                VertexLabel.ARRAY_INITIALIZER -> {
                    v as NewArrayInitializer
                    assertEquals(v.order(), map["order"])
                }
                VertexLabel.BINDING -> {
                    v as NewBinding
                    assertEquals(v.name(), map["name"])
                    assertEquals(v.signature(), map["signature"])
                }
                VertexLabel.BLOCK -> {
                    v as NewBlock
                    assertEquals(v.typeFullName(), map["typeFullName"])
                    assertEquals(v.order(), map["order"])
                    assertEquals(v.argumentIndex(), map["argumentIndex"])
                    assertEquals(v.code(), map["code"])
                    assertEquals(v.columnNumber(), map["columnNumber"])
                    assertEquals(v.lineNumber(), map["lineNumber"])
                }
                VertexLabel.CALL -> {
                    v as NewCall
                    assertEquals(v.typeFullName(), map["typeFullName"])
                    assertEquals(v.order(), map["order"])
                    assertEquals(v.argumentIndex(), map["argumentIndex"])
                    assertEquals(v.code(), map["code"])
                    assertEquals(v.columnNumber(), map["columnNumber"])
                    assertEquals(v.lineNumber(), map["lineNumber"])
                    assertEquals(v.dispatchType(), map["dispatchType"])
                    assertEquals(v.dynamicTypeHintFullName().head(), map["dynamicTypeHintFullName"])
                    assertEquals(v.methodFullName(), map["methodFullName"])
                    assertEquals(v.signature(), map["signature"])
                    assertEquals(v.name(), map["name"])

                }
                VertexLabel.CONTROL_STRUCTURE -> {
                    v as NewControlStructure
                    assertEquals(v.order(), map["order"])
                    assertEquals(v.argumentIndex(), map["argumentIndex"])
                    assertEquals(v.code(), map["code"])
                    assertEquals(v.columnNumber(), map["columnNumber"])
                    assertEquals(v.lineNumber(), map["lineNumber"])
                }
                VertexLabel.FIELD_IDENTIFIER -> {
                    v as NewFieldIdentifier
                    assertEquals(v.order(), map["order"])
                    assertEquals(v.argumentIndex(), map["argumentIndex"])
                    assertEquals(v.code(), map["code"])
                    assertEquals(v.columnNumber(), map["columnNumber"])
                    assertEquals(v.lineNumber(), map["lineNumber"])
                    assertEquals(v.canonicalName(), map["canonicalName"])
                }
                VertexLabel.FILE -> {
                    v as NewFile
                    assertEquals(v.order(), map["order"])
                    assertEquals(v.name(), map["name"])
                    assertEquals(v.hash(), map["hash"])
                }
                VertexLabel.IDENTIFIER -> {
                    v as NewIdentifier
                    assertEquals(v.typeFullName(), map["typeFullName"])
                    assertEquals(v.order(), map["order"])
                    assertEquals(v.argumentIndex(), map["argumentIndex"])
                    assertEquals(v.code(), map["code"])
                    assertEquals(v.columnNumber(), map["columnNumber"])
                    assertEquals(v.lineNumber(), map["lineNumber"])
                    assertEquals(v.name(), map["name"])
                }
                VertexLabel.JUMP_TARGET -> {
                    v as NewJumpTarget
                    assertEquals(v.order(), map["order"])
                    assertEquals(v.argumentIndex(), map["argumentIndex"])
                    assertEquals(v.code(), map["code"])
                    assertEquals(v.columnNumber(), map["columnNumber"])
                    assertEquals(v.lineNumber(), map["lineNumber"])
                    assertEquals(v.name(), map["name"])
                }
                VertexLabel.LITERAL -> {
                    v as NewLiteral
                    assertEquals(v.typeFullName(), map["typeFullName"])
                    assertEquals(v.order(), map["order"])
                    assertEquals(v.argumentIndex(), map["argumentIndex"])
                    assertEquals(v.code(), map["code"])
                    assertEquals(v.columnNumber(), map["columnNumber"])
                    assertEquals(v.lineNumber(), map["lineNumber"])
                }
                VertexLabel.LOCAL -> {
                    v as NewLocal
                    assertEquals(v.typeFullName(), map["typeFullName"])
                    assertEquals(v.order(), map["order"])
                    assertEquals(v.code(), map["code"])
                    assertEquals(v.columnNumber(), map["columnNumber"])
                    assertEquals(v.lineNumber(), map["lineNumber"])
                    assertEquals(v.name(), map["name"])
                }
                VertexLabel.MEMBER -> {
                    v as NewMember
                    assertEquals(v.typeFullName(), map["typeFullName"])
                    assertEquals(v.order(), map["order"])
                    assertEquals(v.code(), map["code"])
                    assertEquals(v.name(), map["name"])
                }
                VertexLabel.META_DATA -> {
                    v as NewMetaData
                    assertEquals(v.language(), map["language"])
                    assertEquals(v.version(), map["version"])
                }
                VertexLabel.METHOD_PARAMETER_IN -> {
                    v as NewMethodParameterIn
                    assertEquals(v.typeFullName(), map["typeFullName"])
                    assertEquals(v.order(), map["order"])
                    assertEquals(v.code(), map["code"])
                    assertEquals(v.lineNumber(), map["lineNumber"])
                    assertEquals(v.name(), map["name"])
                    assertEquals(v.evaluationStrategy(), map["evaluationStrategy"])
                }
                VertexLabel.METHOD_REF -> {
                    v as NewMethodRef
                    assertEquals(v.order(), map["order"])
                    assertEquals(v.argumentIndex(), map["argumentIndex"])
                    assertEquals(v.code(), map["code"])
                    assertEquals(v.columnNumber(), map["columnNumber"])
                    assertEquals(v.lineNumber(), map["lineNumber"])
                    assertEquals(v.methodFullName(), map["methodFullName"])
                    assertEquals(v.methodInstFullName(), map["methodInstFullName"])
                }
                VertexLabel.METHOD_RETURN -> {
                    v as NewMethodReturn
                    assertEquals(v.typeFullName(), map["typeFullName"])
                    assertEquals(v.order(), map["order"])
                    assertEquals(v.code(), map["code"])
                    assertEquals(v.columnNumber(), map["columnNumber"])
                    assertEquals(v.lineNumber(), map["lineNumber"])
                    assertEquals(v.evaluationStrategy(), map["evaluationStrategy"])
                }
                VertexLabel.METHOD -> {
                    v as NewMethod
                    assertEquals(v.order(), map["order"])
                    assertEquals(v.code(), map["code"])
                    assertEquals(v.columnNumber(), map["columnNumber"])
                    assertEquals(v.lineNumber(), map["lineNumber"])
                    assertEquals(v.signature(), map["signature"])
                    assertEquals(v.name(), map["name"])
                    assertEquals(v.fullName(), map["fullName"])
                }
                VertexLabel.MODIFIER -> {
                    v as NewModifier
                    assertEquals(v.order(), map["order"])
                    assertEquals(v.modifierType(), map["modifierType"])
                }
                VertexLabel.NAMESPACE_BLOCK -> {
                    v as NewNamespaceBlock
                    assertEquals(v.order(), map["order"])
                    assertEquals(v.name(), map["name"])
                    assertEquals(v.fullName(), map["fullName"])
                }
                VertexLabel.RETURN -> {
                    v as NewReturn
                    assertEquals(v.order(), map["order"])
                    assertEquals(v.argumentIndex(), map["argumentIndex"])
                    assertEquals(v.code(), map["code"])
                    assertEquals(v.columnNumber(), map["columnNumber"])
                    assertEquals(v.lineNumber(), map["lineNumber"])
                }
                VertexLabel.TYPE_ARGUMENT -> {
                    v as NewTypeArgument
                    assertEquals(v.order(), map["order"])
                }
                VertexLabel.TYPE_DECL -> {
                    v as NewTypeDecl
                    assertEquals(v.order(), map["order"])
                    assertEquals(v.name(), map["name"])
                    assertEquals(v.fullName(), map["fullName"])
                }
                VertexLabel.TYPE_PARAMETER -> {
                    v as NewTypeParameter
                    assertEquals(v.order(), map["order"])
                    assertEquals(v.name(), map["name"])
                }
                VertexLabel.TYPE_REF -> {
                    v as NewTypeRef
                    assertEquals(v.typeFullName(), map["typeFullName"])
                    assertEquals(v.order(), map["order"])
                    assertEquals(v.argumentIndex(), map["argumentIndex"])
                    assertEquals(v.code(), map["code"])
                    assertEquals(v.columnNumber(), map["columnNumber"])
                    assertEquals(v.lineNumber(), map["lineNumber"])
                    assertEquals(v.dynamicTypeHintFullName().head(), map["dynamicTypeFullName"])
                }
                VertexLabel.TYPE -> {
                    v as NewType
                    assertEquals(v.name(), map["name"])
                    assertEquals(v.fullName(), map["fullName"])
                    assertEquals(v.typeDeclFullName(), map["typeDeclFullName"])
                }
                VertexLabel.UNKNOWN -> {
                    v as NewUnknown
                    assertEquals(v.typeFullName(), map["typeFullName"])
                    assertEquals(v.order(), map["order"])
                    assertEquals(v.argumentIndex(), map["argumentIndex"])
                    assertEquals(v.code(), map["code"])
                    assertEquals(v.columnNumber(), map["columnNumber"])
                    assertEquals(v.lineNumber(), map["lineNumber"])
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
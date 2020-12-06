package za.ac.sun.plume.domain

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import za.ac.sun.plume.TestDomainResources
import za.ac.sun.plume.TestDomainResources.Companion.DISPATCH_1
import za.ac.sun.plume.TestDomainResources.Companion.DISPATCH_2
import za.ac.sun.plume.TestDomainResources.Companion.EVAL_1
import za.ac.sun.plume.TestDomainResources.Companion.EVAL_2
import za.ac.sun.plume.TestDomainResources.Companion.INT_1
import za.ac.sun.plume.TestDomainResources.Companion.INT_2
import za.ac.sun.plume.TestDomainResources.Companion.MOD_1
import za.ac.sun.plume.TestDomainResources.Companion.MOD_2
import za.ac.sun.plume.TestDomainResources.Companion.STRING_1
import za.ac.sun.plume.TestDomainResources.Companion.STRING_2
import za.ac.sun.plume.domain.enums.EdgeLabel
import za.ac.sun.plume.domain.enums.VertexBaseTrait
import za.ac.sun.plume.domain.enums.VertexLabel
import za.ac.sun.plume.domain.models.PlumeVertex
import za.ac.sun.plume.domain.models.vertices.*
import za.ac.sun.plume.drivers.DriverFactory
import za.ac.sun.plume.drivers.GraphDatabase
import za.ac.sun.plume.drivers.TinkerGraphDriver
import java.util.*

class ModelTest {
    companion object {
        val driver = (DriverFactory.invoke(GraphDatabase.TINKER_GRAPH) as TinkerGraphDriver).apply { connect() }

        @JvmStatic
        @AfterAll
        fun tearDownAll() {
            driver.close()
        }
    }

    @Nested
    @DisplayName("Domain model to string tests")
    inner class DomainModelToStringTests {
        @Test
        fun arrayInitializerVertexToString() {
            val vertex = TestDomainResources.vertices.first { it is ArrayInitializerVertex }
            assertEquals("ArrayInitializerVertex(order=$INT_1)", vertex.toString())
        }

        @Test
        fun bindingVertexToString() {
            val vertex = TestDomainResources.vertices.first { it is BindingVertex }
            assertEquals("BindingVertex(name='$STRING_1', signature='$STRING_2')", vertex.toString())
        }

        @Test
        fun blockVertexToString() {
            val vertex = TestDomainResources.vertices.first { it is BlockVertex }
            assertEquals("BlockVertex(typeFullName='$STRING_1')", vertex.toString())
        }

        @Test
        fun callVertexToString() {
            val vertex = TestDomainResources.vertices.first { it is CallVertex }
            assertEquals("CallVertex(methodFullName='$STRING_1', argumentIndex=$INT_1, dispatchType=$DISPATCH_1, typeFullName='$STRING_1', dynamicTypeHintFullName='$STRING_1')", vertex.toString())
        }

        @Test
        fun jumpTargetVertexToString() {
            val vertex = TestDomainResources.vertices.first { it is JumpTargetVertex }
            assertEquals("JumpTargetVertex(name='$STRING_1', argumentIndex=$INT_1)", vertex.toString())
        }

        @Test
        fun controlStructureVertexToString() {
            val vertex = TestDomainResources.vertices.first { it is ControlStructureVertex }
            assertEquals("ControlStructureVertex(name='$STRING_1', order=$INT_1)", vertex.toString())
        }

        @Test
        fun fieldIdentifierVertexToString() {
            val vertex = TestDomainResources.vertices.first { it is FieldIdentifierVertex }
            assertEquals("FieldIdentifierVertex(canonicalName='$STRING_1', order=$INT_1)", vertex.toString())
        }

        @Test
        fun fileVertexToString() {
            val vertex = TestDomainResources.vertices.first { it is FileVertex }
            assertEquals("FileVertex(name='$STRING_1', order=$INT_1, hash='$STRING_2')", vertex.toString())
        }

        @Test
        fun identifierVertexToString() {
            val vertex = TestDomainResources.vertices.first { it is IdentifierVertex }
            assertEquals("IdentifierVertex(name='$STRING_1', typeFullName='$STRING_1')", vertex.toString())
        }

        @Test
        fun literalVertexToString() {
            val vertex = TestDomainResources.vertices.first { it is LiteralVertex }
            assertEquals("LiteralVertex(code='$STRING_1', typeFullName='$STRING_1')", vertex.toString())
        }

        @Test
        fun localVertexToString() {
            val vertex = TestDomainResources.vertices.first { it is LocalVertex }
            assertEquals("LocalVertex(code='$STRING_1', typeFullName='$STRING_1', lineNumber=$INT_1)", vertex.toString())
        }

        @Test
        fun memberVertexToString() {
            val vertex = TestDomainResources.vertices.first { it is MemberVertex }
            assertEquals("MemberVertex(code='$STRING_1', typeFullName='$STRING_1')", vertex.toString())
        }

        @Test
        fun metaDataVertexToString() {
            val vertex = TestDomainResources.vertices.first { it is MetaDataVertex }
            assertEquals("MetaDataVertex(language='$STRING_1', version='$STRING_1')", vertex.toString())
        }

        @Test
        fun methodParameterInVertexToString() {
            val vertex = TestDomainResources.vertices.first { it is MethodParameterInVertex }
            assertEquals("MethodParameterInVertex(code='$STRING_1', evaluationStrategy=$EVAL_1, typeFullName='$STRING_1', lineNumber=$INT_1)", vertex.toString())
        }

        @Test
        fun methodRefVertexToString() {
            val vertex = TestDomainResources.vertices.first { it is MethodRefVertex }
            assertEquals("MethodRefVertex(methodInstFullName='$STRING_1', methodFullName='$STRING_1')", vertex.toString())
        }

        @Test
        fun methodReturnVertexToString() {
            val vertex = TestDomainResources.vertices.first { it is MethodReturnVertex }
            assertEquals("MethodReturnVertex(typeFullName='$STRING_1', evaluationStrategy=$EVAL_1)", vertex.toString())
        }

        @Test
        fun methodVertexToString() {
            val vertex = TestDomainResources.vertices.first { it is MethodVertex }
            assertEquals("MethodVertex(name='$STRING_1', fullName='$STRING_1', signature='$STRING_1')", vertex.toString())
        }

        @Test
        fun modifierVertexToString() {
            val vertex = TestDomainResources.vertices.first { it is ModifierVertex }
            assertEquals("ModifierVertex(name=$MOD_1, order=$INT_1)", vertex.toString())
        }

        @Test
        fun namespaceBlockVertexToString() {
            val vertex = TestDomainResources.vertices.first { it is NamespaceBlockVertex }
            assertEquals("NamespaceBlockVertex(name='$STRING_1', fullName='$STRING_1')", vertex.toString())
        }

        @Test
        fun returnVertexToString() {
            val vertex = TestDomainResources.vertices.first { it is ReturnVertex }
            assertEquals("ReturnVertex(lineNumber=$INT_1, order=$INT_1, argumentIndex=$INT_1, code='$STRING_1')", vertex.toString())
        }

        @Test
        fun typeArgumentVertexToString() {
            val vertex = TestDomainResources.vertices.first { it is TypeArgumentVertex }
            assertEquals("TypeArgumentVertex(order=$INT_1)", vertex.toString())
        }

        @Test
        fun typeDeclVertexToString() {
            val vertex = TestDomainResources.vertices.first { it is TypeDeclVertex }
            assertEquals("TypeDeclVertex(name='$STRING_1', fullName='$STRING_1', typeDeclFullName='$STRING_1')", vertex.toString())
        }

        @Test
        fun typeParameterVertexToString() {
            val vertex = TestDomainResources.vertices.first { it is TypeParameterVertex }
            assertEquals("TypeParameterVertex(name='$STRING_1', order=$INT_1)", vertex.toString())
        }

        @Test
        fun typeVertexToString() {
            val vertex = TestDomainResources.vertices.first { it is TypeVertex }
            assertEquals("TypeVertex(name='$STRING_1', fullName='$STRING_1', typeDeclFullName='$STRING_1')", vertex.toString())
        }

        @Test
        fun unknownVertexToString() {
            val vertex = TestDomainResources.vertices.first { it is UnknownVertex }
            assertEquals("UnknownVertex(order=$INT_1, typeFullName='$STRING_1')", vertex.toString())
        }
    }

    @Nested
    @DisplayName("Domain model equal tests")
    inner class DomainModelEqualTests {

        private fun assertVertexEquality(vertex1: PlumeVertex, vertex2: PlumeVertex) {
            assertEquals(vertex1, vertex1)
            assertEquals(vertex1, vertex2)
            assertEquals(vertex1.hashCode(), vertex2.hashCode())
        }

        private fun assertVertexInequality(vertex1: PlumeVertex, vertex2: PlumeVertex) {
            assertNotEquals(vertex1, vertex2)
            assertNotEquals(vertex1.hashCode(), vertex2.hashCode())
            assertNotEquals(vertex1, STRING_1)
        }

        @Test
        fun arrayInitializerVertexEquality() {
            val vertex1 = ArrayInitializerVertex(INT_1)
            val vertex2 = ArrayInitializerVertex(INT_1)
            val vertex3 = ArrayInitializerVertex(INT_2)
            assertVertexEquality(vertex1, vertex2)
            assertVertexInequality(vertex1, vertex3)
        }

        @Test
        fun bindingVertexEquality() {
            val vertex1 = BindingVertex(STRING_1, STRING_1)
            val vertex2 = BindingVertex(STRING_1, STRING_1)
            val vertex3 = BindingVertex(STRING_1, STRING_2)
            assertVertexEquality(vertex1, vertex2)
            assertVertexInequality(vertex1, vertex3)
        }

        @Test
        fun blockVertexEquality() {
            val vertex1 = BlockVertex(STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_1)
            val vertex2 = BlockVertex(STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_1)
            val vertex3 = BlockVertex(STRING_2, STRING_1, INT_1, INT_1, INT_1, INT_1)
            val vertex4 = BlockVertex(STRING_1, STRING_2, INT_1, INT_1, INT_1, INT_1)
            val vertex5 = BlockVertex(STRING_1, STRING_1, INT_2, INT_1, INT_1, INT_1)
            val vertex6 = BlockVertex(STRING_1, STRING_1, INT_1, INT_2, INT_1, INT_1)
            val vertex7 = BlockVertex(STRING_1, STRING_1, INT_1, INT_1, INT_2, INT_1)
            assertVertexEquality(vertex1, vertex2)
            assertVertexInequality(vertex1, vertex3)
            assertVertexInequality(vertex1, vertex4)
            assertVertexInequality(vertex1, vertex5)
            assertVertexInequality(vertex1, vertex6)
            assertVertexInequality(vertex1, vertex7)
        }

        @Test
        fun callVertexEquality() {
            val vertex1 = CallVertex(STRING_1, INT_1, DISPATCH_1, STRING_1, STRING_1, STRING_1, STRING_1, STRING_1, INT_1, INT_1, INT_1)
            val vertex2 = CallVertex(STRING_1, INT_1, DISPATCH_1, STRING_1, STRING_1, STRING_1, STRING_1, STRING_1, INT_1, INT_1, INT_1)
            val vertex3 = CallVertex(STRING_2, INT_1, DISPATCH_1, STRING_1, STRING_1, STRING_1, STRING_1, STRING_1, INT_1, INT_1, INT_1)
            val vertex4 = CallVertex(STRING_1, INT_2, DISPATCH_1, STRING_1, STRING_1, STRING_1, STRING_1, STRING_1, INT_1, INT_1, INT_1)
            val vertex5 = CallVertex(STRING_1, INT_1, DISPATCH_2, STRING_1, STRING_1, STRING_1, STRING_1, STRING_1, INT_1, INT_1, INT_1)
            val vertex6 = CallVertex(STRING_1, INT_1, DISPATCH_1, STRING_2, STRING_1, STRING_1, STRING_1, STRING_1, INT_1, INT_1, INT_1)
            val vertex7 = CallVertex(STRING_1, INT_1, DISPATCH_1, STRING_1, STRING_2, STRING_1, STRING_1, STRING_1, INT_1, INT_1, INT_1)
            val vertex8 = CallVertex(STRING_1, INT_1, DISPATCH_1, STRING_1, STRING_1, STRING_2, STRING_1, STRING_1, INT_1, INT_1, INT_1)
            val vertex9 = CallVertex(STRING_1, INT_1, DISPATCH_1, STRING_1, STRING_1, STRING_1, STRING_2, STRING_1, INT_1, INT_1, INT_1)
            val vertex10 = CallVertex(STRING_1, INT_1, DISPATCH_1, STRING_1, STRING_1, STRING_1, STRING_1, STRING_2, INT_1, INT_1, INT_1)
            val vertex11 = CallVertex(STRING_1, INT_1, DISPATCH_1, STRING_1, STRING_1, STRING_1, STRING_1, STRING_1, INT_2, INT_1, INT_1)
            val vertex12 = CallVertex(STRING_1, INT_1, DISPATCH_1, STRING_1, STRING_1, STRING_1, STRING_1, STRING_1, INT_1, INT_2, INT_1)
            val vertex13 = CallVertex(STRING_1, INT_1, DISPATCH_1, STRING_1, STRING_1, STRING_1, STRING_1, STRING_1, INT_1, INT_1, INT_2)
            assertVertexEquality(vertex1, vertex2)
            assertVertexInequality(vertex1, vertex3)
            assertVertexInequality(vertex1, vertex4)
            assertVertexInequality(vertex1, vertex5)
            assertVertexInequality(vertex1, vertex6)
            assertVertexInequality(vertex1, vertex7)
            assertVertexInequality(vertex1, vertex8)
            assertVertexInequality(vertex1, vertex9)
            assertVertexInequality(vertex1, vertex10)
            assertVertexInequality(vertex1, vertex11)
            assertVertexInequality(vertex1, vertex12)
            assertVertexInequality(vertex1, vertex13)
        }

        @Test
        fun controlStructureVertexEquality() {
            val vertex1 = ControlStructureVertex(STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_1)
            val vertex2 = ControlStructureVertex(STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_1)
            val vertex3 = ControlStructureVertex(STRING_1, STRING_2, INT_1, INT_1, INT_1, INT_1)
            val vertex4 = ControlStructureVertex(STRING_1, STRING_1, INT_2, INT_1, INT_1, INT_1)
            val vertex5 = ControlStructureVertex(STRING_1, STRING_1, INT_1, INT_2, INT_1, INT_1)
            val vertex6 = ControlStructureVertex(STRING_1, STRING_1, INT_1, INT_1, INT_2, INT_1)
            val vertex7 = ControlStructureVertex(STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_2)
            assertVertexEquality(vertex1, vertex2)
            assertVertexInequality(vertex1, vertex3)
            assertVertexInequality(vertex1, vertex4)
            assertVertexInequality(vertex1, vertex5)
            assertVertexInequality(vertex1, vertex6)
            assertVertexInequality(vertex1, vertex7)
        }

        @Test
        fun fieldIdentifierVertexEquality() {
            val vertex1 = FieldIdentifierVertex(STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_1)
            val vertex2 = FieldIdentifierVertex(STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_1)
            val vertex3 = FieldIdentifierVertex(STRING_2, STRING_1, INT_1, INT_1, INT_1, INT_1)
            val vertex4 = FieldIdentifierVertex(STRING_1, STRING_2, INT_1, INT_1, INT_1, INT_1)
            val vertex5 = FieldIdentifierVertex(STRING_1, STRING_1, INT_2, INT_1, INT_1, INT_1)
            val vertex6 = FieldIdentifierVertex(STRING_1, STRING_1, INT_1, INT_2, INT_1, INT_1)
            val vertex7 = FieldIdentifierVertex(STRING_1, STRING_1, INT_1, INT_1, INT_2, INT_1)
            val vertex8 = FieldIdentifierVertex(STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_2)
            assertVertexEquality(vertex1, vertex2)
            assertVertexInequality(vertex1, vertex3)
            assertVertexInequality(vertex1, vertex4)
            assertVertexInequality(vertex1, vertex5)
            assertVertexInequality(vertex1, vertex6)
            assertVertexInequality(vertex1, vertex7)
            assertVertexInequality(vertex1, vertex8)
        }

        @Test
        fun fileVertexEquality() {
            val vertex1 = FileVertex(STRING_1, STRING_1, INT_1)
            val vertex2 = FileVertex(STRING_1, STRING_1, INT_1)
            val vertex3 = FileVertex(STRING_2, STRING_1, INT_1)
            val vertex4 = FileVertex(STRING_1, STRING_2, INT_1)
            val vertex5 = FileVertex(STRING_1, STRING_1, INT_2)
            assertVertexEquality(vertex1, vertex2)
            assertVertexInequality(vertex1, vertex3)
            assertVertexInequality(vertex1, vertex4)
            assertVertexEquality(vertex1, vertex5)
        }

        @Test
        fun identifierVertexEquality() {
            val vertex1 = IdentifierVertex(STRING_1, STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_1)
            val vertex2 = IdentifierVertex(STRING_1, STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_1)
            val vertex3 = IdentifierVertex(STRING_2, STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_1)
            val vertex4 = IdentifierVertex(STRING_1, STRING_2, STRING_1, INT_1, INT_1, INT_1, INT_1)
            val vertex5 = IdentifierVertex(STRING_1, STRING_1, STRING_2, INT_1, INT_1, INT_1, INT_1)
            val vertex6 = IdentifierVertex(STRING_1, STRING_1, STRING_1, INT_2, INT_1, INT_1, INT_1)
            val vertex7 = IdentifierVertex(STRING_1, STRING_1, STRING_1, INT_1, INT_2, INT_1, INT_1)
            val vertex8 = IdentifierVertex(STRING_1, STRING_1, STRING_1, INT_1, INT_1, INT_2, INT_1)
            val vertex9 = IdentifierVertex(STRING_1, STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_2)
            assertVertexEquality(vertex1, vertex2)
            assertVertexInequality(vertex1, vertex3)
            assertVertexInequality(vertex1, vertex4)
            assertVertexInequality(vertex1, vertex5)
            assertVertexInequality(vertex1, vertex6)
            assertVertexInequality(vertex1, vertex7)
            assertVertexInequality(vertex1, vertex8)
            assertVertexInequality(vertex1, vertex9)
        }

        @Test
        fun jumpTargetVertexEquality() {
            val vertex1 = JumpTargetVertex(STRING_1, INT_1, INT_1, INT_1, STRING_1, INT_1)
            val vertex2 = JumpTargetVertex(STRING_1, INT_1, INT_1, INT_1, STRING_1, INT_1)
            val vertex3 = JumpTargetVertex(STRING_2, INT_1, INT_1, INT_1, STRING_1, INT_1)
            val vertex4 = JumpTargetVertex(STRING_1, INT_2, INT_1, INT_1, STRING_1, INT_1)
            val vertex5 = JumpTargetVertex(STRING_1, INT_1, INT_2, INT_1, STRING_1, INT_1)
            val vertex6 = JumpTargetVertex(STRING_1, INT_1, INT_1, INT_2, STRING_1, INT_1)
            val vertex7 = JumpTargetVertex(STRING_1, INT_1, INT_1, INT_1, STRING_2, INT_1)
            val vertex8 = JumpTargetVertex(STRING_1, INT_1, INT_1, INT_1, STRING_1, INT_2)
            assertVertexEquality(vertex1, vertex2)
            assertVertexInequality(vertex1, vertex3)
            assertVertexInequality(vertex1, vertex4)
            assertVertexInequality(vertex1, vertex5)
            assertVertexInequality(vertex1, vertex6)
            assertVertexInequality(vertex1, vertex7)
            assertVertexInequality(vertex1, vertex8)
        }

        @Test
        fun literalVertexEquality() {
            val vertex1 = LiteralVertex(STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_1)
            val vertex2 = LiteralVertex(STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_1)
            val vertex4 = LiteralVertex(STRING_2, STRING_1, INT_1, INT_1, INT_1, INT_1)
            val vertex5 = LiteralVertex(STRING_1, STRING_2, INT_1, INT_1, INT_1, INT_1)
            val vertex6 = LiteralVertex(STRING_1, STRING_1, INT_2, INT_1, INT_1, INT_1)
            val vertex7 = LiteralVertex(STRING_1, STRING_1, INT_1, INT_2, INT_1, INT_1)
            val vertex8 = LiteralVertex(STRING_1, STRING_1, INT_1, INT_1, INT_2, INT_1)
            val vertex9 = LiteralVertex(STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_2)
            assertVertexEquality(vertex1, vertex2)
            assertVertexInequality(vertex1, vertex4)
            assertVertexInequality(vertex1, vertex5)
            assertVertexInequality(vertex1, vertex6)
            assertVertexInequality(vertex1, vertex7)
            assertVertexInequality(vertex1, vertex8)
            assertVertexInequality(vertex1, vertex9)
        }

        @Test
        fun localVertexEquality() {
            val vertex1 = LocalVertex(STRING_1, STRING_1, INT_1, INT_1, STRING_1, INT_1)
            val vertex2 = LocalVertex(STRING_1, STRING_1, INT_1, INT_1, STRING_1, INT_1)
            val vertex3 = LocalVertex(STRING_2, STRING_1, INT_1, INT_1, STRING_1, INT_1)
            val vertex4 = LocalVertex(STRING_1, STRING_2, INT_1, INT_1, STRING_1, INT_1)
            val vertex5 = LocalVertex(STRING_1, STRING_1, INT_2, INT_1, STRING_1, INT_1)
            val vertex6 = LocalVertex(STRING_1, STRING_1, INT_1, INT_2, STRING_1, INT_1)
            val vertex7 = LocalVertex(STRING_1, STRING_1, INT_1, INT_1, STRING_2, INT_1)
            val vertex8 = LocalVertex(STRING_1, STRING_1, INT_1, INT_1, STRING_1, INT_2)
            assertVertexEquality(vertex1, vertex2)
            assertVertexInequality(vertex1, vertex3)
            assertVertexInequality(vertex1, vertex4)
            assertVertexInequality(vertex1, vertex5)
            assertVertexInequality(vertex1, vertex6)
            assertVertexInequality(vertex1, vertex7)
            assertVertexInequality(vertex1, vertex8)
        }

        @Test
        fun memberVertexEquality() {
            val vertex1 = MemberVertex(STRING_1, STRING_1, STRING_1, INT_1)
            val vertex2 = MemberVertex(STRING_1, STRING_1, STRING_1, INT_1)
            val vertex3 = MemberVertex(STRING_2, STRING_1, STRING_1, INT_1)
            val vertex4 = MemberVertex(STRING_1, STRING_2, STRING_1, INT_1)
            val vertex5 = MemberVertex(STRING_1, STRING_1, STRING_2, INT_1)
            assertVertexEquality(vertex1, vertex2)
            assertVertexInequality(vertex1, vertex3)
            assertVertexInequality(vertex1, vertex4)
            assertVertexInequality(vertex1, vertex5)
        }

        @Test
        fun metaDataVertexEquality() {
            val vertex1 = MetaDataVertex(STRING_1, STRING_1)
            val vertex2 = MetaDataVertex(STRING_1, STRING_1)
            val vertex3 = MetaDataVertex(STRING_2, STRING_1)
            val vertex4 = MetaDataVertex(STRING_1, STRING_2)
            assertVertexEquality(vertex1, vertex2)
            assertVertexInequality(vertex1, vertex3)
            assertVertexInequality(vertex1, vertex4)
        }

        @Test
        fun methodParameterInVertexEquality() {
            val vertex1 = MethodParameterInVertex(STRING_1, EVAL_1, STRING_1, INT_1, STRING_1, INT_1)
            val vertex2 = MethodParameterInVertex(STRING_1, EVAL_1, STRING_1, INT_1, STRING_1, INT_1)
            val vertex3 = MethodParameterInVertex(STRING_2, EVAL_1, STRING_1, INT_1, STRING_1, INT_1)
            val vertex4 = MethodParameterInVertex(STRING_1, EVAL_2, STRING_1, INT_1, STRING_1, INT_1)
            val vertex5 = MethodParameterInVertex(STRING_1, EVAL_1, STRING_2, INT_1, STRING_1, INT_1)
            val vertex6 = MethodParameterInVertex(STRING_1, EVAL_1, STRING_1, INT_2, STRING_1, INT_1)
            val vertex7 = MethodParameterInVertex(STRING_1, EVAL_1, STRING_1, INT_1, STRING_2, INT_1)
            val vertex8 = MethodParameterInVertex(STRING_1, EVAL_1, STRING_1, INT_1, STRING_1, INT_2)
            assertVertexEquality(vertex1, vertex2)
            assertVertexInequality(vertex1, vertex3)
            assertVertexInequality(vertex1, vertex4)
            assertVertexInequality(vertex1, vertex5)
            assertVertexInequality(vertex1, vertex6)
            assertVertexInequality(vertex1, vertex7)
            assertVertexInequality(vertex1, vertex8)
        }

        @Test
        fun methodRefVertexEquality() {
            val vertex1 = MethodRefVertex(STRING_1, STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_1)
            val vertex2 = MethodRefVertex(STRING_1, STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_1)
            val vertex3 = MethodRefVertex(STRING_2, STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_1)
            val vertex4 = MethodRefVertex(STRING_1, STRING_2, STRING_1, INT_1, INT_1, INT_1, INT_1)
            val vertex5 = MethodRefVertex(STRING_1, STRING_1, STRING_2, INT_1, INT_1, INT_1, INT_1)
            val vertex6 = MethodRefVertex(STRING_1, STRING_1, STRING_1, INT_2, INT_1, INT_1, INT_1)
            val vertex7 = MethodRefVertex(STRING_1, STRING_1, STRING_1, INT_1, INT_2, INT_1, INT_1)
            val vertex8 = MethodRefVertex(STRING_1, STRING_1, STRING_1, INT_1, INT_1, INT_2, INT_1)
            val vertex9 = MethodRefVertex(STRING_1, STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_2)
            assertVertexEquality(vertex1, vertex2)
            assertVertexInequality(vertex1, vertex3)
            assertVertexInequality(vertex1, vertex4)
            assertVertexInequality(vertex1, vertex5)
            assertVertexInequality(vertex1, vertex6)
            assertVertexInequality(vertex1, vertex7)
            assertVertexInequality(vertex1, vertex8)
            assertVertexInequality(vertex1, vertex9)
        }

        @Test
        fun methodReturnVertexEquality() {
            val vertex1 = MethodReturnVertex(STRING_1, EVAL_1, STRING_1, INT_1, INT_1, INT_1)
            val vertex2 = MethodReturnVertex(STRING_1, EVAL_1, STRING_1, INT_1, INT_1, INT_1)
            val vertex4 = MethodReturnVertex(STRING_2, EVAL_1, STRING_1, INT_1, INT_1, INT_1)
            val vertex5 = MethodReturnVertex(STRING_1, EVAL_2, STRING_1, INT_1, INT_1, INT_1)
            val vertex6 = MethodReturnVertex(STRING_1, EVAL_1, STRING_2, INT_1, INT_1, INT_1)
            val vertex7 = MethodReturnVertex(STRING_1, EVAL_1, STRING_1, INT_2, INT_1, INT_1)
            val vertex8 = MethodReturnVertex(STRING_1, EVAL_1, STRING_1, INT_1, INT_2, INT_1)
            assertVertexEquality(vertex1, vertex2)
            assertVertexInequality(vertex1, vertex4)
            assertVertexInequality(vertex1, vertex5)
            assertVertexInequality(vertex1, vertex6)
            assertVertexInequality(vertex1, vertex7)
            assertVertexInequality(vertex1, vertex8)
        }

        @Test
        fun methodVertexEquality() {
            val vertex1 = MethodVertex(STRING_1, STRING_1, STRING_1, STRING_1, INT_1, INT_1, INT_1)
            val vertex2 = MethodVertex(STRING_1, STRING_1, STRING_1, STRING_1, INT_1, INT_1, INT_1)
            val vertex3 = MethodVertex(STRING_1, STRING_2, STRING_1, STRING_1, INT_1, INT_1, INT_1)
            val vertex4 = MethodVertex(STRING_1, STRING_1, STRING_2, STRING_1, INT_1, INT_1, INT_1)
            assertVertexEquality(vertex1, vertex2)
            assertVertexInequality(vertex1, vertex3)
            assertVertexInequality(vertex1, vertex4)
        }

        @Test
        fun modifierVertexEquality() {
            val vertex1 = ModifierVertex(MOD_1, INT_1)
            val vertex2 = ModifierVertex(MOD_1, INT_1)
            val vertex3 = ModifierVertex(MOD_2, INT_1)
            val vertex4 = ModifierVertex(MOD_1, INT_2)
            assertVertexEquality(vertex1, vertex2)
            assertVertexInequality(vertex1, vertex3)
            assertVertexInequality(vertex1, vertex4)
        }

        @Test
        fun namespaceBlockVertexEquality() {
            val vertex1 = NamespaceBlockVertex(STRING_1, STRING_1, INT_1)
            val vertex2 = NamespaceBlockVertex(STRING_1, STRING_1, INT_1)
            val vertex3 = NamespaceBlockVertex(STRING_2, STRING_1, INT_1)
            val vertex4 = NamespaceBlockVertex(STRING_1, STRING_2, INT_1)
            assertVertexEquality(vertex1, vertex2)
            assertVertexInequality(vertex1, vertex3)
            assertVertexInequality(vertex1, vertex4)
        }

        @Test
        fun returnVertexEquality() {
            val vertex1 = ReturnVertex(INT_1, INT_1, INT_1, INT_1, STRING_1)
            val vertex2 = ReturnVertex(INT_1, INT_1, INT_1, INT_1, STRING_1)
            val vertex3 = ReturnVertex(INT_2, INT_1, INT_1, INT_1, STRING_1)
            val vertex4 = ReturnVertex(INT_1, INT_2, INT_1, INT_1, STRING_1)
            val vertex5 = ReturnVertex(INT_1, INT_1, INT_2, INT_1, STRING_1)
            val vertex6 = ReturnVertex(INT_1, INT_1, INT_1, INT_2, STRING_2)
            val vertex7 = ReturnVertex(INT_1, INT_1, INT_1, INT_1, STRING_1)
            assertVertexEquality(vertex1, vertex2)
            assertVertexInequality(vertex1, vertex3)
            assertVertexInequality(vertex1, vertex4)
            assertVertexInequality(vertex1, vertex5)
            assertVertexInequality(vertex1, vertex6)
            assertVertexEquality(vertex1, vertex7)
        }

        @Test
        fun typeArgumentVertexEquality() {
            val vertex1 = TypeArgumentVertex(INT_1)
            val vertex2 = TypeArgumentVertex(INT_1)
            val vertex3 = TypeArgumentVertex(INT_2)
            assertVertexEquality(vertex1, vertex2)
            assertVertexInequality(vertex1, vertex3)
        }

        @Test
        fun typeDeclVertexEquality() {
            val vertex1 = TypeDeclVertex(STRING_1, STRING_1, STRING_1, INT_1)
            val vertex2 = TypeDeclVertex(STRING_1, STRING_1, STRING_1, INT_1)
            val vertex3 = TypeDeclVertex(STRING_2, STRING_1, STRING_1, INT_1)
            val vertex4 = TypeDeclVertex(STRING_1, STRING_2, STRING_1, INT_1)
            val vertex5 = TypeDeclVertex(STRING_1, STRING_1, STRING_2, INT_1)
            assertVertexEquality(vertex1, vertex2)
            assertVertexInequality(vertex1, vertex3)
            assertVertexInequality(vertex1, vertex4)
            assertVertexInequality(vertex1, vertex5)
        }

        @Test
        fun typeParameterVertexEquality() {
            val vertex1 = TypeParameterVertex(STRING_1, INT_1)
            val vertex2 = TypeParameterVertex(STRING_1, INT_1)
            val vertex3 = TypeParameterVertex(STRING_2, INT_1)
            assertVertexEquality(vertex1, vertex2)
            assertVertexInequality(vertex1, vertex3)
        }

        @Test
        fun typeVertexEquality() {
            val vertex1 = TypeVertex(STRING_1, STRING_1, STRING_1)
            val vertex2 = TypeVertex(STRING_1, STRING_1, STRING_1)
            val vertex3 = TypeVertex(STRING_2, STRING_1, STRING_1)
            val vertex4 = TypeVertex(STRING_1, STRING_2, STRING_1)
            val vertex5 = TypeVertex(STRING_1, STRING_1, STRING_2)
            assertVertexEquality(vertex1, vertex2)
            assertVertexInequality(vertex1, vertex3)
            assertVertexInequality(vertex1, vertex4)
            assertVertexInequality(vertex1, vertex5)
        }

        @Test
        fun typeRefVertexEquality() {
            val vertex1 = TypeRefVertex(STRING_1, STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_1)
            val vertex2 = TypeRefVertex(STRING_1, STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_1)
            val vertex3 = TypeRefVertex(STRING_2, STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_1)
            val vertex4 = TypeRefVertex(STRING_1, STRING_2, STRING_1, INT_1, INT_1, INT_1, INT_1)
            val vertex5 = TypeRefVertex(STRING_1, STRING_1, STRING_2, INT_1, INT_1, INT_1, INT_1)
            val vertex6 = TypeRefVertex(STRING_1, STRING_1, STRING_1, INT_2, INT_1, INT_1, INT_1)
            val vertex7 = TypeRefVertex(STRING_1, STRING_1, STRING_1, INT_1, INT_2, INT_1, INT_1)
            val vertex8 = TypeRefVertex(STRING_1, STRING_1, STRING_1, INT_1, INT_1, INT_2, INT_1)
            val vertex9 = TypeRefVertex(STRING_1, STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_2)
            assertVertexEquality(vertex1, vertex2)
            assertVertexInequality(vertex1, vertex3)
            assertVertexInequality(vertex1, vertex4)
            assertVertexEquality(vertex1, vertex5)
            assertVertexEquality(vertex1, vertex6)
            assertVertexEquality(vertex1, vertex7)
            assertVertexEquality(vertex1, vertex8)
            assertVertexInequality(vertex1, vertex9)
        }

        @Test
        fun unknownVertexEquality() {
            val vertex1 = UnknownVertex(STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_1)
            val vertex2 = UnknownVertex(STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_1)
            val vertex3 = UnknownVertex(STRING_2, STRING_1, INT_1, INT_1, INT_1, INT_1)
            val vertex4 = UnknownVertex(STRING_1, STRING_2, INT_1, INT_1, INT_1, INT_1)
            val vertex5 = UnknownVertex(STRING_1, STRING_1, INT_2, INT_1, INT_1, INT_1)
            val vertex6 = UnknownVertex(STRING_1, STRING_1, INT_1, INT_2, INT_1, INT_1)
            val vertex7 = UnknownVertex(STRING_1, STRING_1, INT_1, INT_1, INT_2, INT_1)
            val vertex8 = UnknownVertex(STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_2)
            assertVertexEquality(vertex1, vertex2)
            assertVertexInequality(vertex1, vertex3)
            assertVertexInequality(vertex1, vertex4)
            assertVertexInequality(vertex1, vertex5)
            assertVertexInequality(vertex1, vertex6)
            assertVertexInequality(vertex1, vertex7)
            assertVertexInequality(vertex1, vertex8)
        }
    }

    @Nested
    @DisplayName("Domain model property tests")
    inner class DomainModelPropertyTests {
        @Test
        fun arrayInitializerVertexEquality() {
            val vertex1 = ArrayInitializerVertex(INT_1)
            assertEquals(VertexLabel.ARRAY_INITIALIZER, ArrayInitializerVertex.LABEL)
            assertEquals(EnumSet.of(VertexBaseTrait.AST_NODE), ArrayInitializerVertex.TRAITS)
            assertEquals(vertex1.order, INT_1)
        }

        @Test
        fun bindingVertexEquality() {
            val vertex1 = BindingVertex(STRING_1, STRING_1)
            assertEquals(VertexLabel.BINDING, BindingVertex.LABEL)
            assertEquals(EnumSet.noneOf(VertexBaseTrait::class.java), BindingVertex.TRAITS)
            assertEquals(vertex1.name, STRING_1)
            assertEquals(vertex1.signature, STRING_1)
        }

        @Test
        fun blockVertexEquality() {
            val vertex1 = BlockVertex(STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_1)
            assertEquals(VertexLabel.BLOCK, BlockVertex.LABEL)
            assertEquals(EnumSet.of(VertexBaseTrait.EXPRESSION), BlockVertex.TRAITS)
            assertEquals(vertex1.code, STRING_1)
            assertEquals(vertex1.order, INT_1)
            assertEquals(vertex1.typeFullName, STRING_1)
            assertEquals(vertex1.lineNumber, INT_1)
            assertEquals(vertex1.columnNumber, INT_1)
            assertEquals(vertex1.argumentIndex, INT_1)
        }

        @Test
        fun callVertexEquality() {
            val vertex1 = CallVertex(STRING_1, INT_1, DISPATCH_1, STRING_1, STRING_1, STRING_1, STRING_1, STRING_1, INT_1, INT_1, INT_1)
            assertEquals(VertexLabel.CALL, CallVertex.LABEL)
            assertEquals(EnumSet.of(VertexBaseTrait.EXPRESSION, VertexBaseTrait.CALL_REPR), CallVertex.TRAITS)
            assertEquals(vertex1.name, STRING_1)
            assertEquals(vertex1.order, INT_1)
            assertEquals(vertex1.typeFullName, STRING_1)
            assertEquals(vertex1.dynamicTypeHintFullName, STRING_1)
            assertEquals(vertex1.lineNumber, INT_1)
            assertEquals(vertex1.columnNumber, INT_1)
            assertEquals(vertex1.argumentIndex, INT_1)
            assertEquals(vertex1.dispatchType, DISPATCH_1)
            assertEquals(vertex1.methodFullName, STRING_1)
            assertEquals(vertex1.signature, STRING_1)
            assertEquals(vertex1.code, STRING_1)
        }

        @Test
        fun controlStructureVertexEquality() {
            val vertex1 = ControlStructureVertex(STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_1)
            assertEquals(VertexLabel.CONTROL_STRUCTURE, ControlStructureVertex.LABEL)
            assertEquals(EnumSet.of(VertexBaseTrait.EXPRESSION), ControlStructureVertex.TRAITS)
            assertEquals(vertex1.name, STRING_1)
            assertEquals(vertex1.code, STRING_1)
            assertEquals(vertex1.order, INT_1)
            assertEquals(vertex1.lineNumber, INT_1)
            assertEquals(vertex1.columnNumber, INT_1)
            assertEquals(vertex1.argumentIndex, INT_1)
        }

        @Test
        fun fieldIdentifierVertexEquality() {
            val vertex1 = FieldIdentifierVertex(STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_1)
            assertEquals(VertexLabel.FIELD_IDENTIFIER, FieldIdentifierVertex.LABEL)
            assertEquals(EnumSet.of(VertexBaseTrait.EXPRESSION), FieldIdentifierVertex.TRAITS)
            assertEquals(vertex1.order, INT_1)
            assertEquals(vertex1.lineNumber, INT_1)
            assertEquals(vertex1.columnNumber, INT_1)
            assertEquals(vertex1.argumentIndex, INT_1)
            assertEquals(vertex1.code, STRING_1)
        }

        @Test
        fun fileVertexEquality() {
            val vertex1 = FileVertex(STRING_1, STRING_2, INT_1)
            assertEquals(VertexLabel.FILE, FileVertex.LABEL)
            assertEquals(EnumSet.of(VertexBaseTrait.AST_NODE), FileVertex.TRAITS)
            assertEquals(vertex1.name, STRING_1)
            assertEquals(vertex1.hash, STRING_2)
            assertEquals(vertex1.order, INT_1)
        }

        @Test
        fun identifierVertexEquality() {
            val vertex1 = IdentifierVertex(STRING_1, STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_1)
            assertEquals(VertexLabel.IDENTIFIER, IdentifierVertex.LABEL)
            assertEquals(EnumSet.of(VertexBaseTrait.EXPRESSION, VertexBaseTrait.LOCAL_LIKE), IdentifierVertex.TRAITS)
            assertEquals(vertex1.name, STRING_1)
            assertEquals(vertex1.order, INT_1)
            assertEquals(vertex1.typeFullName, STRING_1)
            assertEquals(vertex1.lineNumber, INT_1)
            assertEquals(vertex1.columnNumber, INT_1)
            assertEquals(vertex1.argumentIndex, INT_1)
            assertEquals(vertex1.code, STRING_1)
        }

        @Test
        fun jumpTargetVertexEquality() {
            val vertex1 = JumpTargetVertex(STRING_1, INT_1, INT_1, INT_1, STRING_1, INT_1)
            assertEquals(VertexLabel.CONTROL_STRUCTURE, ControlStructureVertex.LABEL)
            assertEquals(EnumSet.of(VertexBaseTrait.EXPRESSION), ControlStructureVertex.TRAITS)
            assertEquals(vertex1.name, STRING_1)
            assertEquals(vertex1.code, STRING_1)
            assertEquals(vertex1.order, INT_1)
            assertEquals(vertex1.lineNumber, INT_1)
            assertEquals(vertex1.columnNumber, INT_1)
            assertEquals(vertex1.argumentIndex, INT_1)
        }

        @Test
        fun literalVertexEquality() {
            val vertex1 = LiteralVertex(STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_1)
            assertEquals(VertexLabel.LITERAL, LiteralVertex.LABEL)
            assertEquals(EnumSet.of(VertexBaseTrait.EXPRESSION), LiteralVertex.TRAITS)
            assertEquals(vertex1.code, STRING_1)
            assertEquals(vertex1.order, INT_1)
            assertEquals(vertex1.typeFullName, STRING_1)
            assertEquals(vertex1.lineNumber, INT_1)
            assertEquals(vertex1.columnNumber, INT_1)
            assertEquals(vertex1.argumentIndex, INT_1)
        }

        @Test
        fun localVertexEquality() {
            val vertex1 = LocalVertex(STRING_1, STRING_1, INT_1, INT_1, STRING_1, INT_1)
            assertEquals(VertexLabel.LOCAL, LocalVertex.LABEL)
            assertEquals(EnumSet.of(VertexBaseTrait.DECLARATION, VertexBaseTrait.LOCAL_LIKE, VertexBaseTrait.CALL_REPR), LocalVertex.TRAITS)
            assertEquals(vertex1.name, STRING_1)
            assertEquals(vertex1.order, INT_1)
            assertEquals(vertex1.typeFullName, STRING_1)
            assertEquals(vertex1.lineNumber, INT_1)
            assertEquals(vertex1.columnNumber, INT_1)
            assertEquals(vertex1.code, STRING_1)
        }

        @Test
        fun memberVertexEquality() {
            val vertex1 = MemberVertex(STRING_1, STRING_1, STRING_1, INT_1)
            assertEquals(VertexLabel.META_DATA, MetaDataVertex.LABEL)
            assertEquals(EnumSet.noneOf(VertexBaseTrait::class.java), MetaDataVertex.TRAITS)
            assertEquals(vertex1.name, STRING_1)
            assertEquals(vertex1.order, INT_1)
            assertEquals(vertex1.typeFullName, STRING_1)
            assertEquals(vertex1.code, STRING_1)
        }

        @Test
        fun metaDataVertexEquality() {
            val vertex1 = MetaDataVertex(STRING_1, STRING_1)
            assertEquals(VertexLabel.META_DATA, MetaDataVertex.LABEL)
            assertEquals(EnumSet.noneOf(VertexBaseTrait::class.java), MetaDataVertex.TRAITS)
            assertEquals(vertex1.language, STRING_1)
            assertEquals(vertex1.version, STRING_1)
        }

        @Test
        fun methodParameterInVertexEquality() {
            val vertex1 = MethodParameterInVertex(STRING_1, EVAL_1, STRING_1, INT_1, STRING_1, INT_1)
            assertEquals(VertexLabel.METHOD_PARAMETER_IN, MethodParameterInVertex.LABEL)
            assertEquals(EnumSet.of(VertexBaseTrait.AST_NODE, VertexBaseTrait.DECLARATION, VertexBaseTrait.CFG_NODE), MethodParameterInVertex.TRAITS)
            assertEquals(vertex1.name, STRING_1)
            assertEquals(vertex1.order, INT_1)
            assertEquals(vertex1.typeFullName, STRING_1)
            assertEquals(vertex1.lineNumber, INT_1)
            assertEquals(vertex1.code, STRING_1)
        }

        @Test
        fun methodRefVertexEquality() {
            val vertex1 = MethodRefVertex(STRING_1, STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_1)
            assertEquals(VertexLabel.METHOD_REF, MethodRefVertex.LABEL)
            assertEquals(EnumSet.of(VertexBaseTrait.EXPRESSION), MethodRefVertex.TRAITS)
            assertEquals(vertex1.order, INT_1)
            assertEquals(vertex1.lineNumber, INT_1)
            assertEquals(vertex1.columnNumber, INT_1)
            assertEquals(vertex1.argumentIndex, INT_1)
            assertEquals(vertex1.methodFullName, STRING_1)
            assertEquals(vertex1.methodInstFullName, STRING_1)
            assertEquals(vertex1.code, STRING_1)
        }

        @Test
        fun methodReturnVertexEquality() {
            val vertex1 = MethodReturnVertex(STRING_1, EVAL_1, STRING_1, INT_1, INT_1, INT_1)
            assertEquals(VertexLabel.METHOD_RETURN, MethodReturnVertex.LABEL)
            assertEquals(EnumSet.of(VertexBaseTrait.CFG_NODE, VertexBaseTrait.TRACKING_POINT), MethodReturnVertex.TRAITS)
            assertEquals(vertex1.code, STRING_1)
            assertEquals(vertex1.order, INT_1)
            assertEquals(vertex1.typeFullName, STRING_1)
            assertEquals(vertex1.lineNumber, INT_1)
            assertEquals(vertex1.columnNumber, INT_1)
        }

        @Test
        fun methodVertexEquality() {
            val vertex1 = MethodVertex(STRING_1, STRING_1, STRING_1, STRING_1, INT_1, INT_1, INT_1)
            assertEquals(VertexLabel.METHOD, MethodVertex.LABEL)
            assertEquals(EnumSet.of(VertexBaseTrait.AST_NODE, VertexBaseTrait.DECLARATION, VertexBaseTrait.CFG_NODE), MethodVertex.TRAITS)
            assertEquals(vertex1.name, STRING_1)
            assertEquals(vertex1.code, STRING_1)
            assertEquals(vertex1.order, INT_1)
            assertEquals(vertex1.lineNumber, INT_1)
            assertEquals(vertex1.columnNumber, INT_1)
            assertEquals(vertex1.signature, STRING_1)
            assertEquals(vertex1.fullName, STRING_1)
        }

        @Test
        fun modifierVertexEquality() {
            val vertex1 = ModifierVertex(MOD_1, INT_1)
            assertEquals(VertexLabel.MODIFIER, ModifierVertex.LABEL)
            assertEquals(EnumSet.of(VertexBaseTrait.AST_NODE), ModifierVertex.TRAITS)
            assertEquals(vertex1.name, MOD_1)
            assertEquals(vertex1.order, INT_1)
        }

        @Test
        fun namespaceBlockVertexEquality() {
            val vertex1 = NamespaceBlockVertex(STRING_1, STRING_1, INT_1)
            assertEquals(VertexLabel.NAMESPACE_BLOCK, NamespaceBlockVertex.LABEL)
            assertEquals(EnumSet.of(VertexBaseTrait.AST_NODE), NamespaceBlockVertex.TRAITS)
            assertEquals(vertex1.name, STRING_1)
            assertEquals(vertex1.fullName, STRING_1)
            assertEquals(vertex1.order, INT_1)
        }

        @Test
        fun returnVertexEquality() {
            val vertex1 = ReturnVertex(INT_1, INT_1, INT_1, INT_1, STRING_1)
            assertEquals(VertexLabel.RETURN, ReturnVertex.LABEL)
            assertEquals(EnumSet.of(VertexBaseTrait.EXPRESSION), ReturnVertex.TRAITS)
            assertEquals(vertex1.order, INT_1)
            assertEquals(vertex1.lineNumber, INT_1)
            assertEquals(vertex1.columnNumber, INT_1)
            assertEquals(vertex1.argumentIndex, INT_1)
            assertEquals(vertex1.code, STRING_1)
        }

        @Test
        fun typeArgumentVertexEquality() {
            val vertex1 = TypeArgumentVertex(INT_1)
            assertEquals(VertexLabel.TYPE_ARGUMENT, TypeArgumentVertex.LABEL)
            assertEquals(EnumSet.of(VertexBaseTrait.AST_NODE), TypeArgumentVertex.TRAITS)
            assertEquals(vertex1.order, INT_1)
        }

        @Test
        fun typeDeclVertexEquality() {
            val vertex1 = TypeDeclVertex(STRING_1, STRING_1, STRING_1, INT_1)
            assertEquals(VertexLabel.TYPE_DECL, TypeDeclVertex.LABEL)
            assertEquals(EnumSet.of(VertexBaseTrait.AST_NODE), TypeDeclVertex.TRAITS)
            assertEquals(vertex1.name, STRING_1)
            assertEquals(vertex1.fullName, STRING_1)
            assertEquals(vertex1.typeDeclFullName, STRING_1)
            assertEquals(vertex1.order, INT_1)
        }

        @Test
        fun typeParameterVertexEquality() {
            val vertex1 = TypeParameterVertex(STRING_1, INT_1)
            assertEquals(VertexLabel.TYPE_PARAMETER, TypeParameterVertex.LABEL)
            assertEquals(EnumSet.of(VertexBaseTrait.AST_NODE), TypeParameterVertex.TRAITS)
            assertEquals(vertex1.name, STRING_1)
            assertEquals(vertex1.order, INT_1)
        }

        @Test
        fun typeVertexEquality() {
            val vertex1 = TypeVertex(STRING_1, STRING_1, STRING_1)
            assertEquals(VertexLabel.TYPE, TypeVertex.LABEL)
            assertEquals(EnumSet.noneOf(VertexBaseTrait::class.java), TypeVertex.TRAITS)
            assertEquals(vertex1.name, STRING_1)
            assertEquals(vertex1.fullName, STRING_1)
            assertEquals(vertex1.typeDeclFullName, STRING_1)
        }

        @Test
        fun typeRefEquality() {
            val vertex1 = TypeRefVertex(STRING_1, STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_1)
            assertEquals(VertexLabel.TYPE, TypeVertex.LABEL)
            assertEquals(EnumSet.noneOf(VertexBaseTrait::class.java), TypeVertex.TRAITS)
            assertEquals(vertex1.typeFullName, STRING_1)
            assertEquals(vertex1.dynamicTypeFullName, STRING_1)
            assertEquals(vertex1.code, STRING_1)
            assertEquals(vertex1.argumentIndex, INT_1)
            assertEquals(vertex1.lineNumber, INT_1)
            assertEquals(vertex1.columnNumber, INT_1)
            assertEquals(vertex1.order, INT_1)
        }

        @Test
        fun unknownVertexEquality() {
            val vertex1 = UnknownVertex(STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_1)
            assertEquals(VertexLabel.UNKNOWN, UnknownVertex.LABEL)
            assertEquals(EnumSet.of(VertexBaseTrait.EXPRESSION), UnknownVertex.TRAITS)
            assertEquals(vertex1.order, INT_1)
            assertEquals(vertex1.typeFullName, STRING_1)
            assertEquals(vertex1.lineNumber, INT_1)
            assertEquals(vertex1.columnNumber, INT_1)
            assertEquals(vertex1.argumentIndex, INT_1)
            assertEquals(vertex1.code, STRING_1)
        }

        @Test
        fun testInterfaceDefaults() {
            assertEquals("UNKNOWN", PlumeVertex.LABEL.toString())
            assertEquals(EnumSet.noneOf(VertexBaseTrait::class.java), PlumeVertex.TRAITS)
        }
    }

    @Nested
    @DisplayName("Plume graph tests")
    inner class PlumeGraphTests {

        private val v1 = MethodVertex(STRING_1, STRING_1, STRING_2, STRING_1, INT_1, INT_2, INT_1)
        private val v2 = MethodParameterInVertex(STRING_1, EVAL_1, STRING_1, INT_1, STRING_2, INT_2)
        private val v3 = BlockVertex(STRING_1, STRING_1, INT_1, INT_2, INT_2, INT_1)
        private val v4 = CallVertex(STRING_1, INT_1, DISPATCH_1, STRING_1, STRING_1, STRING_2, STRING_2, STRING_2, INT_1, INT_1, INT_1)
        private val v5 = LocalVertex(STRING_1, STRING_2, INT_1, INT_1, STRING_1, INT_1)
        private val v6 = IdentifierVertex(STRING_1, STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_1)
        private val v7 = TypeDeclVertex(STRING_1, STRING_2, STRING_1, INT_1)
        private val v8 = LiteralVertex(STRING_2, STRING_2, INT_1, INT_1, INT_1, INT_1)
        private val v9 = ReturnVertex(INT_1, INT_1, INT_1, INT_1, STRING_1)
        private val v10 = MethodReturnVertex(STRING_1, EVAL_1, STRING_1, INT_1, INT_1, INT_1)
        private val v11 = FileVertex(STRING_1, STRING_2, INT_1)
        private val v12 = NamespaceBlockVertex(STRING_1, STRING_1, INT_1)
        private val v13 = NamespaceBlockVertex(STRING_2, STRING_2, INT_1)
        private val v14 = MetaDataVertex(STRING_1, STRING_2)

        @BeforeEach
        fun setUp() {
            // Create program data
            driver.addVertex(v14)
            driver.addEdge(v11, v12, EdgeLabel.AST)
            driver.addEdge(v12, v13, EdgeLabel.AST)
            // Create method head
            driver.addEdge(v7, v1, EdgeLabel.AST)
            driver.addEdge(v1, v11, EdgeLabel.SOURCE_FILE)
            driver.addEdge(v1, v2, EdgeLabel.AST)
            driver.addEdge(v1, v5, EdgeLabel.AST)
            driver.addEdge(v1, v3, EdgeLabel.AST)
            driver.addEdge(v1, v3, EdgeLabel.CFG)
            // Create method body
            driver.addEdge(v3, v4, EdgeLabel.AST)
            driver.addEdge(v3, v4, EdgeLabel.CFG)
            driver.addEdge(v4, v6, EdgeLabel.AST)
            driver.addEdge(v4, v8, EdgeLabel.AST)
            driver.addEdge(v4, v6, EdgeLabel.ARGUMENT)
            driver.addEdge(v4, v8, EdgeLabel.ARGUMENT)
            driver.addEdge(v3, v9, EdgeLabel.AST)
            driver.addEdge(v4, v9, EdgeLabel.CFG)
            driver.addEdge(v1, v10, EdgeLabel.AST)
            driver.addEdge(v9, v10, EdgeLabel.CFG)
            // Link dependencies
            driver.addEdge(v6, v5, EdgeLabel.REF)
        }

        @AfterEach
        fun tearDown() {
            driver.clearGraph()
        }

        @Test
        fun testEquality() {
            val otherDriver = (DriverFactory.invoke(GraphDatabase.TINKER_GRAPH) as TinkerGraphDriver).apply { connect() }
            // Create identical graph
            otherDriver.addVertex(v14)
            otherDriver.addEdge(v11, v12, EdgeLabel.AST)
            otherDriver.addEdge(v12, v13, EdgeLabel.AST)
            otherDriver.addEdge(v7, v1, EdgeLabel.AST)
            otherDriver.addEdge(v1, v11, EdgeLabel.SOURCE_FILE)
            otherDriver.addEdge(v1, v2, EdgeLabel.AST)
            otherDriver.addEdge(v1, v5, EdgeLabel.AST)
            otherDriver.addEdge(v1, v3, EdgeLabel.AST)
            otherDriver.addEdge(v1, v3, EdgeLabel.CFG)
            otherDriver.addEdge(v3, v4, EdgeLabel.AST)
            otherDriver.addEdge(v3, v4, EdgeLabel.CFG)
            otherDriver.addEdge(v4, v6, EdgeLabel.AST)
            otherDriver.addEdge(v4, v8, EdgeLabel.AST)
            otherDriver.addEdge(v4, v6, EdgeLabel.ARGUMENT)
            otherDriver.addEdge(v4, v8, EdgeLabel.ARGUMENT)
            otherDriver.addEdge(v3, v9, EdgeLabel.AST)
            otherDriver.addEdge(v4, v9, EdgeLabel.CFG)
            otherDriver.addEdge(v1, v10, EdgeLabel.AST)
            otherDriver.addEdge(v9, v10, EdgeLabel.CFG)
            otherDriver.addEdge(v6, v5, EdgeLabel.REF)

            val g1 = driver.getWholeGraph()
            val g2 = otherDriver.getWholeGraph()
            otherDriver.close()

            assertEquals(g1, g2)
            assertEquals(g1.hashCode(), g2.hashCode())
            // Add an edge to make g2 different from g1
            g2.addEdge(v11, v13, EdgeLabel.AST)
            assertNotEquals(g1, g2)
            assertNotEquals(g1.hashCode(), g2.hashCode())
            // Add a vertex to make g3 different from g1
            val g3 = driver.getWholeGraph()
            g3.addVertex(ArrayInitializerVertex(10))
            assertNotEquals(g1, g2)
            assertNotEquals(g1.hashCode(), g2.hashCode())
            // Compare a subgraph of g1
            assertNotEquals(g1, driver.getProgramStructure())
            assertNotEquals(g1.hashCode(), driver.getProgramStructure().hashCode())
            assertNotEquals(g1, "Not g1")
        }
    }
}
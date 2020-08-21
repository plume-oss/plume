package za.ac.sun.plume.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
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
import za.ac.sun.plume.domain.enums.VertexBaseTraits
import za.ac.sun.plume.domain.enums.VertexLabels
import za.ac.sun.plume.domain.models.PlumeVertex
import za.ac.sun.plume.domain.models.vertices.*
import java.util.*

class ModelTest {

    @Nested
    inner class `Domain model to string tests` {
        @Test
        fun arrayInitializerVertexToString() {
            val vertex = ArrayInitializerVertex(INT_1)
            assertEquals("ArrayInitializerVertex{order=$INT_1}", vertex.toString())
        }

        @Test
        fun bindingVertexToString() {
            val vertex = BindingVertex(STRING_1, STRING_2)
            assertEquals("BindingVertex{name='$STRING_1', signature='$STRING_2'}", vertex.toString())
        }

        @Test
        fun blockVertexToString() {
            val vertex = BlockVertex(STRING_1, INT_1, INT_1, STRING_1, INT_1)
            assertEquals("BlockVertex{name='$STRING_1', order=$INT_1, argumentIndex=$INT_1, typeFullName='$STRING_1', lineNumber=$INT_1}", vertex.toString())
        }

        @Test
        fun callVertexToString() {
            val vertex = CallVertex(STRING_1, STRING_1, INT_1, STRING_1, STRING_1, INT_1, DISPATCH_1, STRING_1, STRING_1, INT_1)
            assertEquals("CallVertex{code='$STRING_1', name='$STRING_1', order=$INT_1, methodInstFullName='$STRING_1', methodFullName='$STRING_1', argumentIndex=$INT_1, dispatchType=$DISPATCH_1, signature='$STRING_1', typeFullName='$STRING_1', lineNumber=$INT_1}", vertex.toString())
        }

        @Test
        fun controlStructureVertexToString() {
            val vertex = ControlStructureVertex(STRING_1, INT_1, INT_1, INT_1)
            assertEquals("ControlStructureVertex{name='$STRING_1', lineNumber=$INT_1, order=$INT_1, argumentIndex=$INT_1}", vertex.toString())
        }

        @Test
        fun fieldIdentifierVertexToString() {
            val vertex = FieldIdentifierVertex(STRING_1, STRING_1, INT_1, INT_1, INT_1)
            assertEquals("FieldIdentifierVertex{code='$STRING_1', canonicalName='$STRING_1', order=$INT_1, argumentIndex=$INT_1, lineNumber=$INT_1}", vertex.toString())
        }

        @Test
        fun fileVertexToString() {
            val vertex = FileVertex(STRING_1, INT_1)
            assertEquals("FileVertex{name='$STRING_1', order=$INT_1}", vertex.toString())
        }

        @Test
        fun identifierVertexToString() {
            val vertex = IdentifierVertex(STRING_1, STRING_1, INT_1, INT_1, STRING_1, INT_1)
            assertEquals("IdentifierVertex{code='$STRING_1', name='$STRING_1', order=$INT_1, argumentIndex=$INT_1, typeFullName='$STRING_1', lineNumber=$INT_1}", vertex.toString())
        }

        @Test
        fun literalVertexToString() {
            val vertex = LiteralVertex(STRING_1, INT_1, INT_1, STRING_1, INT_1)
            assertEquals("LiteralVertex{name='$STRING_1', order=$INT_1, argumentIndex=$INT_1, typeFullName='$STRING_1', lineNumber=$INT_1}", vertex.toString())
        }

        @Test
        fun localVertexToString() {
            val vertex = LocalVertex(STRING_1, STRING_1, STRING_1, INT_1, INT_1)
            assertEquals("LocalVertex{code='$STRING_1', name='$STRING_1', typeFullName='$STRING_1', lineNumber=$INT_1, order=$INT_1}", vertex.toString())
        }

        @Test
        fun memberVertexToString() {
            val vertex = MemberVertex(STRING_1, STRING_1, STRING_1, INT_1)
            assertEquals("MemberVertex{code='$STRING_1', name='$STRING_1', typeFullName='$STRING_1', order=$INT_1}", vertex.toString())
        }

        @Test
        fun metaDataVertexToString() {
            val vertex = MetaDataVertex(STRING_1, STRING_1)
            assertEquals("MetaDataVertex{language='$STRING_1', version='$STRING_1'}", vertex.toString())
        }

        @Test
        fun methodParameterInVertexToString() {
            val vertex = MethodParameterInVertex(STRING_1, STRING_1, EVAL_1, STRING_1, INT_1, INT_1)
            assertEquals("MethodParameterInVertex{code='$STRING_1', name='$STRING_1', evaluationStrategy=$EVAL_1, typeFullName='$STRING_1', lineNumber=$INT_1, order=$INT_1}", vertex.toString())
        }

        @Test
        fun methodRefVertexToString() {
            val vertex = MethodRefVertex(STRING_1, INT_1, INT_1, STRING_1, STRING_1, INT_1)
            assertEquals("MethodRefVertex{code='$STRING_1', order=$INT_1, argumentIndex=$INT_1, methodInstFullName='$STRING_1', methodFullName='$STRING_1', lineNumber=$INT_1}", vertex.toString())
        }

        @Test
        fun methodReturnVertexToString() {
            val vertex = MethodReturnVertex(STRING_1, STRING_1, EVAL_1, INT_1, INT_1)
            assertEquals("MethodReturnVertex{name='$STRING_1', evaluationStrategy=$EVAL_1, typeFullName='$STRING_1', lineNumber=$INT_1, order=$INT_1}", vertex.toString())
        }

        @Test
        fun methodVertexToString() {
            val vertex = MethodVertex(STRING_1, STRING_1, STRING_1, INT_1, INT_1)
            assertEquals("MethodVertex{name='$STRING_1', fullName='$STRING_1', signature='$STRING_1', lineNumber=$INT_1, order=$INT_1}", vertex.toString())
        }

        @Test
        fun modifierVertexToString() {
            val vertex = ModifierVertex(MOD_1, INT_1)
            assertEquals("ModifierVertex{name=$MOD_1, order=$INT_1}", vertex.toString())
        }

        @Test
        fun namespaceBlockVertexToString() {
            val vertex = NamespaceBlockVertex(STRING_1, STRING_1, INT_1)
            assertEquals("NamespaceBlockVertex{name='$STRING_1', fullName='$STRING_1', order=$INT_1}", vertex.toString())
        }

        @Test
        fun returnVertexToString() {
            val vertex = ReturnVertex(INT_1, INT_1, INT_1, STRING_1)
            assertEquals("ReturnVertex{lineNumber=$INT_1, order=$INT_1, argumentIndex=$INT_1, code='$STRING_1'}", vertex.toString())
        }

        @Test
        fun typeArgumentVertexToString() {
            val vertex = TypeArgumentVertex(INT_1)
            assertEquals("TypeArgumentVertex{order=$INT_1}", vertex.toString())
        }

        @Test
        fun typeDeclVertexToString() {
            val vertex = TypeDeclVertex(STRING_1, STRING_1, STRING_1)
            assertEquals("TypeDeclVertex{name='$STRING_1', fullName='$STRING_1', typeDeclFullName='$STRING_1'}", vertex.toString())
        }

        @Test
        fun typeParameterVertexToString() {
            val vertex = TypeParameterVertex(STRING_1, INT_1)
            assertEquals("TypeParameterVertex{name='$STRING_1', order=$INT_1}", vertex.toString())
        }

        @Test
        fun typeVertexToString() {
            val vertex = TypeVertex(STRING_1, STRING_1, STRING_1)
            assertEquals("TypeVertex{name='$STRING_1', fullName='$STRING_1', typeDeclFullName='$STRING_1'}", vertex.toString())
        }

        @Test
        fun unknownVertexToString() {
            val vertex = UnknownVertex(STRING_1, INT_1, INT_1, INT_1, STRING_1)
            assertEquals("UnknownVertex{code='$STRING_1', order=$INT_1, argumentIndex=$INT_1, lineNumber=$INT_1, typeFullName='$STRING_1'}", vertex.toString())
        }
    }

    @Nested
    inner class `Domain model equal tests` {

        private fun assertVertexEquality(vertex1: PlumeVertex, vertex2: PlumeVertex, vertex3: PlumeVertex) {
            assertEquals(vertex1, vertex1)
            assertEquals(vertex1, vertex2)
            assertEquals(vertex1.hashCode(), vertex2.hashCode())
            assertNotEquals(vertex1, vertex3)
            assertNotEquals(vertex1.hashCode(), vertex3.hashCode())
            assertNotEquals(vertex1, STRING_1)
        }

        @Test
        fun arrayInitializerVertexEquality() {
            val vertex1 = ArrayInitializerVertex(INT_1)
            val vertex2 = ArrayInitializerVertex(INT_1)
            val vertex3 = ArrayInitializerVertex(INT_2)
            assertVertexEquality(vertex1, vertex2, vertex3)
        }

        @Test
        fun bindingVertexEquality() {
            val vertex1 = BindingVertex(STRING_1, STRING_1)
            val vertex2 = BindingVertex(STRING_1, STRING_1)
            val vertex3 = BindingVertex(STRING_1, STRING_2)
            assertVertexEquality(vertex1, vertex2, vertex3)
        }

        @Test
        fun blockVertexEquality() {
            val vertex1 = BlockVertex(STRING_1, INT_1, INT_1, STRING_1, INT_1)
            val vertex2 = BlockVertex(STRING_1, INT_1, INT_1, STRING_1, INT_1)
            val vertex3 = BlockVertex(STRING_2, INT_1, INT_1, STRING_1, INT_1)
            val vertex4 = BlockVertex(STRING_1, INT_2, INT_1, STRING_1, INT_1)
            val vertex5 = BlockVertex(STRING_1, INT_1, INT_2, STRING_1, INT_1)
            val vertex6 = BlockVertex(STRING_1, INT_1, INT_1, STRING_2, INT_1)
            val vertex7 = BlockVertex(STRING_1, INT_1, INT_1, STRING_1, INT_2)
            assertVertexEquality(vertex1, vertex2, vertex3)
            assertVertexEquality(vertex1, vertex2, vertex3)
            assertVertexEquality(vertex1, vertex2, vertex4)
            assertVertexEquality(vertex1, vertex2, vertex5)
            assertVertexEquality(vertex1, vertex2, vertex6)
            assertVertexEquality(vertex1, vertex2, vertex7)
        }

        @Test
        fun callVertexEquality() {
            val vertex1 = CallVertex(STRING_1, STRING_1, INT_1, STRING_1, STRING_1, INT_1, DISPATCH_1, STRING_1, STRING_1, INT_1)
            val vertex2 = CallVertex(STRING_1, STRING_1, INT_1, STRING_1, STRING_1, INT_1, DISPATCH_1, STRING_1, STRING_1, INT_1)
            val vertex3 = CallVertex(STRING_2, STRING_1, INT_1, STRING_1, STRING_1, INT_1, DISPATCH_1, STRING_1, STRING_1, INT_1)
            val vertex4 = CallVertex(STRING_1, STRING_2, INT_1, STRING_1, STRING_1, INT_1, DISPATCH_1, STRING_1, STRING_1, INT_1)
            val vertex5 = CallVertex(STRING_1, STRING_1, INT_2, STRING_1, STRING_1, INT_1, DISPATCH_1, STRING_1, STRING_1, INT_1)
            val vertex6 = CallVertex(STRING_1, STRING_1, INT_1, STRING_2, STRING_1, INT_1, DISPATCH_1, STRING_1, STRING_1, INT_1)
            val vertex7 = CallVertex(STRING_1, STRING_1, INT_1, STRING_1, STRING_2, INT_1, DISPATCH_1, STRING_1, STRING_1, INT_1)
            val vertex8 = CallVertex(STRING_1, STRING_1, INT_1, STRING_1, STRING_1, INT_2, DISPATCH_1, STRING_1, STRING_1, INT_1)
            val vertex9 = CallVertex(STRING_1, STRING_1, INT_1, STRING_1, STRING_1, INT_1, DISPATCH_2, STRING_1, STRING_1, INT_1)
            val vertex10 = CallVertex(STRING_1, STRING_1, INT_1, STRING_1, STRING_1, INT_1, DISPATCH_1, STRING_2, STRING_1, INT_1)
            val vertex11 = CallVertex(STRING_1, STRING_1, INT_1, STRING_1, STRING_1, INT_1, DISPATCH_1, STRING_1, STRING_2, INT_1)
            val vertex12 = CallVertex(STRING_1, STRING_1, INT_1, STRING_1, STRING_1, INT_1, DISPATCH_1, STRING_1, STRING_1, INT_2)
            assertVertexEquality(vertex1, vertex2, vertex3)
            assertVertexEquality(vertex1, vertex2, vertex3)
            assertVertexEquality(vertex1, vertex2, vertex4)
            assertVertexEquality(vertex1, vertex2, vertex5)
            assertVertexEquality(vertex1, vertex2, vertex6)
            assertVertexEquality(vertex1, vertex2, vertex7)
            assertVertexEquality(vertex1, vertex2, vertex8)
            assertVertexEquality(vertex1, vertex2, vertex9)
            assertVertexEquality(vertex1, vertex2, vertex10)
            assertVertexEquality(vertex1, vertex2, vertex11)
            assertVertexEquality(vertex1, vertex2, vertex12)
        }

        @Test
        fun controlStructureVertexEquality() {
            val vertex1 = ControlStructureVertex(STRING_1, INT_1, INT_1, INT_1)
            val vertex2 = ControlStructureVertex(STRING_1, INT_1, INT_1, INT_1)
            val vertex3 = ControlStructureVertex(STRING_2, INT_1, INT_1, INT_1)
            val vertex4 = ControlStructureVertex(STRING_1, INT_2, INT_1, INT_1)
            val vertex5 = ControlStructureVertex(STRING_1, INT_1, INT_2, INT_1)
            val vertex6 = ControlStructureVertex(STRING_1, INT_1, INT_1, INT_2)
            assertVertexEquality(vertex1, vertex2, vertex3)
            assertVertexEquality(vertex1, vertex2, vertex4)
            assertVertexEquality(vertex1, vertex2, vertex5)
            assertVertexEquality(vertex1, vertex2, vertex6)
        }

        @Test
        fun fieldIdentifierVertexEquality() {
            val vertex1 = FieldIdentifierVertex(STRING_1, STRING_1, INT_1, INT_1, INT_1)
            val vertex2 = FieldIdentifierVertex(STRING_1, STRING_1, INT_1, INT_1, INT_1)
            val vertex3 = FieldIdentifierVertex(STRING_2, STRING_1, INT_1, INT_1, INT_1)
            val vertex4 = FieldIdentifierVertex(STRING_1, STRING_2, INT_1, INT_1, INT_1)
            val vertex5 = FieldIdentifierVertex(STRING_1, STRING_1, INT_2, INT_1, INT_1)
            val vertex6 = FieldIdentifierVertex(STRING_1, STRING_1, INT_1, INT_2, INT_1)
            val vertex7 = FieldIdentifierVertex(STRING_1, STRING_1, INT_1, INT_1, INT_2)
            assertVertexEquality(vertex1, vertex2, vertex3)
            assertVertexEquality(vertex1, vertex2, vertex4)
            assertVertexEquality(vertex1, vertex2, vertex5)
            assertVertexEquality(vertex1, vertex2, vertex6)
            assertVertexEquality(vertex1, vertex2, vertex7)
        }

        @Test
        fun fileVertexEquality() {
            val vertex1 = FileVertex(STRING_1, INT_1)
            val vertex2 = FileVertex(STRING_1, INT_1)
            val vertex3 = FileVertex(STRING_2, INT_1)
            val vertex4 = FileVertex(STRING_1, INT_2)
            assertVertexEquality(vertex1, vertex2, vertex3)
            assertVertexEquality(vertex1, vertex2, vertex4)
        }

        @Test
        fun identifierVertexEquality() {
            val vertex1 = IdentifierVertex(STRING_1, STRING_1, INT_1, INT_1, STRING_1, INT_1)
            val vertex2 = IdentifierVertex(STRING_1, STRING_1, INT_1, INT_1, STRING_1, INT_1)
            val vertex3 = IdentifierVertex(STRING_2, STRING_1, INT_1, INT_1, STRING_1, INT_1)
            val vertex4 = IdentifierVertex(STRING_1, STRING_1, INT_2, INT_1, STRING_1, INT_1)
            val vertex5 = IdentifierVertex(STRING_1, STRING_1, INT_1, INT_2, STRING_1, INT_1)
            val vertex6 = IdentifierVertex(STRING_1, STRING_1, INT_1, INT_1, STRING_2, INT_1)
            val vertex7 = IdentifierVertex(STRING_1, STRING_1, INT_1, INT_1, STRING_1, INT_2)
            assertVertexEquality(vertex1, vertex2, vertex3)
            assertVertexEquality(vertex1, vertex2, vertex4)
            assertVertexEquality(vertex1, vertex2, vertex5)
            assertVertexEquality(vertex1, vertex2, vertex6)
            assertVertexEquality(vertex1, vertex2, vertex7)
        }

        @Test
        fun literalVertexEquality() {
            val vertex1 = LiteralVertex(STRING_1, INT_1, INT_1, STRING_1, INT_1)
            val vertex2 = LiteralVertex(STRING_1, INT_1, INT_1, STRING_1, INT_1)
            val vertex3 = LiteralVertex(STRING_2, INT_1, INT_1, STRING_1, INT_1)
            val vertex4 = LiteralVertex(STRING_1, INT_2, INT_1, STRING_1, INT_1)
            val vertex5 = LiteralVertex(STRING_1, INT_1, INT_2, STRING_1, INT_1)
            val vertex6 = LiteralVertex(STRING_1, INT_1, INT_1, STRING_2, INT_1)
            val vertex7 = LiteralVertex(STRING_1, INT_1, INT_1, STRING_1, INT_2)
            assertVertexEquality(vertex1, vertex2, vertex3)
            assertVertexEquality(vertex1, vertex2, vertex4)
            assertVertexEquality(vertex1, vertex2, vertex5)
            assertVertexEquality(vertex1, vertex2, vertex6)
            assertVertexEquality(vertex1, vertex2, vertex7)
        }

        @Test
        fun localVertexEquality() {
            val vertex1 = LocalVertex(STRING_1, STRING_1, STRING_1, INT_1, INT_1)
            val vertex2 = LocalVertex(STRING_1, STRING_1, STRING_1, INT_1, INT_1)
            val vertex3 = LocalVertex(STRING_2, STRING_1, STRING_1, INT_1, INT_1)
            val vertex4 = LocalVertex(STRING_1, STRING_2, STRING_1, INT_1, INT_1)
            val vertex5 = LocalVertex(STRING_1, STRING_1, STRING_2, INT_1, INT_1)
            val vertex6 = LocalVertex(STRING_1, STRING_1, STRING_1, INT_2, INT_1)
            val vertex7 = LocalVertex(STRING_1, STRING_1, STRING_1, INT_1, INT_2)
            assertVertexEquality(vertex1, vertex2, vertex3)
            assertVertexEquality(vertex1, vertex2, vertex4)
            assertVertexEquality(vertex1, vertex2, vertex5)
            assertVertexEquality(vertex1, vertex2, vertex6)
            assertVertexEquality(vertex1, vertex2, vertex7)
        }

        @Test
        fun memberVertexEquality() {
            val vertex1 = MemberVertex(STRING_1, STRING_1, STRING_1, INT_1)
            val vertex2 = MemberVertex(STRING_1, STRING_1, STRING_1, INT_1)
            val vertex3 = MemberVertex(STRING_2, STRING_1, STRING_1, INT_1)
            val vertex4 = MemberVertex(STRING_1, STRING_2, STRING_1, INT_1)
            val vertex5 = MemberVertex(STRING_1, STRING_1, STRING_2, INT_1)
            val vertex6 = MemberVertex(STRING_1, STRING_1, STRING_1, INT_2)
            assertVertexEquality(vertex1, vertex2, vertex3)
            assertVertexEquality(vertex1, vertex2, vertex4)
            assertVertexEquality(vertex1, vertex2, vertex5)
            assertVertexEquality(vertex1, vertex2, vertex6)
        }

        @Test
        fun metaDataVertexEquality() {
            val vertex1 = MetaDataVertex(STRING_1, STRING_1)
            val vertex2 = MetaDataVertex(STRING_1, STRING_1)
            val vertex3 = MetaDataVertex(STRING_2, STRING_1)
            val vertex4 = MetaDataVertex(STRING_1, STRING_2)
            assertVertexEquality(vertex1, vertex2, vertex3)
            assertVertexEquality(vertex1, vertex2, vertex4)
        }

        @Test
        fun methodParameterInVertexEquality() {
            val vertex1 = MethodParameterInVertex(STRING_1, STRING_1, EVAL_1, STRING_1, INT_1, INT_1)
            val vertex2 = MethodParameterInVertex(STRING_1, STRING_1, EVAL_1, STRING_1, INT_1, INT_1)
            val vertex3 = MethodParameterInVertex(STRING_2, STRING_1, EVAL_1, STRING_1, INT_1, INT_1)
            val vertex4 = MethodParameterInVertex(STRING_1, STRING_2, EVAL_1, STRING_1, INT_1, INT_1)
            val vertex5 = MethodParameterInVertex(STRING_1, STRING_1, EVAL_2, STRING_1, INT_1, INT_1)
            val vertex6 = MethodParameterInVertex(STRING_1, STRING_1, EVAL_1, STRING_2, INT_1, INT_1)
            val vertex7 = MethodParameterInVertex(STRING_1, STRING_1, EVAL_1, STRING_1, INT_2, INT_1)
            val vertex8 = MethodParameterInVertex(STRING_1, STRING_1, EVAL_1, STRING_1, INT_1, INT_2)
            assertVertexEquality(vertex1, vertex2, vertex3)
            assertVertexEquality(vertex1, vertex2, vertex4)
            assertVertexEquality(vertex1, vertex2, vertex5)
            assertVertexEquality(vertex1, vertex2, vertex6)
            assertVertexEquality(vertex1, vertex2, vertex7)
            assertVertexEquality(vertex1, vertex2, vertex8)
        }

        @Test
        fun methodRefVertexEquality() {
            val vertex1 = MethodRefVertex(STRING_1, INT_1, INT_1, STRING_1, STRING_1, INT_1)
            val vertex2 = MethodRefVertex(STRING_1, INT_1, INT_1, STRING_1, STRING_1, INT_1)
            val vertex3 = MethodRefVertex(STRING_2, INT_1, INT_1, STRING_1, STRING_1, INT_1)
            val vertex4 = MethodRefVertex(STRING_1, INT_2, INT_1, STRING_1, STRING_1, INT_1)
            val vertex5 = MethodRefVertex(STRING_1, INT_1, INT_2, STRING_1, STRING_1, INT_1)
            val vertex6 = MethodRefVertex(STRING_1, INT_1, INT_1, STRING_2, STRING_1, INT_1)
            val vertex7 = MethodRefVertex(STRING_1, INT_1, INT_1, STRING_1, STRING_2, INT_1)
            val vertex8 = MethodRefVertex(STRING_1, INT_1, INT_1, STRING_1, STRING_1, INT_2)
            assertVertexEquality(vertex1, vertex2, vertex3)
            assertVertexEquality(vertex1, vertex2, vertex4)
            assertVertexEquality(vertex1, vertex2, vertex5)
            assertVertexEquality(vertex1, vertex2, vertex6)
            assertVertexEquality(vertex1, vertex2, vertex7)
            assertVertexEquality(vertex1, vertex2, vertex8)
        }

        @Test
        fun methodReturnVertexEquality() {
            val vertex1 = MethodReturnVertex(STRING_1, STRING_1, EVAL_1, INT_1, INT_1)
            val vertex2 = MethodReturnVertex(STRING_1, STRING_1, EVAL_1, INT_1, INT_1)
            val vertex3 = MethodReturnVertex(STRING_2, STRING_1, EVAL_1, INT_1, INT_1)
            val vertex4 = MethodReturnVertex(STRING_1, STRING_2, EVAL_1, INT_1, INT_1)
            val vertex5 = MethodReturnVertex(STRING_1, STRING_1, EVAL_2, INT_1, INT_1)
            val vertex6 = MethodReturnVertex(STRING_1, STRING_1, EVAL_1, INT_2, INT_1)
            val vertex7 = MethodReturnVertex(STRING_1, STRING_1, EVAL_1, INT_1, INT_2)
            assertVertexEquality(vertex1, vertex2, vertex3)
            assertVertexEquality(vertex1, vertex2, vertex4)
            assertVertexEquality(vertex1, vertex2, vertex5)
            assertVertexEquality(vertex1, vertex2, vertex6)
            assertVertexEquality(vertex1, vertex2, vertex7)
        }

        @Test
        fun methodVertexEquality() {
            val vertex1 = MethodVertex(STRING_1, STRING_1, STRING_1, INT_1, INT_1)
            val vertex2 = MethodVertex(STRING_1, STRING_1, STRING_1, INT_1, INT_1)
            val vertex3 = MethodVertex(STRING_2, STRING_1, STRING_1, INT_1, INT_1)
            val vertex4 = MethodVertex(STRING_1, STRING_2, STRING_1, INT_1, INT_1)
            val vertex5 = MethodVertex(STRING_1, STRING_1, STRING_2, INT_1, INT_1)
            val vertex6 = MethodVertex(STRING_1, STRING_1, STRING_1, INT_2, INT_1)
            val vertex7 = MethodVertex(STRING_1, STRING_1, STRING_1, INT_1, INT_2)
            assertVertexEquality(vertex1, vertex2, vertex3)
            assertVertexEquality(vertex1, vertex2, vertex4)
            assertVertexEquality(vertex1, vertex2, vertex5)
            assertVertexEquality(vertex1, vertex2, vertex6)
            assertVertexEquality(vertex1, vertex2, vertex7)
        }

        @Test
        fun modifierVertexEquality() {
            val vertex1 = ModifierVertex(MOD_1, INT_1)
            val vertex2 = ModifierVertex(MOD_1, INT_1)
            val vertex3 = ModifierVertex(MOD_2, INT_1)
            val vertex4 = ModifierVertex(MOD_1, INT_2)
            assertVertexEquality(vertex1, vertex2, vertex3)
            assertVertexEquality(vertex1, vertex2, vertex4)
        }

        @Test
        fun namespaceBlockVertexEquality() {
            val vertex1 = NamespaceBlockVertex(STRING_1, STRING_1, INT_1)
            val vertex2 = NamespaceBlockVertex(STRING_1, STRING_1, INT_1)
            val vertex3 = NamespaceBlockVertex(STRING_2, STRING_1, INT_1)
            val vertex4 = NamespaceBlockVertex(STRING_1, STRING_2, INT_1)
            val vertex5 = NamespaceBlockVertex(STRING_1, STRING_1, INT_2)
            assertVertexEquality(vertex1, vertex2, vertex3)
            assertVertexEquality(vertex1, vertex2, vertex4)
            assertVertexEquality(vertex1, vertex2, vertex5)
        }

        @Test
        fun returnVertexEquality() {
            val vertex1 = ReturnVertex(INT_1, INT_1, INT_1, STRING_1)
            val vertex2 = ReturnVertex(INT_1, INT_1, INT_1, STRING_1)
            val vertex3 = ReturnVertex(INT_2, INT_1, INT_1, STRING_1)
            val vertex4 = ReturnVertex(INT_1, INT_2, INT_1, STRING_1)
            val vertex5 = ReturnVertex(INT_1, INT_1, INT_2, STRING_1)
            val vertex6 = ReturnVertex(INT_1, INT_1, INT_1, STRING_2)
            assertVertexEquality(vertex1, vertex2, vertex3)
            assertVertexEquality(vertex1, vertex2, vertex4)
            assertVertexEquality(vertex1, vertex2, vertex5)
            assertVertexEquality(vertex1, vertex2, vertex6)
        }

        @Test
        fun typeArgumentVertexEquality() {
            val vertex1 = TypeArgumentVertex(INT_1)
            val vertex2 = TypeArgumentVertex(INT_1)
            val vertex3 = TypeArgumentVertex(INT_2)
            assertVertexEquality(vertex1, vertex2, vertex3)
        }

        @Test
        fun typeDeclVertexEquality() {
            val vertex1 = TypeDeclVertex(STRING_1, STRING_1, STRING_1)
            val vertex2 = TypeDeclVertex(STRING_1, STRING_1, STRING_1)
            val vertex3 = TypeDeclVertex(STRING_2, STRING_1, STRING_1)
            val vertex4 = TypeDeclVertex(STRING_1, STRING_2, STRING_1)
            val vertex5 = TypeDeclVertex(STRING_1, STRING_1, STRING_2)
            assertVertexEquality(vertex1, vertex2, vertex3)
            assertVertexEquality(vertex1, vertex2, vertex4)
            assertVertexEquality(vertex1, vertex2, vertex5)
        }

        @Test
        fun typeParameterVertexEquality() {
            val vertex1 = TypeParameterVertex(STRING_1, INT_1)
            val vertex2 = TypeParameterVertex(STRING_1, INT_1)
            val vertex3 = TypeParameterVertex(STRING_2, INT_1)
            assertVertexEquality(vertex1, vertex2, vertex3)
        }

        @Test
        fun typeVertexEquality() {
            val vertex1 = TypeVertex(STRING_1, STRING_1, STRING_1)
            val vertex2 = TypeVertex(STRING_1, STRING_1, STRING_1)
            val vertex3 = TypeVertex(STRING_2, STRING_1, STRING_1)
            val vertex4 = TypeVertex(STRING_1, STRING_2, STRING_1)
            val vertex5 = TypeVertex(STRING_1, STRING_1, STRING_2)
            assertVertexEquality(vertex1, vertex2, vertex3)
            assertVertexEquality(vertex1, vertex2, vertex4)
            assertVertexEquality(vertex1, vertex2, vertex5)
        }

        @Test
        fun unknownVertexEquality() {
            val vertex1 = UnknownVertex(STRING_1, INT_1, INT_1, INT_1, STRING_1)
            val vertex2 = UnknownVertex(STRING_1, INT_1, INT_1, INT_1, STRING_1)
            val vertex3 = UnknownVertex(STRING_2, INT_1, INT_1, INT_1, STRING_1)
            val vertex4 = UnknownVertex(STRING_1, INT_2, INT_1, INT_1, STRING_1)
            val vertex5 = UnknownVertex(STRING_1, INT_1, INT_2, INT_1, STRING_1)
            val vertex6 = UnknownVertex(STRING_1, INT_1, INT_1, INT_2, STRING_1)
            val vertex7 = UnknownVertex(STRING_1, INT_1, INT_1, INT_1, STRING_2)
            assertVertexEquality(vertex1, vertex2, vertex3)
            assertVertexEquality(vertex1, vertex2, vertex4)
            assertVertexEquality(vertex1, vertex2, vertex5)
            assertVertexEquality(vertex1, vertex2, vertex6)
            assertVertexEquality(vertex1, vertex2, vertex7)
        }
    }

    @Nested
    inner class `Domain model property tests` {
        @Test
        fun arrayInitializerVertexEquality() {
            val vertex1 = ArrayInitializerVertex(INT_1)
            assertEquals(VertexLabels.ARRAY_INITIALIZER, ArrayInitializerVertex.LABEL)
            assertEquals(EnumSet.of(VertexBaseTraits.AST_NODE), ArrayInitializerVertex.TRAITS)
            assertEquals(vertex1.order, INT_1)
        }

        @Test
        fun bindingVertexEquality() {
            val vertex1 = BindingVertex(STRING_1, STRING_1)
            assertEquals(VertexLabels.BINDING, BindingVertex.LABEL)
            assertEquals(EnumSet.noneOf(VertexBaseTraits::class.java), BindingVertex.TRAITS)
            assertEquals(vertex1.name, STRING_1)
            assertEquals(vertex1.signature, STRING_1)
        }

        @Test
        fun blockVertexEquality() {
            val vertex1 = BlockVertex(STRING_1, INT_1, INT_1, STRING_1, INT_1)
            assertEquals(VertexLabels.BLOCK, BlockVertex.LABEL)
            assertEquals(EnumSet.of(VertexBaseTraits.EXPRESSION), BlockVertex.TRAITS)
            assertEquals(vertex1.name, STRING_1)
            assertEquals(vertex1.order, INT_1)
            assertEquals(vertex1.typeFullName, STRING_1)
            assertEquals(vertex1.lineNumber, INT_1)
            assertEquals(vertex1.argumentIndex, INT_1)
        }

        @Test
        fun callVertexEquality() {
            val vertex1 = CallVertex(STRING_1, STRING_1, INT_1, STRING_1, STRING_1, INT_1, DISPATCH_1, STRING_1, STRING_1, INT_1)
            assertEquals(VertexLabels.CALL, CallVertex.LABEL)
            assertEquals(EnumSet.of(VertexBaseTraits.EXPRESSION, VertexBaseTraits.CALL_REPR), CallVertex.TRAITS)
            assertEquals(vertex1.name, STRING_1)
            assertEquals(vertex1.order, INT_1)
            assertEquals(vertex1.typeFullName, STRING_1)
            assertEquals(vertex1.lineNumber, INT_1)
            assertEquals(vertex1.argumentIndex, INT_1)
            assertEquals(vertex1.dispatchType, DISPATCH_1)
            assertEquals(vertex1.methodFullName, STRING_1)
            assertEquals(vertex1.methodInstFullName, STRING_1)
            assertEquals(vertex1.signature, STRING_1)
            assertEquals(vertex1.code, STRING_1)
        }

        @Test
        fun controlStructureVertexEquality() {
            val vertex1 = ControlStructureVertex(STRING_1, INT_1, INT_1, INT_1)
            assertEquals(VertexLabels.CONTROL_STRUCTURE, ControlStructureVertex.LABEL)
            assertEquals(EnumSet.of(VertexBaseTraits.EXPRESSION), ControlStructureVertex.TRAITS)
            assertEquals(vertex1.name, STRING_1)
            assertEquals(vertex1.order, INT_1)
            assertEquals(vertex1.lineNumber, INT_1)
            assertEquals(vertex1.argumentIndex, INT_1)
        }

        @Test
        fun fieldIdentifierVertexEquality() {
            val vertex1 = FieldIdentifierVertex(STRING_1, STRING_1, INT_1, INT_1, INT_1)
            assertEquals(VertexLabels.FIELD_IDENTIFIER, FieldIdentifierVertex.LABEL)
            assertEquals(EnumSet.of(VertexBaseTraits.EXPRESSION), FieldIdentifierVertex.TRAITS)
            assertEquals(vertex1.order, INT_1)
            assertEquals(vertex1.lineNumber, INT_1)
            assertEquals(vertex1.argumentIndex, INT_1)
            assertEquals(vertex1.code, STRING_1)
        }

        @Test
        fun fileVertexEquality() {
            val vertex1 = FileVertex(STRING_1, INT_1)
            assertEquals(VertexLabels.FILE, FileVertex.LABEL)
            assertEquals(EnumSet.of(VertexBaseTraits.AST_NODE), FileVertex.TRAITS)
            assertEquals(vertex1.name, STRING_1)
            assertEquals(vertex1.order, INT_1)
        }

        @Test
        fun identifierVertexEquality() {
            val vertex1 = IdentifierVertex(STRING_1, STRING_1, INT_1, INT_1, STRING_1, INT_1)
            assertEquals(VertexLabels.IDENTIFIER, IdentifierVertex.LABEL)
            assertEquals(EnumSet.of(VertexBaseTraits.EXPRESSION, VertexBaseTraits.LOCAL_LIKE), IdentifierVertex.TRAITS)
            assertEquals(vertex1.name, STRING_1)
            assertEquals(vertex1.order, INT_1)
            assertEquals(vertex1.typeFullName, STRING_1)
            assertEquals(vertex1.lineNumber, INT_1)
            assertEquals(vertex1.argumentIndex, INT_1)
            assertEquals(vertex1.code, STRING_1)
        }

        @Test
        fun literalVertexEquality() {
            val vertex1 = LiteralVertex(STRING_1, INT_1, INT_1, STRING_1, INT_1)
            assertEquals(VertexLabels.LITERAL, LiteralVertex.LABEL)
            assertEquals(EnumSet.of(VertexBaseTraits.EXPRESSION), LiteralVertex.TRAITS)
            assertEquals(vertex1.name, STRING_1)
            assertEquals(vertex1.order, INT_1)
            assertEquals(vertex1.typeFullName, STRING_1)
            assertEquals(vertex1.lineNumber, INT_1)
            assertEquals(vertex1.argumentIndex, INT_1)
        }

        @Test
        fun localVertexEquality() {
            val vertex1 = LocalVertex(STRING_1, STRING_1, STRING_1, INT_1, INT_1)
            assertEquals(VertexLabels.LOCAL, LocalVertex.LABEL)
            assertEquals(EnumSet.of(VertexBaseTraits.DECLARATION, VertexBaseTraits.LOCAL_LIKE, VertexBaseTraits.CALL_REPR), LocalVertex.TRAITS)
            assertEquals(vertex1.name, STRING_1)
            assertEquals(vertex1.order, INT_1)
            assertEquals(vertex1.typeFullName, STRING_1)
            assertEquals(vertex1.lineNumber, INT_1)
            assertEquals(vertex1.code, STRING_1)
        }

        @Test
        fun memberVertexEquality() {
            val vertex1 = MemberVertex(STRING_1, STRING_1, STRING_1, INT_1)
            assertEquals(VertexLabels.META_DATA, MetaDataVertex.LABEL)
            assertEquals(EnumSet.noneOf(VertexBaseTraits::class.java), MetaDataVertex.TRAITS)
            assertEquals(vertex1.name, STRING_1)
            assertEquals(vertex1.order, INT_1)
            assertEquals(vertex1.typeFullName, STRING_1)
            assertEquals(vertex1.code, STRING_1)
        }

        @Test
        fun metaDataVertexEquality() {
            val vertex1 = MetaDataVertex(STRING_1, STRING_1)
            assertEquals(VertexLabels.META_DATA, MetaDataVertex.LABEL)
            assertEquals(EnumSet.noneOf(VertexBaseTraits::class.java), MetaDataVertex.TRAITS)
            assertEquals(vertex1.language, STRING_1)
            assertEquals(vertex1.version, STRING_1)
        }

        @Test
        fun methodParameterInVertexEquality() {
            val vertex1 = MethodParameterInVertex(STRING_1, STRING_1, EVAL_1, STRING_1, INT_1, INT_1)
            assertEquals(VertexLabels.METHOD_PARAMETER_IN, MethodParameterInVertex.LABEL)
            assertEquals(EnumSet.of(VertexBaseTraits.AST_NODE, VertexBaseTraits.DECLARATION, VertexBaseTraits.CFG_NODE), MethodParameterInVertex.TRAITS)
            assertEquals(vertex1.name, STRING_1)
            assertEquals(vertex1.order, INT_1)
            assertEquals(vertex1.typeFullName, STRING_1)
            assertEquals(vertex1.lineNumber, INT_1)
            assertEquals(vertex1.code, STRING_1)
        }

        @Test
        fun methodRefVertexEquality() {
            val vertex1 = MethodRefVertex(STRING_1, INT_1, INT_1, STRING_1, STRING_1, INT_1)
            assertEquals(VertexLabels.METHOD_REF, MethodRefVertex.LABEL)
            assertEquals(EnumSet.of(VertexBaseTraits.EXPRESSION), MethodRefVertex.TRAITS)
            assertEquals(vertex1.order, INT_1)
            assertEquals(vertex1.lineNumber, INT_1)
            assertEquals(vertex1.argumentIndex, INT_1)
            assertEquals(vertex1.methodFullName, STRING_1)
            assertEquals(vertex1.methodInstFullName, STRING_1)
            assertEquals(vertex1.code, STRING_1)
        }

        @Test
        fun methodReturnVertexEquality() {
            val vertex1 = MethodReturnVertex(STRING_1, STRING_1, EVAL_1, INT_1, INT_1)
            assertEquals(VertexLabels.METHOD_RETURN, MethodReturnVertex.LABEL)
            assertEquals(EnumSet.of(VertexBaseTraits.CFG_NODE, VertexBaseTraits.TRACKING_POINT), MethodReturnVertex.TRAITS)
            assertEquals(vertex1.name, STRING_1)
            assertEquals(vertex1.order, INT_1)
            assertEquals(vertex1.typeFullName, STRING_1)
            assertEquals(vertex1.lineNumber, INT_1)
        }

        @Test
        fun methodVertexEquality() {
            val vertex1 = MethodVertex(STRING_1, STRING_1, STRING_1, INT_1, INT_1)
            assertEquals(VertexLabels.METHOD, MethodVertex.LABEL)
            assertEquals(EnumSet.of(VertexBaseTraits.AST_NODE, VertexBaseTraits.DECLARATION, VertexBaseTraits.CFG_NODE), MethodVertex.TRAITS)
            assertEquals(vertex1.name, STRING_1)
            assertEquals(vertex1.order, INT_1)
            assertEquals(vertex1.lineNumber, INT_1)
            assertEquals(vertex1.signature, STRING_1)
            assertEquals(vertex1.fullName, STRING_1)
        }

        @Test
        fun modifierVertexEquality() {
            val vertex1 = ModifierVertex(MOD_1, INT_1)
            assertEquals(VertexLabels.MODIFIER, ModifierVertex.LABEL)
            assertEquals(EnumSet.of(VertexBaseTraits.AST_NODE), ModifierVertex.TRAITS)
            assertEquals(vertex1.name, MOD_1)
            assertEquals(vertex1.order, INT_1)
        }

        @Test
        fun namespaceBlockVertexEquality() {
            val vertex1 = NamespaceBlockVertex(STRING_1, STRING_1, INT_1)
            assertEquals(VertexLabels.NAMESPACE_BLOCK, NamespaceBlockVertex.LABEL)
            assertEquals(EnumSet.of(VertexBaseTraits.AST_NODE), NamespaceBlockVertex.TRAITS)
            assertEquals(vertex1.name, STRING_1)
            assertEquals(vertex1.fullName, STRING_1)
            assertEquals(vertex1.order, INT_1)
        }

        @Test
        fun returnVertexEquality() {
            val vertex1 = ReturnVertex(INT_1, INT_1, INT_1, STRING_1)
            assertEquals(VertexLabels.RETURN, ReturnVertex.LABEL)
            assertEquals(EnumSet.of(VertexBaseTraits.EXPRESSION), ReturnVertex.TRAITS)
            assertEquals(vertex1.order, INT_1)
            assertEquals(vertex1.lineNumber, INT_1)
            assertEquals(vertex1.argumentIndex, INT_1)
            assertEquals(vertex1.code, STRING_1)
        }

        @Test
        fun typeArgumentVertexEquality() {
            val vertex1 = TypeArgumentVertex(INT_1)
            assertEquals(VertexLabels.TYPE_ARGUMENT, TypeArgumentVertex.LABEL)
            assertEquals(EnumSet.of(VertexBaseTraits.AST_NODE), TypeArgumentVertex.TRAITS)
            assertEquals(vertex1.order, INT_1)
        }

        @Test
        fun typeDeclVertexEquality() {
            val vertex1 = TypeDeclVertex(STRING_1, STRING_1, STRING_1)
            assertEquals(VertexLabels.TYPE_DECL, TypeDeclVertex.LABEL)
            assertEquals(EnumSet.of(VertexBaseTraits.AST_NODE), TypeDeclVertex.TRAITS)
            assertEquals(vertex1.name, STRING_1)
            assertEquals(vertex1.fullName, STRING_1)
            assertEquals(vertex1.typeDeclFullName, STRING_1)
        }

        @Test
        fun typeParameterVertexEquality() {
            val vertex1 = TypeParameterVertex(STRING_1, INT_1)
            assertEquals(VertexLabels.TYPE_PARAMETER, TypeParameterVertex.LABEL)
            assertEquals(EnumSet.of(VertexBaseTraits.AST_NODE), TypeParameterVertex.TRAITS)
            assertEquals(vertex1.name, STRING_1)
            assertEquals(vertex1.order, INT_1)
        }

        @Test
        fun typeVertexEquality() {
            val vertex1 = TypeVertex(STRING_1, STRING_1, STRING_1)
            assertEquals(VertexLabels.TYPE, TypeVertex.LABEL)
            assertEquals(EnumSet.noneOf(VertexBaseTraits::class.java), TypeVertex.TRAITS)
            assertEquals(vertex1.name, STRING_1)
            assertEquals(vertex1.fullName, STRING_1)
            assertEquals(vertex1.typeDeclFullName, STRING_1)
        }

        @Test
        fun unknownVertexEquality() {
            val vertex1 = UnknownVertex(STRING_1, INT_1, INT_1, INT_1, STRING_1)
            assertEquals(VertexLabels.UNKNOWN, UnknownVertex.LABEL)
            assertEquals(EnumSet.of(VertexBaseTraits.EXPRESSION), UnknownVertex.TRAITS)
            assertEquals(vertex1.order, INT_1)
            assertEquals(vertex1.typeFullName, STRING_1)
            assertEquals(vertex1.lineNumber, INT_1)
            assertEquals(vertex1.argumentIndex, INT_1)
            assertEquals(vertex1.code, STRING_1)
        }

        @Test
        fun testInterfaceDefaults() {
            assertEquals("UNKNOWN", PlumeVertex.LABEL.toString())
            assertEquals(EnumSet.noneOf(VertexBaseTraits::class.java), PlumeVertex.TRAITS)
        }
    }

}
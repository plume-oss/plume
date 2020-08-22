package za.ac.sun.plume.drivers

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import za.ac.sun.plume.TestDomainResources.Companion.FIRST_BLOCK
import za.ac.sun.plume.TestDomainResources.Companion.MOD_1
import za.ac.sun.plume.TestDomainResources.Companion.ROOT_PACKAGE
import za.ac.sun.plume.TestDomainResources.Companion.SECOND_PACKAGE
import za.ac.sun.plume.TestDomainResources.Companion.STRING_1
import za.ac.sun.plume.TestDomainResources.Companion.TEST_ID
import za.ac.sun.plume.TestDomainResources.Companion.THIRD_PACKAGE
import za.ac.sun.plume.domain.enums.EdgeLabel
import za.ac.sun.plume.domain.models.vertices.*

abstract class AbstractGremlinDriverTest : AbstractDriverTest() {
    /**
     * Provides a hook to a new database hook. Default is a new [TinkerGraphDriver].
     *
     * @return a built hook.
     */
    override fun provideHook(): GremlinDriver {
        return TinkerGraphDriver.Builder().build()
    }

    /**
     * Provides a hook builder to continue to configure.
     * Default is a [za.ac.sun.plume.drivers.TinkerGraphDriver.Builder].
     *
     * @return an [IDriverBuilder] to build with.
     */
    override fun provideBuilder(): GremlinDriverBuilder {
        return TinkerGraphDriver.Builder()
    }

    @Nested
    inner class GremlinCheckMethodJoinInteraction : CheckMethodJoinInteraction() {
        private lateinit var gremlinHook: GremlinDriver

        @BeforeEach
        override fun setUp() {
            super.setUp()
            gremlinHook = hook as GremlinDriver
        }

        @Test
        override fun joinMethodToMethodParamIn() {
            super.joinMethodToMethodParamIn()
            gremlinHook.openTx()
            Assertions.assertTrue(gremlinHook.g.E().hasLabel(EdgeLabel.AST.name).hasNext())
            Assertions.assertTrue(gremlinHook.g.V().hasLabel(MethodVertex.LABEL.name)
                    .out(EdgeLabel.AST.name)
                    .has(MethodParameterInVertex.LABEL.name, "name", STRING_1)
                    .hasNext())
            gremlinHook.closeTx()
        }

        @Test
        override fun joinMethodToMethodReturn() {
            super.joinMethodToMethodReturn()
            gremlinHook.openTx()
            Assertions.assertTrue(gremlinHook.g.E().hasLabel(EdgeLabel.AST.name).hasNext())
            Assertions.assertTrue(gremlinHook.g.V().hasLabel(MethodVertex.LABEL.name)
                    .out(EdgeLabel.AST.name)
                    .has(MethodReturnVertex.LABEL.name, "name", STRING_1)
                    .hasNext())
            gremlinHook.closeTx()
        }

        @Test
        override fun joinMethodToModifier() {
            super.joinMethodToModifier()
            gremlinHook.openTx()
            Assertions.assertTrue(gremlinHook.g.E().hasLabel(EdgeLabel.AST.name).hasNext())
            Assertions.assertTrue(gremlinHook.g.V().hasLabel(MethodVertex.LABEL.name)
                    .out(EdgeLabel.AST.name)
                    .has(ModifierVertex.LABEL.name, "name", MOD_1.name)
                    .hasNext())
            gremlinHook.closeTx()
        }
    }

    @Nested
    inner class GremlinFileJoinInteraction : FileJoinInteraction() {
        lateinit var gremlinHook: GremlinDriver

        @BeforeEach
        override fun setUp() {
            super.setUp()
            gremlinHook = hook as GremlinDriver
        }

        @Test
        override fun testJoinFile2NamespaceBlock() {
            super.testJoinFile2NamespaceBlock()
            gremlinHook.openTx()
            Assertions.assertTrue(gremlinHook.g.E().hasLabel(EdgeLabel.AST.name).hasNext())
            Assertions.assertTrue(gremlinHook.g.V().has(NamespaceBlockVertex.LABEL.name, "fullName", STRING_1)
                    .out(EdgeLabel.AST.name)
                    .hasLabel(FileVertex.LABEL.name)
                    .hasNext())
            gremlinHook.closeTx()
        }

        @Test
        override fun testJoinFile2Method() {
            super.testJoinFile2Method()
            gremlinHook.openTx()
            Assertions.assertTrue(gremlinHook.g.E().hasLabel(EdgeLabel.AST.name).hasNext())
            Assertions.assertTrue(gremlinHook.g.V().hasLabel(FileVertex.LABEL.name)
                    .out(EdgeLabel.AST.name)
                    .has(MethodVertex.LABEL.name, "fullName", STRING_1)
                    .hasNext())
            gremlinHook.closeTx()
        }
    }

    @Nested
    inner class GremlinBlockJoinInteraction : BlockJoinInteraction() {
        lateinit var gremlinHook: GremlinDriver

        @BeforeEach
        override fun setUp() {
            super.setUp()
            gremlinHook = hook as GremlinDriver
        }

        @Test
        override fun testMethodJoinBlockTest() {
            super.testMethodJoinBlockTest()
            gremlinHook.openTx()
            Assertions.assertTrue(gremlinHook.g.E().hasLabel(EdgeLabel.AST.name).hasNext())
            Assertions.assertTrue(gremlinHook.g.V().hasLabel(MethodVertex.LABEL.name)
                    .out(EdgeLabel.AST.name)
                    .has(BlockVertex.LABEL.name, "name", FIRST_BLOCK)
                    .hasNext())
            gremlinHook.closeTx()
        }

        @Test
        override fun testBlockJoinBlockTest() {
            super.testBlockJoinBlockTest()
            gremlinHook.openTx()
            Assertions.assertTrue(gremlinHook.g.V().has(BlockVertex.LABEL.name, "name", FIRST_BLOCK)
                    .out(EdgeLabel.AST.name)
                    .has(BlockVertex.LABEL.name, "name", TEST_ID)
                    .hasNext())
            gremlinHook.closeTx()
        }

        @Test
        override fun testAssignLiteralToBlock() {
            super.testAssignLiteralToBlock()
            gremlinHook.openTx()
            Assertions.assertTrue(gremlinHook.g.V().has(BlockVertex.LABEL.name, "name", FIRST_BLOCK)
                    .out(EdgeLabel.AST.name)
                    .has(LiteralVertex.LABEL.name, "name", TEST_ID)
                    .hasNext())
            gremlinHook.closeTx()
        }

        @Test
        override fun testAssignLocalToBlock() {
            super.testAssignLocalToBlock()
            gremlinHook.openTx()
            Assertions.assertTrue(gremlinHook.g.V().has(BlockVertex.LABEL.name, "name", FIRST_BLOCK)
                    .out(EdgeLabel.AST.name)
                    .has(LocalVertex.LABEL.name, "name", TEST_ID)
                    .hasNext())
            gremlinHook.closeTx()
        }

        @Test
        override fun testAssignControlToBlock() {
            super.testAssignControlToBlock()
            gremlinHook.openTx()
            Assertions.assertTrue(gremlinHook.g.V().has(BlockVertex.LABEL.name, "name", FIRST_BLOCK)
                    .out(EdgeLabel.AST.name)
                    .has(ControlStructureVertex.LABEL.name, "name", TEST_ID)
                    .hasNext())
            gremlinHook.closeTx()
        }
    }

    @Nested
    inner class GremlinNamespaceBlockJoinInteraction : NamespaceBlockJoinInteraction() {
        lateinit var gremlinHook: GremlinDriver

        @BeforeEach
        override fun setUp() {
            super.setUp()
            gremlinHook = hook as GremlinDriver
        }

        @Test
        override fun joinTwoNamespaceBlocks() {
            super.joinTwoNamespaceBlocks()
            gremlinHook.openTx()
            Assertions.assertTrue(gremlinHook.g.V().has(NamespaceBlockVertex.LABEL.name, "name", ROOT_PACKAGE)
                    .out(EdgeLabel.AST.name)
                    .has(NamespaceBlockVertex.LABEL.name, "name", SECOND_PACKAGE).hasNext())
            gremlinHook.closeTx()
        }

        @Test
        override fun joinThreeNamespaceBlocks() {
            super.joinThreeNamespaceBlocks()
            gremlinHook.openTx()
            Assertions.assertTrue(gremlinHook.g.V().has(NamespaceBlockVertex.LABEL.name, "name", ROOT_PACKAGE)
                    .out(EdgeLabel.AST.name)
                    .has(NamespaceBlockVertex.LABEL.name, "name", SECOND_PACKAGE).hasNext())
            Assertions.assertTrue(gremlinHook.g.V().has(NamespaceBlockVertex.LABEL.name, "name", SECOND_PACKAGE)
                    .out(EdgeLabel.AST.name)
                    .has(NamespaceBlockVertex.LABEL.name, "name", THIRD_PACKAGE).hasNext())
            gremlinHook.closeTx()
        }

        @Test
        override fun joinExistingConnection() {
            super.joinExistingConnection()
            gremlinHook.openTx()
            Assertions.assertEquals(1, gremlinHook.g.V().has(NamespaceBlockVertex.LABEL.name, "name", ROOT_PACKAGE)
                    .out(EdgeLabel.AST.name).count().next())
            gremlinHook.closeTx()
        }
    }

    @Nested
    inner class GremlinUpdateChecks : UpdateChecks() {
        lateinit var gremlinHook: GremlinDriver

        @BeforeEach
        override fun setUp() {
            super.setUp()
            gremlinHook = hook as GremlinDriver
            gremlinHook.openTx()
            Assertions.assertTrue(gremlinHook.g.V().hasLabel(BlockVertex.LABEL.name).has(super.keyToTest, super.initValue).hasNext())
            gremlinHook.closeTx()
        }

        @Test
        override fun testUpdateOnOneBlockProperty() {
            super.testUpdateOnOneBlockProperty()
            gremlinHook.openTx()
            Assertions.assertTrue(gremlinHook.g.V().hasLabel(BlockVertex.LABEL.name).has(super.keyToTest, super.updatedValue).hasNext())
            gremlinHook.closeTx()
        }
    }
}
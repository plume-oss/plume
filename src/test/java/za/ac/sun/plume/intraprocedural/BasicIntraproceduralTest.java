package za.ac.sun.plume.intraprocedural;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import za.ac.sun.plume.Extractor;
import za.ac.sun.plume.domain.enums.EdgeLabels;
import za.ac.sun.plume.domain.enums.VertexLabels;
import za.ac.sun.plume.domain.models.vertices.BlockVertex;
import za.ac.sun.plume.domain.models.vertices.LiteralVertex;
import za.ac.sun.plume.domain.models.vertices.LocalVertex;
import za.ac.sun.plume.drivers.TinkerGraphDriver;
import za.ac.sun.plume.util.ResourceCompilationUtil;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static za.ac.sun.plume.util.TestQueryBuilderUtil.*;

public class BasicIntraproceduralTest {

    final static Logger logger = LogManager.getLogger();

    private static final File PATH;
    private static final String TEST_DIR = "/tmp/plume/plume-extractor-test.xml";
    private GraphTraversalSource g;
    private String currentTestNumber;

    static {
        PATH = new File(Objects.requireNonNull(ArithmeticTest.class.getClassLoader().getResource("intraprocedural/basic")).getFile());
    }

    @AfterAll
    static void tearDownAll() throws IOException {
        ResourceCompilationUtil.deleteClassFiles(PATH);
        File f = new File(TEST_DIR);
        if (f.exists()) {
            if (!f.delete()) {
                logger.warn("Could not clear " + BasicIntraproceduralTest.class.getName() + "'s test resources.");
            }
        }
    }

    @BeforeEach
    public void setUp(TestInfo testInfo) throws IOException {
        TinkerGraphDriver hook = new TinkerGraphDriver.Builder().build();
        Extractor fileCannon = new Extractor(hook);
        // Select test resource based on integer in method name
        currentTestNumber = testInfo
                .getDisplayName()
                .replaceAll("[^0-9]", "");
        String resourceDir = PATH.getAbsolutePath().concat("/Basic").concat(currentTestNumber).concat(".java");
        // Load test resource and project + export graph
        File f = new File(resourceDir);
        fileCannon.load(f);
        fileCannon.project();
        hook.exportCurrentGraph(TEST_DIR);

        g = TinkerGraph.open().traversal();
        g.io(TEST_DIR).read().iterate();
    }

    public static void testBasic1Structure(final GraphTraversalSource g, final Vertex methodRoot) {
        assertTrue(buildStoreTraversal(g, EdgeLabels.AST, methodRoot).hasNext());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LiteralVertex.LABEL, "name", "3").has("typeFullName", "INTEGER").hasNext());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "2").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LiteralVertex.LABEL, "name", "2").has("typeFullName", "INTEGER").hasNext());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "3").has("typeFullName", "INTEGER").hasNext());
        final GraphTraversal<Vertex, Vertex> addTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "ADD").has("typeFullName", "INTEGER");
        assertTrue(addTraversal.hasNext());
        final Vertex addVertex = addTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, addVertex, LocalVertex.LABEL, "name", "2").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, addVertex, LocalVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());
    }

    @Test
    public void basic1Test() {
        final GraphTraversal<Vertex, Vertex> methodTraversal = g.V()
                .has("METHOD", "fullName", "intraprocedural.basic.Basic".concat(currentTestNumber).concat(".main"));
        assertTrue(methodTraversal.hasNext());
        final Vertex methodRoot = methodTraversal.next();
        assertEquals(3, buildStoreTraversal(g, EdgeLabels.AST, methodRoot).count().next());

        testBasic1Structure(g, methodRoot);
    }

    @Test
    public void basic2Test() {
        final GraphTraversal<Vertex, Vertex> methodTraversal = g.V()
                .has("METHOD", "fullName", "intraprocedural.basic.Basic".concat(currentTestNumber).concat(".main"));
        assertTrue(methodTraversal.hasNext());
        final Vertex methodRoot = methodTraversal.next();

        assertTrue(buildStoreTraversal(g, EdgeLabels.AST, methodRoot).hasNext());
        assertEquals(3, buildStoreTraversal(g, EdgeLabels.AST, methodRoot).count().next());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LiteralVertex.LABEL, "name", "6").has("typeFullName", "INTEGER").hasNext());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "2").has("typeFullName", "DOUBLE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LiteralVertex.LABEL, "name", "2.0").has("typeFullName", "DOUBLE").hasNext());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "4").has("typeFullName", "DOUBLE").hasNext());
        final GraphTraversal<Vertex, Vertex> addTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "ADD").has("typeFullName", "DOUBLE");
        assertTrue(addTraversal.hasNext());
        final Vertex addVertex = addTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, addVertex, LocalVertex.LABEL, "name", "2").has("typeFullName", "DOUBLE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, addVertex, LocalVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());
    }

    @Test
    public void basic3Test() {
        final GraphTraversal<Vertex, Vertex> methodTraversal = g.V()
                .has("METHOD", "fullName", "intraprocedural.basic.Basic".concat(currentTestNumber).concat(".main"));
        assertTrue(methodTraversal.hasNext());
        final Vertex methodRoot = methodTraversal.next();

        assertTrue(buildStoreTraversal(g, EdgeLabels.AST, methodRoot).hasNext());
        assertEquals(3, buildStoreTraversal(g, EdgeLabels.AST, methodRoot).count().next());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "1").has("typeFullName", "LONG").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LiteralVertex.LABEL, "name", "4300343223423").has("typeFullName", "LONG").hasNext());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "3").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LiteralVertex.LABEL, "name", "-2342").has("typeFullName", "INTEGER").hasNext());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "4").has("typeFullName", "LONG").hasNext());
        final GraphTraversal<Vertex, Vertex> addTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "ADD").has("typeFullName", "LONG");
        assertTrue(addTraversal.hasNext());
        final Vertex addVertex = addTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, addVertex, LocalVertex.LABEL, "name", "3").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, addVertex, LocalVertex.LABEL, "name", "1").has("typeFullName", "LONG").hasNext());
    }

    @Test
    public void basic4Test() {
        final GraphTraversal<Vertex, Vertex> constructorTraversal = g.V()
                .has("METHOD", "fullName", "intraprocedural.basic.Basic".concat(currentTestNumber).concat(".<init>"));
        assertTrue(constructorTraversal.hasNext());
        final Vertex constructor = constructorTraversal.next();
        assertTrue(buildMethodModifierTraversal(g, EdgeLabels.AST, constructor).hasNext());
        assertEquals(3, buildMethodModifierTraversal(g, EdgeLabels.AST, constructor).count().next());
        assertTrue(buildMethodModifierTraversal(g, EdgeLabels.AST, constructor).has("name", "VIRTUAL").hasNext());
        assertTrue(buildMethodModifierTraversal(g, EdgeLabels.AST, constructor).has("name", "CONSTRUCTOR").hasNext());
        assertTrue(buildMethodModifierTraversal(g, EdgeLabels.AST, constructor).has("name", "PUBLIC").hasNext());
        assertTrue(buildMethodReturnTraversal(g, EdgeLabels.AST, constructor).has("name", "VOID").hasNext());

        final GraphTraversal<Vertex, Vertex> johnTraversal = g.V()
                .has("METHOD", "fullName", "intraprocedural.basic.Basic".concat(currentTestNumber).concat(".John"));
        assertTrue(johnTraversal.hasNext());
        final Vertex johnVertex = johnTraversal.next();
        assertTrue(buildMethodModifierTraversal(g, EdgeLabels.AST, johnVertex).hasNext());
        assertEquals(2, buildMethodModifierTraversal(g, EdgeLabels.AST, johnVertex).count().next());
        assertTrue(buildMethodModifierTraversal(g, EdgeLabels.AST, johnVertex).has("name", "STATIC").hasNext());
        assertTrue(buildMethodModifierTraversal(g, EdgeLabels.AST, johnVertex).has("name", "PUBLIC").hasNext());
        assertTrue(buildMethodReturnTraversal(g, EdgeLabels.AST, johnVertex).has("name", "[INTEGER").hasNext());

        final GraphTraversal<Vertex, Vertex> mainTraversal = g.V()
                .has("METHOD", "fullName", "intraprocedural.basic.Basic".concat(currentTestNumber).concat(".main"));
        assertTrue(mainTraversal.hasNext());
        final Vertex mainVertex = mainTraversal.next();
        assertTrue(buildMethodModifierTraversal(g, EdgeLabels.AST, mainVertex).hasNext());
        assertEquals(2, buildMethodModifierTraversal(g, EdgeLabels.AST, mainVertex).count().next());
        assertTrue(buildMethodModifierTraversal(g, EdgeLabels.AST, mainVertex).has("name", "STATIC").hasNext());
        assertTrue(buildMethodModifierTraversal(g, EdgeLabels.AST, mainVertex).has("name", "PUBLIC").hasNext());
        assertTrue(buildMethodReturnTraversal(g, EdgeLabels.AST, mainVertex).has("name", "VOID").hasNext());
        assertTrue(buildMethodParameterInTraversal(g, EdgeLabels.AST, mainVertex).has("name", "[String").hasNext());

        final GraphTraversal<Vertex, Vertex> dickTraversal = g.V()
                .has("METHOD", "fullName", "intraprocedural.basic.Basic".concat(currentTestNumber).concat(".Dick"));
        assertTrue(dickTraversal.hasNext());
        final Vertex dickVertex = dickTraversal.next();
        assertTrue(buildMethodModifierTraversal(g, EdgeLabels.AST, dickVertex).hasNext());
        assertEquals(2, buildMethodModifierTraversal(g, EdgeLabels.AST, dickVertex).count().next());
        assertTrue(buildMethodModifierTraversal(g, EdgeLabels.AST, dickVertex).has("name", "STATIC").hasNext());
        assertTrue(buildMethodModifierTraversal(g, EdgeLabels.AST, dickVertex).has("name", "PUBLIC").hasNext());
        assertTrue(buildMethodReturnTraversal(g, EdgeLabels.AST, dickVertex).has("name", "BOOLEAN").hasNext());

        final GraphTraversal<Vertex, Vertex> nigelTraversal = g.V()
                .has("METHOD", "fullName", "intraprocedural.basic.Basic".concat(currentTestNumber).concat(".Nigel"));
        assertTrue(nigelTraversal.hasNext());
        final Vertex nigelVertex = nigelTraversal.next();
        assertTrue(buildMethodModifierTraversal(g, EdgeLabels.AST, nigelVertex).hasNext());
        assertEquals(2, buildMethodModifierTraversal(g, EdgeLabels.AST, nigelVertex).count().next());
        assertTrue(buildMethodModifierTraversal(g, EdgeLabels.AST, nigelVertex).has("name", "STATIC").hasNext());
        assertTrue(buildMethodModifierTraversal(g, EdgeLabels.AST, nigelVertex).has("name", "PUBLIC").hasNext());
        assertTrue(buildMethodReturnTraversal(g, EdgeLabels.AST, nigelVertex).has("name", "DOUBLE").hasNext());

        final GraphTraversal<Vertex, Vertex> sallyTraversal = g.V()
                .has("METHOD", "fullName", "intraprocedural.basic.Basic".concat(currentTestNumber).concat(".Sally"));
        assertTrue(sallyTraversal.hasNext());
        final Vertex sallyVertex = sallyTraversal.next();
        assertTrue(buildMethodModifierTraversal(g, EdgeLabels.AST, sallyVertex).hasNext());
        assertEquals(2, buildMethodModifierTraversal(g, EdgeLabels.AST, sallyVertex).count().next());
        assertTrue(buildMethodModifierTraversal(g, EdgeLabels.AST, sallyVertex).has("name", "STATIC").hasNext());
        assertTrue(buildMethodModifierTraversal(g, EdgeLabels.AST, sallyVertex).has("name", "PUBLIC").hasNext());
        assertTrue(buildMethodReturnTraversal(g, EdgeLabels.AST, sallyVertex).has("name", "INTEGER").hasNext());
    }

    @Test
    public void basic5Test() {
        // This is Basic1 without a package, so we will just check that no package is present
        final GraphTraversal<Vertex, Vertex> mainMethodTraversal = g.V()
                .has("METHOD", "fullName", "Basic".concat(currentTestNumber).concat(".main"));
        assertTrue(mainMethodTraversal.hasNext());
        final Vertex mainMethod = mainMethodTraversal.next();

        assertFalse(g.V(mainMethod).repeat(__.in(EdgeLabels.AST.toString())).emit()
                .hasLabel(VertexLabels.NAMESPACE_BLOCK.toString()).hasNext());

        testBasic1Structure(g, mainMethod);
    }

    @Test
    public void basic6Test() throws IOException {
        TinkerGraphDriver hook = new TinkerGraphDriver.Builder().useExistingGraph(TEST_DIR).build();
        Extractor fileCannon = new Extractor(hook);
        String resourceDir = PATH.getAbsolutePath().concat("/basic6/Basic6.java");
        // Load test resource and project + export graph
        File f = new File(resourceDir);
        fileCannon.load(f);
        fileCannon.project();
        hook.exportCurrentGraph(TEST_DIR);

        g = TinkerGraph.open().traversal();
        g.io(TEST_DIR).read().iterate();

        // This is Basic1 in two separate packages
        final GraphTraversal<Vertex, Vertex> intraNamespaceTraversal = g.V().has(VertexLabels.NAMESPACE_BLOCK.toString(), "fullName", "intraprocedural");
        assertTrue(intraNamespaceTraversal.hasNext());
        final Vertex intraNamespaceVertex = intraNamespaceTraversal.next();
        final GraphTraversal<Vertex, Vertex> basicNamespaceTraversal = getVertexAlongEdge(g, EdgeLabels.AST, intraNamespaceVertex, VertexLabels.NAMESPACE_BLOCK, "fullName", "intraprocedural.basic");
        assertTrue(basicNamespaceTraversal.hasNext());
        final Vertex basicNamespaceVertex = basicNamespaceTraversal.next();
        final GraphTraversal<Vertex, Vertex> basic6NamespaceTraversal = getVertexAlongEdge(g, EdgeLabels.AST, basicNamespaceVertex, VertexLabels.NAMESPACE_BLOCK, "fullName", "intraprocedural.basic.basic6");
        assertTrue(basic6NamespaceTraversal.hasNext());
        final Vertex basic6NamespaceVertex = basic6NamespaceTraversal.next();

        final GraphTraversal<Vertex, Vertex> basicMethodTraversal = getVertexAlongEdge(g, EdgeLabels.AST, basicNamespaceVertex, VertexLabels.METHOD, "name", "main");
        assertTrue(basicMethodTraversal.hasNext());
        final GraphTraversal<Vertex, Vertex> basic6MethodTraversal = getVertexAlongEdge(g, EdgeLabels.AST, basic6NamespaceVertex, VertexLabels.METHOD, "name", "main");
        assertTrue(basic6MethodTraversal.hasNext());

        assertEquals(6, buildStoreTraversal(g, EdgeLabels.AST, intraNamespaceVertex).count().next());

        testBasic1Structure(g, basicNamespaceVertex);
        testBasic1Structure(g, basic6NamespaceVertex);
    }

    @Test
    public void basic7Test() {
        // This is Basic1 in one package
        final GraphTraversal<Vertex, Vertex> intraNamespaceTraversal = g.V().has(VertexLabels.NAMESPACE_BLOCK.toString(), "fullName", "basic");
        assertTrue(intraNamespaceTraversal.hasNext());
        final Vertex intraNamespaceVertex = intraNamespaceTraversal.next();
        testBasic1Structure(g, intraNamespaceVertex);
    }

}

package za.ac.sun.plume.intraprocedural;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.junit.jupiter.api.*;
import za.ac.sun.plume.Extractor;
import za.ac.sun.plume.domain.enums.EdgeLabels;
import za.ac.sun.plume.domain.models.vertices.BlockVertex;
import za.ac.sun.plume.domain.models.vertices.LiteralVertex;
import za.ac.sun.plume.domain.models.vertices.LocalVertex;
import za.ac.sun.plume.drivers.TinkerGraphDriver;
import za.ac.sun.plume.util.ResourceCompilationUtil;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static za.ac.sun.plume.util.TestQueryBuilderUtil.buildStoreTraversal;
import static za.ac.sun.plume.util.TestQueryBuilderUtil.getVertexAlongEdge;

@Disabled
public class ArithmeticTest {
    final static Logger logger = LogManager.getLogger(ArithmeticTest.class);

    private static final File PATH;
    private static final String TEST_DIR = "/tmp/plume/plume-extractor-test.xml";
    private GraphTraversalSource g;
    private Vertex methodRoot;

    static {
        PATH = new File(Objects.requireNonNull(ArithmeticTest.class.getClassLoader().getResource("intraprocedural/arithmetic/")).getFile());
    }

    @AfterAll
    static void tearDownAll() throws IOException {
        ResourceCompilationUtil.deleteClassFiles(PATH);
        File f = new File(TEST_DIR);
        if (f.exists()) {
            if (!f.delete()) {
                logger.warn("Could not clear " + ArithmeticTest.class.getName() + "'s test resources.");
            }
        }
    }

    @BeforeEach
    public void setUp(TestInfo testInfo) throws IOException {
        TinkerGraphDriver hook = new TinkerGraphDriver.Builder().build();
        Extractor fileCannon = new Extractor(hook);
        // Select test resource based on integer in method name
        final String currentTestNumber = testInfo
                .getDisplayName()
                .replaceAll("[^0-9]", "");
        String resourceDir = PATH.getAbsolutePath().concat("/Arithmetic").concat(currentTestNumber).concat(".java");
        // Load test resource and project + export graph
        File f = new File(resourceDir);
        fileCannon.load(f);
        fileCannon.project();
        hook.exportCurrentGraph(TEST_DIR);

        g = TinkerGraph.open().traversal();
        g.io(TEST_DIR).read().iterate();

        final GraphTraversal<Vertex, Vertex> methodTraversal = g.V()
                .has("METHOD", "fullName", "intraprocedural.arithmetic.Arithmetic"
                        .concat(currentTestNumber).concat(".main"));
        assertTrue(methodTraversal.hasNext());
        methodRoot = methodTraversal.next();
    }

    @Test
    public void arithmetic1Test() {
        assertTrue(buildStoreTraversal(g, EdgeLabels.AST, methodRoot).hasNext());
        assertEquals(6, buildStoreTraversal(g, EdgeLabels.AST, methodRoot).count().next());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LiteralVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "2").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LiteralVertex.LABEL, "name", "2").has("typeFullName", "INTEGER").hasNext());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "4").has("typeFullName", "INTEGER").hasNext());
        final GraphTraversal<Vertex, Vertex> subTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "SUB").has("typeFullName", "INTEGER");
        assertTrue(subTraversal.hasNext());
        final Vertex subVertex = subTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, subVertex, LocalVertex.LABEL, "name", "2").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, subVertex, LocalVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "6").has("typeFullName", "INTEGER").hasNext());
        final GraphTraversal<Vertex, Vertex> divTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "DIV").has("typeFullName", "INTEGER");
        assertTrue(divTraversal.hasNext());
        final Vertex divVertex = divTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, divVertex, LocalVertex.LABEL, "name", "2").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, divVertex, LocalVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "3").has("typeFullName", "INTEGER").hasNext());
        final GraphTraversal<Vertex, Vertex> addTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "ADD").has("typeFullName", "INTEGER");
        assertTrue(addTraversal.hasNext());
        final Vertex addVertex = addTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, addVertex, LocalVertex.LABEL, "name", "2").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, addVertex, LocalVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "3").has("typeFullName", "INTEGER").hasNext());
        final GraphTraversal<Vertex, Vertex> mulTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "MUL").has("typeFullName", "INTEGER");
        assertTrue(mulTraversal.hasNext());
        final Vertex mulVertex = mulTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, mulVertex, LocalVertex.LABEL, "name", "2").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, mulVertex, LocalVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());
    }

    @Test
    public void arithmetic2Test() {
        assertTrue(buildStoreTraversal(g, EdgeLabels.AST, methodRoot).hasNext());
        assertEquals(4, buildStoreTraversal(g, EdgeLabels.AST, methodRoot).count().next());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "4").has("typeFullName", "INTEGER").hasNext());
        final GraphTraversal<Vertex, Vertex> addTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "ADD").has("typeFullName", "INTEGER");
        assertTrue(addTraversal.hasNext());
        final Vertex addVertex = addTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, addVertex, LocalVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());

        final GraphTraversal<Vertex, Vertex> mulTraversal = getVertexAlongEdge(g, EdgeLabels.AST, addVertex, BlockVertex.LABEL, "name", "MUL").has("typeFullName", "INTEGER");
        assertTrue(mulTraversal.hasNext());
        final Vertex mulVertex = mulTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, mulVertex, LocalVertex.LABEL, "name", "3").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, mulVertex, LocalVertex.LABEL, "name", "2").has("typeFullName", "INTEGER").hasNext());
    }

    @Test
    public void arithmetic3Test() {
        assertTrue(buildStoreTraversal(g, EdgeLabels.AST, methodRoot).hasNext());
        assertEquals(4, buildStoreTraversal(g, EdgeLabels.AST, methodRoot).count().next());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "4").has("typeFullName", "INTEGER").hasNext());
        final GraphTraversal<Vertex, Vertex> subTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "SUB").has("typeFullName", "INTEGER").has("typeFullName", "INTEGER");
        assertTrue(subTraversal.hasNext());
        final Vertex subVertex = subTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, subVertex, LocalVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());

        final GraphTraversal<Vertex, Vertex> addTraversal = getVertexAlongEdge(g, EdgeLabels.AST, subVertex, BlockVertex.LABEL, "name", "ADD").has("typeFullName", "INTEGER");
        assertTrue(addTraversal.hasNext());
        final Vertex addVertex = addTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, addVertex, LocalVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());

        final GraphTraversal<Vertex, Vertex> mulTraversal = getVertexAlongEdge(g, EdgeLabels.AST, addVertex, BlockVertex.LABEL, "name", "MUL").has("typeFullName", "INTEGER");
        assertTrue(mulTraversal.hasNext());
        final Vertex mulVertex = mulTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, mulVertex, LocalVertex.LABEL, "name", "3").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, mulVertex, LocalVertex.LABEL, "name", "2").has("typeFullName", "INTEGER").hasNext());
    }

    @Test
    public void arithmetic4Test() {
        assertTrue(buildStoreTraversal(g, EdgeLabels.AST, methodRoot).hasNext());
        assertEquals(6, buildStoreTraversal(g, EdgeLabels.AST, methodRoot).count().next());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "2").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LiteralVertex.LABEL, "name", "2").has("typeFullName", "INTEGER").hasNext());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LiteralVertex.LABEL, "name", "-1").has("typeFullName", "INTEGER").hasNext());

        final GraphTraversal<Vertex, Vertex> incA = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "ADD").has("typeFullName", "INTEGER").has("order", 21);
        assertTrue(incA.hasNext());
        final Vertex incAVertex = incA.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, incAVertex, LocalVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, incAVertex, LiteralVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());

        final GraphTraversal<Vertex, Vertex> subB = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "SUB").has("typeFullName", "INTEGER").has("order", 29);
        assertTrue(subB.hasNext());
        final Vertex subBVertex = subB.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, subBVertex, LocalVertex.LABEL, "name", "2").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, subBVertex, LiteralVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());

        final GraphTraversal<Vertex, Vertex> storeC = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "STORE").has("typeFullName", "INTEGER").has("order", 24);
        assertTrue(storeC.hasNext());
        final Vertex storeCVertex = storeC.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, storeCVertex, LocalVertex.LABEL, "name", "3").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, storeCVertex, LocalVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());

        final GraphTraversal<Vertex, Vertex> storeD = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "STORE").has("typeFullName", "INTEGER").has("order", 32);
        assertTrue(storeD.hasNext());
        final Vertex storeDVertex = storeD.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, storeDVertex, LocalVertex.LABEL, "name", "4").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, storeDVertex, LocalVertex.LABEL, "name", "2").has("typeFullName", "INTEGER").hasNext());
    }

    @Test
    public void arithmetic5Test() {
        assertTrue(buildStoreTraversal(g, EdgeLabels.AST, methodRoot).hasNext());
        assertEquals(7, buildStoreTraversal(g, EdgeLabels.AST, methodRoot).count().next());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LiteralVertex.LABEL, "name", "13682").has("typeFullName", "INTEGER").hasNext());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "2").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LiteralVertex.LABEL, "name", "27371").has("typeFullName", "INTEGER").hasNext());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "5").has("typeFullName", "INTEGER").hasNext());
        final GraphTraversal<Vertex, Vertex> shlTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "SHL").has("typeFullName", "INTEGER");
        assertTrue(shlTraversal.hasNext());
        final Vertex shlVertex = shlTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, shlVertex, LocalVertex.LABEL, "name", "2").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, shlVertex, LocalVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "3").has("typeFullName", "INTEGER").hasNext());
        final GraphTraversal<Vertex, Vertex> andTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "AND").has("typeFullName", "INTEGER");
        assertTrue(andTraversal.hasNext());
        final Vertex andVertex = andTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, andVertex, LocalVertex.LABEL, "name", "2").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, andVertex, LocalVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "6").has("typeFullName", "INTEGER").hasNext());
        final GraphTraversal<Vertex, Vertex> shrTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "SHR").has("typeFullName", "INTEGER");
        assertTrue(shrTraversal.hasNext());
        final Vertex shrVertex = shrTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, shrVertex, LocalVertex.LABEL, "name", "2").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, shrVertex, LocalVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "4").has("typeFullName", "INTEGER").hasNext());
        final GraphTraversal<Vertex, Vertex> orTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "OR").has("typeFullName", "INTEGER");
        assertTrue(orTraversal.hasNext());
        final Vertex orVertex = orTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, orVertex, LocalVertex.LABEL, "name", "2").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, orVertex, LocalVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "7").has("typeFullName", "INTEGER").hasNext());
        final GraphTraversal<Vertex, Vertex> remTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "REM").has("typeFullName", "INTEGER");
        assertTrue(remTraversal.hasNext());
        final Vertex remVertex = remTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, remVertex, LocalVertex.LABEL, "name", "2").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, remVertex, LocalVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());
    }

    @Test
    public void arithmetic6Test() {
        assertTrue(buildStoreTraversal(g, EdgeLabels.AST, methodRoot).hasNext());
        assertEquals(4, buildStoreTraversal(g, EdgeLabels.AST, methodRoot).count().next());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LiteralVertex.LABEL, "name", "0").has("typeFullName", "INTEGER").hasNext());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "2").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LiteralVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "4").has("typeFullName", "INTEGER").hasNext());
        final GraphTraversal<Vertex, Vertex> ushrTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "USHR").has("typeFullName", "INTEGER");
        assertTrue(ushrTraversal.hasNext());
        final Vertex ushrVertex = ushrTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ushrVertex, LocalVertex.LABEL, "name", "2").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ushrVertex, LocalVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "3").has("typeFullName", "INTEGER").hasNext());
        final GraphTraversal<Vertex, Vertex> xorTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "XOR").has("typeFullName", "INTEGER");
        assertTrue(xorTraversal.hasNext());
        final Vertex xorVertex = xorTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, xorVertex, LocalVertex.LABEL, "name", "2").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, xorVertex, LocalVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());
    }

    @Test
    public void arithmetic7Test() {
        assertTrue(buildStoreTraversal(g, EdgeLabels.AST, methodRoot).hasNext());
        assertEquals(6, buildStoreTraversal(g, EdgeLabels.AST, methodRoot).count().next());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LiteralVertex.LABEL, "name", "0").has("typeFullName", "INTEGER").hasNext());

        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LocalVertex.LABEL, "name", "2").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, LiteralVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());

        final GraphTraversal<Vertex, Vertex> incOne = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "ADD").has("typeFullName", "INTEGER").has("order", 21);
        assertTrue(incOne.hasNext());
        final Vertex incOneVertex = incOne.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, incOneVertex, LocalVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, incOneVertex, LiteralVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());

        final GraphTraversal<Vertex, Vertex> decOne = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "SUB").has("typeFullName", "INTEGER").has("order", 26);
        assertTrue(decOne.hasNext());
        final Vertex decOneVertex = decOne.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, decOneVertex, LocalVertex.LABEL, "name", "2").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, decOneVertex, LiteralVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());

        final GraphTraversal<Vertex, Vertex> incTwo = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "ADD").has("typeFullName", "INTEGER").has("order", 31);
        assertTrue(incTwo.hasNext());
        final Vertex incTwoVertex = incTwo.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, incTwoVertex, LocalVertex.LABEL, "name", "1").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, incTwoVertex, LiteralVertex.LABEL, "name", "2").has("typeFullName", "INTEGER").hasNext());

        final GraphTraversal<Vertex, Vertex> decThree = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "SUB").has("typeFullName", "INTEGER").has("order", 36);
        assertTrue(decThree.hasNext());
        final Vertex decThreeVertex = decThree.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, decThreeVertex, LocalVertex.LABEL, "name", "2").has("typeFullName", "INTEGER").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, decThreeVertex, LiteralVertex.LABEL, "name", "3").has("typeFullName", "INTEGER").hasNext());
    }

}

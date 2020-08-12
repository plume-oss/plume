package za.ac.sun.plume;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.junit.jupiter.api.*;
import za.ac.sun.plume.domain.enums.EdgeLabels;
import za.ac.sun.plume.domain.enums.VertexLabels;
import za.ac.sun.plume.drivers.TinkerGraphDriver;
import za.ac.sun.plume.intraprocedural.BasicIntraproceduralTest;
import za.ac.sun.plume.intraprocedural.ConditionalIntraproceduralTest;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static za.ac.sun.plume.util.TestQueryBuilderUtil.buildStoreTraversal;
import static za.ac.sun.plume.util.TestQueryBuilderUtil.getVertexAlongEdge;

public class ExtractorTest {

    final static Logger logger = LogManager.getLogger();

    private static final String TEST_DIR = "/tmp/plume/plume-extractor-test.xml";
    private static Extractor extractor;
    private static File validSourceFile;
    private static File validClassFile;
    private static File validDirectory;
    private static File validJarFile;
    private static TinkerGraphDriver driver;

    private static File getTestResource(String dir) {
        final URL resourceURL = ExtractorTest.class.getClassLoader().getResource(dir);
        String fullURL = Objects.requireNonNull(resourceURL).getFile();
        return new File(fullURL);
    }

    @BeforeAll
    static void setUpAll() {
        validSourceFile = getTestResource("extractor_tests/Test1.java");
        validClassFile = getTestResource("extractor_tests/Test2.class");
        validJarFile = getTestResource("extractor_tests/Test3.jar");
        validDirectory = getTestResource("extractor_tests/dir_test");
        driver = new TinkerGraphDriver.Builder().build();
        extractor = new Extractor(driver);
    }

    @AfterEach
    void tearDown() {
        driver.clearGraph();
    }

    @AfterAll
    static void tearDownAll() {
        File f = new File(TEST_DIR);
        if (f.exists()) {
            if (!f.delete()) {
                logger.warn("Could not clear " + ConditionalIntraproceduralTest.class.getName() + "'s test resources.");
            }
        }
    }

    @Test
    public void validSourceFileTest() throws IOException {
        extractor.load(validSourceFile);
        extractor.project();
        driver.exportCurrentGraph(TEST_DIR);
    }

    @Test
    public void validClassFileTest() throws IOException {
        extractor.load(validClassFile);
        extractor.project();
        driver.exportCurrentGraph(TEST_DIR);
    }

    @Test
    public void validDirectoryTest() throws IOException {
        extractor.load(validDirectory);
        extractor.project();
        driver.exportCurrentGraph(TEST_DIR);
    }

    @Test
    public void validJarTest() throws IOException {
        GraphTraversalSource g = TinkerGraph.open().traversal();
        extractor.load(validJarFile);
        extractor.project();
        driver.exportCurrentGraph(TEST_DIR);
        g.io(TEST_DIR).read().iterate();

        // This is za.ac.sun.plume.intraprocedural.Basic6's test in a JAR
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

        BasicIntraproceduralTest.testBasic1Structure(g, basicNamespaceVertex);
        BasicIntraproceduralTest.testBasic1Structure(g, basic6NamespaceVertex);
    }

    @Test
    public void loadNullFileTest() {
        assertThrows(IllegalArgumentException.class, () -> extractor.load(null));
    }

    @Test
    public void loadFileThatDoesNotExistTest() {
        assertThrows(NullPointerException.class, () -> extractor.load(new File("dne.class")));
    }

}

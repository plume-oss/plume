package za.ac.sun.plume.interprocedural;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.junit.jupiter.api.*;
import za.ac.sun.plume.Extractor;
import za.ac.sun.plume.TestConstants;
import za.ac.sun.plume.drivers.TinkerGraphDriver;
import za.ac.sun.plume.intraprocedural.ArithmeticTest;
import za.ac.sun.plume.util.ResourceCompilationUtil;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

@Disabled
public class BasicInterproceduralTest {

    final static Logger logger = LogManager.getLogger(BasicInterproceduralTest.class);

    private static final File PATH;
    private static final File CLS_PATH;
    private static final String TEST_PATH = "interprocedural" + File.separator + "basic";
    private static final String TEST_GRAPH = TestConstants.INSTANCE.getTestGraph();

    static {
        PATH = new File(Objects.requireNonNull(ArithmeticTest.class.getClassLoader().getResource(TEST_PATH)).getFile());
        CLS_PATH = new File(PATH.getAbsolutePath().replace(System.getProperty("user.dir") + File.separator, "").replace(TEST_PATH, ""));
    }

    private GraphTraversalSource g;

    @AfterAll
    static void tearDownAll() throws IOException {
        ResourceCompilationUtil.deleteClassFiles(PATH);
        File f = new File(TEST_GRAPH);
        if (f.exists() && !f.delete()) {
            logger.warn("Could not clear " + ArithmeticTest.class.getName() + "'s test resources.");
        }
    }

    @BeforeEach
    public void setUp(final TestInfo testInfo) throws IOException {
        final TinkerGraphDriver driver = new TinkerGraphDriver.Builder().build();
        final Extractor extractor = new Extractor(driver, CLS_PATH);
        // Select test resource based on integer in method name
        final String currentTestNumber = testInfo.getDisplayName().replaceAll("[^0-9]", "");
        String resourceDir = PATH.getAbsolutePath().concat("/Basic").concat(currentTestNumber).concat(".java");
        // Load test resource and project + export graph
        final File f = new File(resourceDir);
        extractor.load(f);
        extractor.project();
        driver.exportCurrentGraph(TEST_GRAPH);

        g = TinkerGraph.open().traversal();
        g.io(TEST_GRAPH).read().iterate();
    }

    @Test
    public void basicCall1Test() {
    }

    @Test
    public void basicCall2Test() {
    }

    @Test
    public void basicCall3Test() {
    }

    @Test
    public void basicCall4Test() {
    }

    @Test
    public void basicCall5Test() {
    }

}

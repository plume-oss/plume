package za.ac.sun.plume.interprocedural;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import za.ac.sun.plume.Extractor;
import za.ac.sun.plume.drivers.TinkerGraphDriver;
import za.ac.sun.plume.intraprocedural.ArithmeticTest;
import za.ac.sun.plume.util.ResourceCompilationUtil;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class BasicInterproceduralTest {

    final static Logger logger = LogManager.getLogger();

    private static final File PATH;
    private static final String TEST_DIR = "/tmp/plume/plume-extractor-test.xml";
    private Extractor extractor;

    static {
        PATH = new File(Objects.requireNonNull(ArithmeticTest.class.getClassLoader().getResource("interprocedural/basic/")).getFile());
    }

    private GraphTraversalSource g;

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
        extractor = new Extractor(hook);
        // Select test resource based on integer in method name
        final String currentTestNumber = testInfo.getDisplayName().replaceAll("[^0-9]", "");
        String resourceDir = PATH.getAbsolutePath().concat("/Basic").concat(currentTestNumber).concat(".java");
        // Load test resource and project + export graph
        File f = new File(resourceDir);
        extractor.load(f);
        extractor.project();
        hook.exportCurrentGraph(TEST_DIR);

        g = TinkerGraph.open().traversal();
        g.io(TEST_DIR).read().iterate();
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

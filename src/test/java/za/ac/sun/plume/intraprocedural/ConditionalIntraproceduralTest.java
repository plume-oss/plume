package za.ac.sun.plume.intraprocedural;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.junit.jupiter.api.*;
import za.ac.sun.plume.Extractor;
import za.ac.sun.plume.domain.enums.EdgeLabels;
import za.ac.sun.plume.domain.enums.Equality;
import za.ac.sun.plume.domain.models.vertices.BlockVertex;
import za.ac.sun.plume.domain.models.vertices.ControlStructureVertex;
import za.ac.sun.plume.domain.models.vertices.LiteralVertex;
import za.ac.sun.plume.domain.models.vertices.LocalVertex;
import za.ac.sun.plume.drivers.TinkerGraphDriver;
import za.ac.sun.plume.util.ResourceCompilationUtil;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static za.ac.sun.plume.util.TestQueryBuilderUtil.getVertexAlongEdge;
import static za.ac.sun.plume.util.TestQueryBuilderUtil.getVertexAlongEdgeFixed;

@Disabled
public class ConditionalIntraproceduralTest {

    final static Logger logger = LogManager.getLogger();

    private static final File PATH;
    private static final String TEST_DIR = "/tmp/plume/plume-extractor-test.xml";
    private GraphTraversalSource g;
    private Vertex methodRoot;

    static {
        PATH = new File(Objects.requireNonNull(ArithmeticTest.class.getClassLoader().getResource("intraprocedural/conditional")).getFile());
    }

    @AfterAll
    static void tearDownAll() throws IOException {
        ResourceCompilationUtil.deleteClassFiles(PATH);
        File f = new File(TEST_DIR);
        if (f.exists()) {
            if (!f.delete()) {
                logger.warn("Could not clear " + ConditionalIntraproceduralTest.class.getName() + "'s test resources.");
            }
        }
    }

    @BeforeEach
    public void setUp(TestInfo testInfo) throws IOException {
        final TinkerGraphDriver hook = new TinkerGraphDriver.Builder().build();
        final Extractor fileCannon = new Extractor(hook);
        // Select test resource based on integer in method name
        final String currentTestNumber = testInfo
                .getDisplayName()
                .replaceAll("[^0-9]", "");
        String resourceDir = PATH.getAbsolutePath().concat("/Conditional").concat(currentTestNumber).concat(".java");
        // Load test resource and project + export graph
        File f = new File(resourceDir);
        fileCannon.load(f);
        fileCannon.project();
        hook.exportCurrentGraph(TEST_DIR);

        g = TinkerGraph.open().traversal();
        g.io(TEST_DIR).read().iterate();

        final GraphTraversal<Vertex, Vertex> methodTraversal = g.V()
                .has("METHOD", "fullName", "intraprocedural.conditional.Conditional"
                        .concat(currentTestNumber).concat(".main"));
        assertTrue(methodTraversal.hasNext());
        methodRoot = methodTraversal.next();
    }

    @Test
    public void conditional1Test() {
        // Get conditional root
        final GraphTraversal<Vertex, Vertex> ifRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, ControlStructureVertex.LABEL, "name", "IF");
        assertTrue(ifRootTraversal.hasNext());
        final Vertex ifRoot = ifRootTraversal.next();
        // Check if branch
        final GraphTraversal<Vertex, Vertex> ifBodyTraversal = g.V(ifRoot).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "IF_BODY");
        assertTrue(ifBodyTraversal.hasNext());
        final Vertex ifBody = ifBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, LocalVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, BlockVertex.LABEL, "name", "SUB").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, LocalVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, BlockVertex.LABEL, "name", "SUB").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, LocalVertex.LABEL, "name", "2").hasNext());
        // Check else branch
        final GraphTraversal<Vertex, Vertex> ifElseTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", "ELSE_BODY");
        assertTrue(ifElseTraversal.hasNext());
        final Vertex elseBody = ifElseTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, BlockVertex.LABEL, "name", "ADD").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, LocalVertex.LABEL, "name", "1").hasNext());
        // Check condition branch
        final GraphTraversal<Vertex, Vertex> ifConditionTraversal =
                getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", Equality.GT.name());
        assertTrue(ifConditionTraversal.hasNext());
        final Vertex ifCondition = ifConditionTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifCondition, LocalVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifCondition, LocalVertex.LABEL, "name", "2").hasNext());
    }

    @Test
    public void conditional2Test() {
        final GraphTraversal<Vertex, Vertex> methodStoreRootTraversal =
                getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "STORE").has("order", 34);
        // This test is a modified version of Conditional 1, just test changes
        assertTrue(methodStoreRootTraversal.hasNext());
        final Vertex methodStoreRoot = methodStoreRootTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodStoreRoot, BlockVertex.LABEL, "name", "ADD").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodStoreRoot, LocalVertex.LABEL, "name", "2").hasNext());
        final GraphTraversal<Vertex, Vertex> storeOpTraversal =
                getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "ADD");
        assertTrue(storeOpTraversal.hasNext());
        final Vertex storeOp = storeOpTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, storeOp, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, storeOp, LocalVertex.LABEL, "name", "1").hasNext());
        // Get conditional root
        final GraphTraversal<Vertex, Vertex> ifRootTraversal =
                getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, ControlStructureVertex.LABEL, "name", "IF");
        assertTrue(ifRootTraversal.hasNext());
        Vertex ifRoot = ifRootTraversal.next();
        // Check no else branch exists
        assertFalse(getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", "ELSE_BODY").hasNext());
    }

    @Test
    public void conditional3Test() {
        // This test is a modified version of Conditional 1, just test changes
        // Get conditional root
        final GraphTraversal<Vertex, Vertex> ifRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, ControlStructureVertex.LABEL, "name", "IF");
        assertTrue(ifRootTraversal.hasNext());
        final Vertex ifRoot = ifRootTraversal.next();
        // Check mul op under else body
        final GraphTraversal<Vertex, Vertex> elseBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", "ELSE_BODY");
        assertTrue(elseBodyTraversal.hasNext());
        final Vertex elseBody = elseBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, BlockVertex.LABEL, "name", "MUL").hasNext());
        // Check the method operation still remains under method
        final GraphTraversal<Vertex, Vertex> methodStoreRootBody = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "STORE").has("order", 41);
        assertTrue(methodStoreRootBody.hasNext());
        final Vertex methodStoreRoot = methodStoreRootBody.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodStoreRoot, BlockVertex.LABEL, "name", "ADD").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodStoreRoot, LocalVertex.LABEL, "name", "2").hasNext());
        final GraphTraversal<Vertex, Vertex> storeOpBody = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "ADD");
        assertTrue(storeOpBody.hasNext());
        final Vertex storeOp = storeOpBody.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, storeOp, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, storeOp, LocalVertex.LABEL, "name", "1").hasNext());
    }

    @Test
    public void conditional4Test() {
        // Get conditional root
        final GraphTraversal<Vertex, Vertex> ifRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, ControlStructureVertex.LABEL, "name", "IF");
        assertTrue(ifRootTraversal.hasNext());
        final Vertex ifRoot = ifRootTraversal.next();
        // Check if branch
        final GraphTraversal<Vertex, Vertex> ifBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "IF_BODY");
        assertTrue(ifBodyTraversal.hasNext());
        final Vertex ifBody = ifBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, LocalVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, BlockVertex.LABEL, "name", "SUB").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, LocalVertex.LABEL, "name", "1").hasNext());
        // Check if-if branch
        final GraphTraversal<Vertex, Vertex> ifIfRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifBody, ControlStructureVertex.LABEL, "name", "IF");
        assertTrue(ifIfRootTraversal.hasNext());
        final Vertex ifIfRoot = ifIfRootTraversal.next();
        // Check if-body branch
        final GraphTraversal<Vertex, Vertex> ifIfBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifIfRoot, BlockVertex.LABEL, "name", "IF_BODY");
        assertTrue(ifIfBodyTraversal.hasNext());
        final Vertex ifIfBody = ifIfBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifIfBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifIfBody, BlockVertex.LABEL, "name", "SUB").hasNext());
        // Check if-if condition branch
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", Equality.GT.name()).hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, LocalVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, LocalVertex.LABEL, "name", "2").hasNext());
        // Check else branch
        final GraphTraversal<Vertex, Vertex> elseBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", "ELSE_BODY");
        assertTrue(elseBodyTraversal.hasNext());
        final Vertex elseBody = elseBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, BlockVertex.LABEL, "name", "MUL").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, LocalVertex.LABEL, "name", "1").hasNext());
        // Check condition branch
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", Equality.EQ.name()).hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, LocalVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, LocalVertex.LABEL, "name", "2").hasNext());
        // Check method-level operation
        final GraphTraversal<Vertex, Vertex> methodStoreRootTraversal =
                getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "STORE")
                        .has("order", 46);
        assertTrue(methodStoreRootTraversal.hasNext());
        final Vertex methodStoreRoot = methodStoreRootTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodStoreRoot, BlockVertex.LABEL, "name", "ADD").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodStoreRoot, LocalVertex.LABEL, "name", "2").hasNext());
        final GraphTraversal<Vertex, Vertex> storeOpTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "ADD");
        assertTrue(storeOpTraversal.hasNext());
        final Vertex storeOp = storeOpTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, storeOp, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, storeOp, LocalVertex.LABEL, "name", "1").hasNext());
    }

    @Test
    public void conditional5Test() {
        // This test is a modified version of Conditional 4
        // Get conditional root
        final GraphTraversal<Vertex, Vertex> ifRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, ControlStructureVertex.LABEL, "name", "IF");
        assertTrue(ifRootTraversal.hasNext());
        final Vertex ifRoot = ifRootTraversal.next();
        // Check if branch
        final GraphTraversal<Vertex, Vertex> ifBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "IF_BODY");
        assertTrue(ifBodyTraversal.hasNext());
        final Vertex ifBody = ifBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, LocalVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, BlockVertex.LABEL, "name", "SUB").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, LocalVertex.LABEL, "name", "1").hasNext());
        // Check if-if branch
        final GraphTraversal<Vertex, Vertex> ifIfRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifBody, ControlStructureVertex.LABEL, "name", "IF");
        assertTrue(ifIfRootTraversal.hasNext());
        final Vertex ifIfRoot = ifIfRootTraversal.next();
        // Check if-if-body branch
        final GraphTraversal<Vertex, Vertex> ifIfBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifIfRoot, BlockVertex.LABEL, "name", "IF_BODY");
        assertTrue(ifIfBodyTraversal.hasNext());
        final Vertex ifIfBody = ifIfBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifIfBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifIfBody, BlockVertex.LABEL, "name", "SUB").hasNext());
        // Check if-else-body branch
        final GraphTraversal<Vertex, Vertex> ifElseBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifIfRoot, BlockVertex.LABEL, "name", "ELSE_BODY");
        assertTrue(ifElseBodyTraversal.hasNext());
        final Vertex ifElseBody = ifElseBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifElseBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifElseBody, BlockVertex.LABEL, "name", "DIV").hasNext());
        // Check if-if condition branch
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", Equality.GT.name()).hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, LocalVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, LocalVertex.LABEL, "name", "2").hasNext());
        // Check else branch
        final GraphTraversal<Vertex, Vertex> elseBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", "ELSE_BODY");
        assertTrue(elseBodyTraversal.hasNext());
        final Vertex elseBody = elseBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, BlockVertex.LABEL, "name", "MUL").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, LocalVertex.LABEL, "name", "1").hasNext());
        // Check condition branch
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", Equality.EQ.name()).hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, LocalVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, LocalVertex.LABEL, "name", "2").hasNext());
        // Check method-level operation
        final GraphTraversal<Vertex, Vertex> methodStoreRootTraversal =
                getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "STORE").has("order", 53);
        assertTrue(methodStoreRootTraversal.hasNext());
        final Vertex methodStoreRoot = methodStoreRootTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodStoreRoot, BlockVertex.LABEL, "name", "ADD").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodStoreRoot, LocalVertex.LABEL, "name", "2").hasNext());
        final GraphTraversal<Vertex, Vertex> storeOpTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "ADD");
        assertTrue(storeOpTraversal.hasNext());
        final Vertex storeOp = storeOpTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, storeOp, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, storeOp, LocalVertex.LABEL, "name", "1").hasNext());
    }

    @Test
    public void conditional6Test() {
        // This test is a modified version of Conditional 5
        // Get conditional root
        final GraphTraversal<Vertex, Vertex> ifRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, ControlStructureVertex.LABEL, "name", "IF");
        assertTrue(ifRootTraversal.hasNext());
        final Vertex ifRoot = ifRootTraversal.next();
        // Check if branch
        final GraphTraversal<Vertex, Vertex> ifBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "IF_BODY");
        assertTrue(ifBodyTraversal.hasNext());
        final Vertex ifBody = ifBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, LocalVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, BlockVertex.LABEL, "name", "SUB").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, LocalVertex.LABEL, "name", "1").hasNext());
        // Check else branch
        final GraphTraversal<Vertex, Vertex> elseBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", "ELSE_BODY");
        assertTrue(elseBodyTraversal.hasNext());
        final Vertex elseBody = elseBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, BlockVertex.LABEL, "name", "MUL").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, LocalVertex.LABEL, "name", "1").hasNext());
        // Check else-if branch
        final GraphTraversal<Vertex, Vertex> elseIfRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, elseBody, ControlStructureVertex.LABEL, "name", "IF");
        assertTrue(elseIfRootTraversal.hasNext());
        final Vertex elseIfRoot = elseIfRootTraversal.next();
        // Check else-if-body branch
        final GraphTraversal<Vertex, Vertex> elseIfBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, elseIfRoot, BlockVertex.LABEL, "name", "IF_BODY");
        assertTrue(elseIfBodyTraversal.hasNext());
        final Vertex elseIfBody = elseIfBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseIfBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseIfBody, BlockVertex.LABEL, "name", "SUB").hasNext());
        // Check else-else-body branch
        final GraphTraversal<Vertex, Vertex> elseElseBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, elseIfRoot, BlockVertex.LABEL, "name", "ELSE_BODY");
        assertTrue(elseElseBodyTraversal.hasNext());
        final Vertex ifElseBody = elseElseBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifElseBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifElseBody, BlockVertex.LABEL, "name", "DIV").hasNext());
        // Check else-if condition branch
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", Equality.GT.name()).hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, LocalVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, LocalVertex.LABEL, "name", "2").hasNext());
        // Check condition branch
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", Equality.EQ.name()).hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, LocalVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, LocalVertex.LABEL, "name", "2").hasNext());
        // Check method-level operation
        final GraphTraversal<Vertex, Vertex> methodStoreRootTraversal =
                getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "STORE").has("order",  53);
        assertTrue(methodStoreRootTraversal.hasNext());
        final Vertex methodStoreRoot = methodStoreRootTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodStoreRoot, BlockVertex.LABEL, "name", "ADD").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodStoreRoot, LocalVertex.LABEL, "name", "2").hasNext());
        final GraphTraversal<Vertex, Vertex> storeOpTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "ADD");
        assertTrue(storeOpTraversal.hasNext());
        final Vertex storeOp = storeOpTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, storeOp, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, storeOp, LocalVertex.LABEL, "name", "1").hasNext());
    }

    @Test
    public void conditional7Test() {
        // This test is a modified version of Conditional 6
        // Get conditional root
        final GraphTraversal<Vertex, Vertex> ifRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, ControlStructureVertex.LABEL, "name", "IF");
        assertTrue(ifRootTraversal.hasNext());
        final Vertex ifRoot = ifRootTraversal.next();
        // Check if branch
        final GraphTraversal<Vertex, Vertex> ifBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "IF_BODY");
        assertTrue(ifBodyTraversal.hasNext());
        final Vertex ifBody = ifBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, LocalVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, BlockVertex.LABEL, "name", "SUB").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, LocalVertex.LABEL, "name", "1").hasNext());
        // Check if-if branch
        final GraphTraversal<Vertex, Vertex> ifIfRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifBody, ControlStructureVertex.LABEL, "name", "IF");
        assertTrue(ifIfRootTraversal.hasNext());
        final Vertex ifIfRoot = ifIfRootTraversal.next();
        // Check if-if-body branch
        final GraphTraversal<Vertex, Vertex> ifIfBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifIfRoot, BlockVertex.LABEL, "name", "IF_BODY");
        assertTrue(ifIfBodyTraversal.hasNext());
        final Vertex ifIfBody = ifIfBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifIfBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifIfBody, BlockVertex.LABEL, "name", "MUL").hasNext());
        // Check else branch
        final GraphTraversal<Vertex, Vertex> elseBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", "ELSE_BODY");
        assertTrue(elseBodyTraversal.hasNext());
        final Vertex elseBody = elseBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, BlockVertex.LABEL, "name", "MUL").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, LocalVertex.LABEL, "name", "1").hasNext());
        // Check else-if branch
        final GraphTraversal<Vertex, Vertex> elseIfRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, elseBody, ControlStructureVertex.LABEL, "name", "IF");
        assertTrue(elseIfRootTraversal.hasNext());
        final Vertex elseIfRoot = elseIfRootTraversal.next();
        // Check else-if-body branch
        final GraphTraversal<Vertex, Vertex> elseIfBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, elseIfRoot, BlockVertex.LABEL, "name", "IF_BODY");
        assertTrue(elseIfBodyTraversal.hasNext());
        final Vertex elseIfBody = elseIfBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseIfBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseIfBody, BlockVertex.LABEL, "name", "SUB").hasNext());
        // Check else-else-body branch
        final GraphTraversal<Vertex, Vertex> elseElseBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, elseIfRoot, BlockVertex.LABEL, "name", "ELSE_BODY");
        assertTrue(elseElseBodyTraversal.hasNext());
        final Vertex ifElseBody = elseElseBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifElseBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifElseBody, BlockVertex.LABEL, "name", "DIV").hasNext());
        // Check else-if condition branch
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", Equality.GT.name()).hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, LocalVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, LocalVertex.LABEL, "name", "2").hasNext());
        // Check condition branch
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", Equality.EQ.name()).hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, LocalVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, LocalVertex.LABEL, "name", "2").hasNext());
    }

    @Test
    public void conditional8Test() {
        // Get conditional root
        final GraphTraversal<Vertex, Vertex> ifRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, ControlStructureVertex.LABEL, "name", "IF");
        assertTrue(ifRootTraversal.hasNext());
        final Vertex ifRoot = ifRootTraversal.next();
        // Check if branch
        final GraphTraversal<Vertex, Vertex> ifBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", "IF_BODY");
        assertTrue(ifBodyTraversal.hasNext());
        final Vertex ifBody = ifBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, LocalVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, BlockVertex.LABEL, "name", "SUB").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, LocalVertex.LABEL, "name", "1").hasNext());
        // Check if-if branch
        final GraphTraversal<Vertex, Vertex> ifIfRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifBody, ControlStructureVertex.LABEL, "name", "IF");
        assertTrue(ifIfRootTraversal.hasNext());
        final Vertex ifIfRoot = ifIfRootTraversal.next();
        // Check if-if-body branch
        final GraphTraversal<Vertex, Vertex> ifIfBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifIfRoot, BlockVertex.LABEL, "name", "IF_BODY");
        assertTrue(ifIfBodyTraversal.hasNext());
        final Vertex ifIfBody = ifIfBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifIfBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifIfBody, BlockVertex.LABEL, "name", "MUL").hasNext());
        // Check if-else-body branch
        final GraphTraversal<Vertex, Vertex> ifElseBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifIfRoot, BlockVertex.LABEL, "name", "ELSE_BODY");
        assertTrue(ifElseBodyTraversal.hasNext());
        final Vertex ifElseBody = ifElseBodyTraversal.next();
        // Check if-else-if branch
        final GraphTraversal<Vertex, Vertex> ifElseIfRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifElseBody, ControlStructureVertex.LABEL, "name", "IF");
        assertTrue(ifIfRootTraversal.hasNext());
        final Vertex ifElseIfRoot = ifElseIfRootTraversal.next();
        // Check if-else-if-body branch
        final GraphTraversal<Vertex, Vertex> ifElseIfBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifElseIfRoot, BlockVertex.LABEL, "name", "IF_BODY");
        assertTrue(ifElseIfBodyTraversal.hasNext());
        final Vertex ifElseIfBody = ifElseIfBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifElseIfBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifElseIfBody, BlockVertex.LABEL, "name", "DIV").hasNext());
    }

    @Test
    public void conditional9Test() {
        // This is Conditional 8 with a symmetrical else body minus a SUB operation
        // Get conditional root
        final GraphTraversal<Vertex, Vertex> ifRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, ControlStructureVertex.LABEL, "name", "IF");
        assertTrue(ifRootTraversal.hasNext());
        final Vertex ifRoot = ifRootTraversal.next();
        // Check if branch
        final GraphTraversal<Vertex, Vertex> ifBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", "IF_BODY");
        assertTrue(ifBodyTraversal.hasNext());
        final Vertex ifBody = ifBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, LocalVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, BlockVertex.LABEL, "name", "SUB").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifBody, LocalVertex.LABEL, "name", "1").hasNext());
        // Check if-if branch
        final GraphTraversal<Vertex, Vertex> ifIfRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifBody, ControlStructureVertex.LABEL, "name", "IF");
        assertTrue(ifIfRootTraversal.hasNext());
        final Vertex ifIfRoot = ifIfRootTraversal.next();
        // Check if-if-body branch
        final GraphTraversal<Vertex, Vertex> ifIfBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifIfRoot, BlockVertex.LABEL, "name", "IF_BODY");
        assertTrue(ifIfBodyTraversal.hasNext());
        final Vertex ifIfBody = ifIfBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifIfBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifIfBody, BlockVertex.LABEL, "name", "MUL").hasNext());
        // Check if-else-body branch
        final GraphTraversal<Vertex, Vertex> ifElseBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifIfRoot, BlockVertex.LABEL, "name", "ELSE_BODY");
        assertTrue(ifElseBodyTraversal.hasNext());
        final Vertex ifElseBody = ifElseBodyTraversal.next();
        // Check if-else-if branch
        final GraphTraversal<Vertex, Vertex> ifElseIfRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifElseBody, ControlStructureVertex.LABEL, "name", "IF");
        assertTrue(ifIfRootTraversal.hasNext());
        final Vertex ifElseIfRoot = ifElseIfRootTraversal.next();
        // Check if-else-if-body branch
        final GraphTraversal<Vertex, Vertex> ifElseIfBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifElseIfRoot, BlockVertex.LABEL, "name", "IF_BODY");
        assertTrue(ifElseIfBodyTraversal.hasNext());
        final Vertex ifElseIfBody = ifElseIfBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifElseIfBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifElseIfBody, BlockVertex.LABEL, "name", "DIV").hasNext());
        // Check else branch
        final GraphTraversal<Vertex, Vertex> elseBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", "ELSE_BODY");
        assertTrue(elseBodyTraversal.hasNext());
        final Vertex elseBody = elseBodyTraversal.next();
        // Check else-if branch
        final GraphTraversal<Vertex, Vertex> elseIfRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, elseBody, ControlStructureVertex.LABEL, "name", "IF");
        assertTrue(elseIfRootTraversal.hasNext());
        final Vertex elseIfRoot = elseIfRootTraversal.next();
        // Check else-if-body branch
        final GraphTraversal<Vertex, Vertex> elseIfBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, elseIfRoot, BlockVertex.LABEL, "name", "IF_BODY");
        assertTrue(elseIfBodyTraversal.hasNext());
        final Vertex elseIfBody = elseIfBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseIfBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseIfBody, BlockVertex.LABEL, "name", "MUL").hasNext());
        // Check else-else-body branch
        final GraphTraversal<Vertex, Vertex> elseElseBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, elseBody, BlockVertex.LABEL, "name", "ELSE_BODY");
        assertTrue(elseElseBodyTraversal.hasNext());
        final Vertex elseElseBody = elseElseBodyTraversal.next();
        // Check else-else-if branch
        final GraphTraversal<Vertex, Vertex> elseElseIfRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, elseElseBody, ControlStructureVertex.LABEL, "name", "IF");
        assertTrue(elseElseIfRootTraversal.hasNext());
        final Vertex elseElseIfRoot = elseElseIfRootTraversal.next();
        // Check if-else-if-body branch
        final GraphTraversal<Vertex, Vertex> elseElseIfBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, elseElseIfRoot, BlockVertex.LABEL, "name", "IF_BODY");
        assertTrue(elseElseIfBodyTraversal.hasNext());
        final Vertex elseElseIfBody = elseElseIfBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseElseIfBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseElseIfBody, BlockVertex.LABEL, "name", "DIV").hasNext());
    }

    @Test
    public void conditional10Test() {
        // Get conditional root
        final GraphTraversal<Vertex, Vertex> ifRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, ControlStructureVertex.LABEL, "name", "IF");
        assertTrue(ifRootTraversal.hasNext());
        final Vertex ifRoot = ifRootTraversal.next();
        // Check empty if branch
        final GraphTraversal<Vertex, Vertex> ifBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", "IF_BODY");
        assertFalse(ifBodyTraversal.hasNext());
        // Check else branch
        final GraphTraversal<Vertex, Vertex> elseBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", "ELSE_BODY");
        assertTrue(elseBodyTraversal.hasNext());
        final Vertex elseBody = elseBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseBody, BlockVertex.LABEL, "name", "MUL").hasNext());
    }

    @Test
    public void conditional11Test() {
        // This test is a modified version of Conditional 3
        // Get conditional root
        final GraphTraversal<Vertex, Vertex> ifRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, ControlStructureVertex.LABEL, "name", "IF").has("order", 19);
        assertTrue(ifRootTraversal.hasNext());
        final Vertex ifRoot = ifRootTraversal.next();
        // Check mul op under else body
        final GraphTraversal<Vertex, Vertex> elseBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", "ELSE_BODY").has("order", 35);
        assertTrue(elseBodyTraversal.hasNext());
        final Vertex elseBody = elseBodyTraversal.next();
        // Check the else-if root
        final GraphTraversal<Vertex, Vertex> elseIfRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, elseBody, ControlStructureVertex.LABEL, "name", "IF").has("order", 36);
        assertTrue(elseIfRootTraversal.hasNext());
        final Vertex elseIfRoot = elseIfRootTraversal.next();
        // Check the else-if-body branch
        final GraphTraversal<Vertex, Vertex> elseIfBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, elseIfRoot, BlockVertex.LABEL, "name", "IF_BODY").has("order", 38);
        assertTrue(elseIfBodyTraversal.hasNext());
        final Vertex elseIfBody = elseIfBodyTraversal.next();
        final GraphTraversal<Vertex, Vertex> elseIfStoreOpTraversal = getVertexAlongEdge(g, EdgeLabels.AST, elseIfBody, BlockVertex.LABEL, "name", "STORE");
        assertTrue(elseIfStoreOpTraversal.hasNext());
        final Vertex elseIfStoreOp = elseIfStoreOpTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseIfStoreOp, BlockVertex.LABEL, "name", "MUL").hasNext());
        // Check the else-else-body branch
        final GraphTraversal<Vertex, Vertex> elseElseBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, elseIfRoot, BlockVertex.LABEL, "name", "ELSE_BODY").has("order", 47);
        assertTrue(elseElseBodyTraversal.hasNext());
        final Vertex elseElseBody = elseElseBodyTraversal.next();
        final GraphTraversal<Vertex, Vertex> elseElseStoreOpTraversal = getVertexAlongEdge(g, EdgeLabels.AST, elseElseBody, BlockVertex.LABEL, "name", "STORE");
        assertTrue(elseElseStoreOpTraversal.hasNext());
        final Vertex elseElseStoreOp = elseElseStoreOpTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseElseStoreOp, BlockVertex.LABEL, "name", "DIV").hasNext());
        // Check the method operation still remains under method
        final GraphTraversal<Vertex, Vertex> methodStoreRootBody = getVertexAlongEdgeFixed(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "STORE", 2).has("order", 53);
        assertTrue(methodStoreRootBody.hasNext());
        final Vertex methodStoreRoot = methodStoreRootBody.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodStoreRoot, BlockVertex.LABEL, "name", "ADD").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodStoreRoot, LocalVertex.LABEL, "name", "2").hasNext());
        final GraphTraversal<Vertex, Vertex> storeOpBody = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "ADD");
        assertTrue(storeOpBody.hasNext());
        final Vertex storeOp = storeOpBody.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, storeOp, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, storeOp, LocalVertex.LABEL, "name", "1").hasNext());
    }

    @Test
    public void conditional12Test() {
        // This test is a modified version of Conditional 11
        // Get conditional root
        final GraphTraversal<Vertex, Vertex> ifRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, ControlStructureVertex.LABEL, "name", "IF").has("order", 19);
        assertTrue(ifRootTraversal.hasNext());
        final Vertex ifRoot = ifRootTraversal.next();
        // Check mul op under else body
        final GraphTraversal<Vertex, Vertex> elseBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", "ELSE_BODY").has("order", 35);
        assertTrue(elseBodyTraversal.hasNext());
        final Vertex elseBody = elseBodyTraversal.next();
        // Check the else-if root
        final GraphTraversal<Vertex, Vertex> elseIfRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, elseBody, ControlStructureVertex.LABEL, "name", "IF").has("order", 36);
        assertTrue(elseIfRootTraversal.hasNext());
        final Vertex elseIfRoot = elseIfRootTraversal.next();
        // Check the else-if-body branch
        final GraphTraversal<Vertex, Vertex> elseIfBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, elseIfRoot, BlockVertex.LABEL, "name", "IF_BODY").has("order", 38);
        assertTrue(elseIfBodyTraversal.hasNext());
        final Vertex elseIfBody = elseIfBodyTraversal.next();
        final GraphTraversal<Vertex, Vertex> elseIfStoreOpTraversal = getVertexAlongEdge(g, EdgeLabels.AST, elseIfBody, BlockVertex.LABEL, "name", "STORE");
        assertTrue(elseIfStoreOpTraversal.hasNext());
        final Vertex elseIfStoreOp = elseIfStoreOpTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseIfStoreOp, BlockVertex.LABEL, "name", "MUL").hasNext());
        // Check the else-else-body branch
        final GraphTraversal<Vertex, Vertex> elseElseBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, elseIfRoot, BlockVertex.LABEL, "name", "ELSE_BODY").has("order", 47);
        assertTrue(elseElseBodyTraversal.hasNext());
        final Vertex elseElseBody = elseElseBodyTraversal.next();
        // Check the else-else-if-body branch
        final GraphTraversal<Vertex, Vertex> elseElseIfBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, elseElseBody, BlockVertex.LABEL, "name", "IF_BODY").has("order", 50);
        assertTrue(elseElseIfBodyTraversal.hasNext());
        final Vertex elseElseIfBody = elseElseIfBodyTraversal.next();
        final GraphTraversal<Vertex, Vertex> elseElseStoreOpTraversal = getVertexAlongEdge(g, EdgeLabels.AST, elseElseIfBody, BlockVertex.LABEL, "name", "STORE");
        assertTrue(elseElseStoreOpTraversal.hasNext());
        final Vertex elseElseStoreOp = elseElseStoreOpTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseElseStoreOp, BlockVertex.LABEL, "name", "DIV").hasNext());
        // Check the method operation still remains under method
        final GraphTraversal<Vertex, Vertex> methodStoreRootBody = getVertexAlongEdgeFixed(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "STORE", 3).has("order", 58);
        assertTrue(methodStoreRootBody.hasNext());
        final Vertex methodStoreRoot = methodStoreRootBody.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodStoreRoot, BlockVertex.LABEL, "name", "ADD").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodStoreRoot, LocalVertex.LABEL, "name", "2").hasNext());
        final GraphTraversal<Vertex, Vertex> storeOpBody = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "ADD");
        assertTrue(storeOpBody.hasNext());
        final Vertex storeOp = storeOpBody.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, storeOp, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, storeOp, LocalVertex.LABEL, "name", "1").hasNext());
    }

    @Test
    public void conditional13Test() {
        // This test is a modified version of Conditional 11
        // Get conditional root
        final GraphTraversal<Vertex, Vertex> ifRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, ControlStructureVertex.LABEL, "name", "IF").has("order", 19);
        assertTrue(ifRootTraversal.hasNext());
        final Vertex ifRoot = ifRootTraversal.next();
        // Check the if-body branch
        final GraphTraversal<Vertex, Vertex> ifBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", "IF_BODY").has("order", 21);
        assertTrue(ifBodyTraversal.hasNext());
        final Vertex ifBody = ifBodyTraversal.next();
        // Check the if-if-body branch
        final GraphTraversal<Vertex, Vertex> ifIfBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifBody, BlockVertex.LABEL, "name", "IF_BODY").has("order", 31);
        assertTrue(ifIfBodyTraversal.hasNext());
        final Vertex ifIfBody = ifIfBodyTraversal.next();
        final GraphTraversal<Vertex, Vertex> ifIfStoreOpTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifIfBody, BlockVertex.LABEL, "name", "STORE");
        assertTrue(ifIfStoreOpTraversal.hasNext());
        final Vertex ifIfStoreOp = ifIfStoreOpTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifIfStoreOp, BlockVertex.LABEL, "name", "SUB").hasNext());
        // Check mul op under else body
        final GraphTraversal<Vertex, Vertex> elseBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", "ELSE_BODY").has("order", 40);
        assertTrue(elseBodyTraversal.hasNext());
        final Vertex elseBody = elseBodyTraversal.next();
        // Check the else-if root
        final GraphTraversal<Vertex, Vertex> elseIfRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, elseBody, ControlStructureVertex.LABEL, "name", "IF").has("order", 41);
        assertTrue(elseIfRootTraversal.hasNext());
        final Vertex elseIfRoot = elseIfRootTraversal.next();
        // Check the else-if-body branch
        final GraphTraversal<Vertex, Vertex> elseIfBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, elseIfRoot, BlockVertex.LABEL, "name", "IF_BODY").has("order", 43);
        assertTrue(elseIfBodyTraversal.hasNext());
        final Vertex elseIfBody = elseIfBodyTraversal.next();
        final GraphTraversal<Vertex, Vertex> elseIfStoreOpTraversal = getVertexAlongEdge(g, EdgeLabels.AST, elseIfBody, BlockVertex.LABEL, "name", "STORE");
        assertTrue(elseIfStoreOpTraversal.hasNext());
        final Vertex elseIfStoreOp = elseIfStoreOpTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseIfStoreOp, BlockVertex.LABEL, "name", "MUL").hasNext());
        // Check the else-else-body branch
        final GraphTraversal<Vertex, Vertex> elseElseBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, elseIfRoot, BlockVertex.LABEL, "name", "ELSE_BODY").has("order", 52);
        assertTrue(elseElseBodyTraversal.hasNext());
        final Vertex elseElseBody = elseElseBodyTraversal.next();
        final GraphTraversal<Vertex, Vertex> elseElseStoreOpTraversal = getVertexAlongEdge(g, EdgeLabels.AST, elseElseBody, BlockVertex.LABEL, "name", "STORE");
        assertTrue(elseElseStoreOpTraversal.hasNext());
        final Vertex elseElseStoreOp = elseElseStoreOpTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseElseStoreOp, BlockVertex.LABEL, "name", "DIV").hasNext());
        // Check the method operation still remains under method
        final GraphTraversal<Vertex, Vertex> methodStoreRootBody = getVertexAlongEdgeFixed(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "STORE", 3).has("order", 58);
        assertTrue(methodStoreRootBody.hasNext());
        final Vertex methodStoreRoot = methodStoreRootBody.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodStoreRoot, BlockVertex.LABEL, "name", "ADD").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, methodStoreRoot, LocalVertex.LABEL, "name", "2").hasNext());
        final GraphTraversal<Vertex, Vertex> storeOpBody = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "ADD");
        assertTrue(storeOpBody.hasNext());
        final Vertex storeOp = storeOpBody.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, storeOp, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, storeOp, LocalVertex.LABEL, "name", "1").hasNext());
    }

    @Test
    public void conditional14Test() {
        // This test is a modified version of Conditional 11
        // Get conditional root
        final GraphTraversal<Vertex, Vertex> ifRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, ControlStructureVertex.LABEL, "name", "IF").has("order", 19);
        assertTrue(ifRootTraversal.hasNext());
        final Vertex ifRoot = ifRootTraversal.next();
        // Check the if-body branch
        final GraphTraversal<Vertex, Vertex> ifBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", "IF_BODY").has("order", 21);
        assertTrue(ifBodyTraversal.hasNext());
        final Vertex ifBody = ifBodyTraversal.next();
        final GraphTraversal<Vertex, Vertex> ifIfStoreOpTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifBody, BlockVertex.LABEL, "name", "STORE");
        assertTrue(ifIfStoreOpTraversal.hasNext());
        final Vertex ifIfStoreOp = ifIfStoreOpTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, ifIfStoreOp, BlockVertex.LABEL, "name", "SUB").hasNext());
        // Check mul op under else body
        final GraphTraversal<Vertex, Vertex> elseBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", "ELSE_BODY").has("order", 35);
        assertTrue(elseBodyTraversal.hasNext());
        final Vertex elseBody = elseBodyTraversal.next();
        // Check the else-if root
        final GraphTraversal<Vertex, Vertex> elseIfRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, elseBody, ControlStructureVertex.LABEL, "name", "IF").has("order", 36);
        assertTrue(elseIfRootTraversal.hasNext());
        final Vertex elseIfRoot = elseIfRootTraversal.next();
        // Check the else-if-body branch
        final GraphTraversal<Vertex, Vertex> elseIfBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, elseIfRoot, BlockVertex.LABEL, "name", "IF_BODY").has("order", 38);
        assertTrue(elseIfBodyTraversal.hasNext());
        final Vertex elseIfBody = elseIfBodyTraversal.next();
        final GraphTraversal<Vertex, Vertex> elseIfStoreOpTraversal = getVertexAlongEdge(g, EdgeLabels.AST, elseIfBody, BlockVertex.LABEL, "name", "STORE");
        assertTrue(elseIfStoreOpTraversal.hasNext());
        final Vertex elseIfStoreOp = elseIfStoreOpTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, elseIfStoreOp, BlockVertex.LABEL, "name", "MUL").hasNext());
    }

    @Test
    public void conditional15Test() {
        // Get store root
        final GraphTraversal<Vertex, Vertex> storeTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "STORE").has("order", 23);
        assertTrue(storeTraversal.hasNext());
        final Vertex storeVertex = storeTraversal.next();
        // Left child
        final GraphTraversal<Vertex, Vertex> localVarTraversal = getVertexAlongEdgeFixed(g, EdgeLabels.AST, storeVertex, LocalVertex.LABEL, "name", "3", 1).has("order", 28);
        assertTrue(localVarTraversal.hasNext());
        // Get conditional root
        final GraphTraversal<Vertex, Vertex> ifRootTraversal = getVertexAlongEdgeFixed(g, EdgeLabels.AST, storeVertex, ControlStructureVertex.LABEL, "name", "IF", 3).has("order", 19);
        assertTrue(ifRootTraversal.hasNext());
        final Vertex ifRoot = ifRootTraversal.next();
        // Check the if-body branch
        final GraphTraversal<Vertex, Vertex> ifBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", "IF_BODY").has("order", 24);
        assertTrue(ifBodyTraversal.hasNext());
        final Vertex ifBody = ifBodyTraversal.next();
        final GraphTraversal<Vertex, Vertex> ifBodyLiteral = getVertexAlongEdge(g, EdgeLabels.AST, ifBody, LiteralVertex.LABEL, "name", "2");
        assertTrue(ifBodyLiteral.hasNext());
        // Check the else-body branch
        final GraphTraversal<Vertex, Vertex> elseBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", "ELSE_BODY").has("order", 26);
        assertTrue(elseBodyTraversal.hasNext());
        final Vertex elseBody = elseBodyTraversal.next();
        final GraphTraversal<Vertex, Vertex> elseBodyLiteral = getVertexAlongEdge(g, EdgeLabels.AST, elseBody, LiteralVertex.LABEL, "name", "1");
        assertTrue(elseBodyLiteral.hasNext());
    }

    @Test
    public void conditional16Test() {
        // Get store root
        final GraphTraversal<Vertex, Vertex> storeTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "STORE").has("order", 27);
        assertTrue(storeTraversal.hasNext());
        final Vertex storeVertex = storeTraversal.next();
        // Left child
        final GraphTraversal<Vertex, Vertex> localVarTraversal = getVertexAlongEdgeFixed(g, EdgeLabels.AST, storeVertex, LocalVertex.LABEL, "name", "3", 1).has("order", 35);
        assertTrue(localVarTraversal.hasNext());
        // Get conditional root
        final GraphTraversal<Vertex, Vertex> ifRootTraversal = getVertexAlongEdgeFixed(g, EdgeLabels.AST, storeVertex, ControlStructureVertex.LABEL, "name", "IF", 3).has("order", 19);
        assertTrue(ifRootTraversal.hasNext());
        final Vertex ifRoot = ifRootTraversal.next();
        // Check the if-body branch
        final GraphTraversal<Vertex, Vertex> ifBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", "IF_BODY").has("order", 28);
        assertTrue(ifBodyTraversal.hasNext());
        final Vertex ifBody = ifBodyTraversal.next();
        // Check the if-if-root
        final GraphTraversal<Vertex, Vertex> ifIfRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifBody, ControlStructureVertex.LABEL, "name", "IF").has("order", 23);
        assertTrue(ifIfRootTraversal.hasNext());
        final Vertex ifIfRoot = ifIfRootTraversal.next();
        // Check the if-if-body branch
        final GraphTraversal<Vertex, Vertex> ifIfBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifIfRoot, BlockVertex.LABEL, "name", "IF_BODY").has("order", 29);
        assertTrue(ifIfBodyTraversal.hasNext());
        final Vertex ifIfBody = ifIfBodyTraversal.next();
        final GraphTraversal<Vertex, Vertex> ifBodyLiteral = getVertexAlongEdge(g, EdgeLabels.AST, ifIfBody, LiteralVertex.LABEL, "name", "2");
        assertTrue(ifBodyLiteral.hasNext());
        // Check the if-else-body branch
        final GraphTraversal<Vertex, Vertex> ifElseBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifIfRoot, BlockVertex.LABEL, "name", "ELSE_BODY").has("order", 31);
        assertTrue(ifElseBodyTraversal.hasNext());
        final Vertex ifElseBody = ifElseBodyTraversal.next();
        final GraphTraversal<Vertex, Vertex> ifElseBodyLiteral = getVertexAlongEdge(g, EdgeLabels.AST, ifElseBody, LiteralVertex.LABEL, "name", "4");
        assertTrue(ifElseBodyLiteral.hasNext());
        // Check the else-body branch
        final GraphTraversal<Vertex, Vertex> elseBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", "ELSE_BODY").has("order", 33);
        assertTrue(elseBodyTraversal.hasNext());
        final Vertex elseBody = elseBodyTraversal.next();
        final GraphTraversal<Vertex, Vertex> elseBodyLiteral = getVertexAlongEdge(g, EdgeLabels.AST, elseBody, LiteralVertex.LABEL, "name", "1");
        assertTrue(elseBodyLiteral.hasNext());
    }

    @Test
    public void conditional17Test() {
        // Get store root
        final GraphTraversal<Vertex, Vertex> storeTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "STORE").has("order", 27);
        assertTrue(storeTraversal.hasNext());
        final Vertex storeVertex = storeTraversal.next();
        // Left child
        final GraphTraversal<Vertex, Vertex> localVarTraversal = getVertexAlongEdgeFixed(g, EdgeLabels.AST, storeVertex, LocalVertex.LABEL, "name", "3", 1).has("order", 35);
        assertTrue(localVarTraversal.hasNext());
        // Get conditional root
        final GraphTraversal<Vertex, Vertex> ifRootTraversal = getVertexAlongEdgeFixed(g, EdgeLabels.AST, storeVertex, ControlStructureVertex.LABEL, "name", "IF", 3).has("order", 19);
        assertTrue(ifRootTraversal.hasNext());
        final Vertex ifRoot = ifRootTraversal.next();
        // Check the if-body branch
        final GraphTraversal<Vertex, Vertex> ifBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", "IF_BODY").has("order", 28);
        assertTrue(ifBodyTraversal.hasNext());
        final Vertex ifBody = ifBodyTraversal.next();
        final GraphTraversal<Vertex, Vertex> elseBodyLiteral = getVertexAlongEdge(g, EdgeLabels.AST, ifBody, LocalVertex.LABEL, "name", "1");
        assertTrue(elseBodyLiteral.hasNext());
        // Check the else-body branch
        final GraphTraversal<Vertex, Vertex> elseBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifRoot, BlockVertex.LABEL, "name", "ELSE_BODY").has("order", 30);
        assertTrue(elseBodyTraversal.hasNext());
        final Vertex elseBody = elseBodyTraversal.next();
        // Check the else-if-root
        final GraphTraversal<Vertex, Vertex> ifIfRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, elseBody, ControlStructureVertex.LABEL, "name", "IF").has("order", 23);
        assertTrue(ifIfRootTraversal.hasNext());
        final Vertex ifIfRoot = ifIfRootTraversal.next();
        // Check the else-if-body branch
        final GraphTraversal<Vertex, Vertex> ifIfBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifIfRoot, BlockVertex.LABEL, "name", "IF_BODY").has("order", 31);
        assertTrue(ifIfBodyTraversal.hasNext());
        final Vertex ifIfBody = ifIfBodyTraversal.next();
        final GraphTraversal<Vertex, Vertex> ifBodyLiteral = getVertexAlongEdge(g, EdgeLabels.AST, ifIfBody, LiteralVertex.LABEL, "name", "2");
        assertTrue(ifBodyLiteral.hasNext());
        // Check the else-else-body branch
        final GraphTraversal<Vertex, Vertex> ifElseBodyTraversal = getVertexAlongEdge(g, EdgeLabels.AST, ifIfRoot, BlockVertex.LABEL, "name", "ELSE_BODY").has("order", 33);
        assertTrue(ifElseBodyTraversal.hasNext());
        final Vertex ifElseBody = ifElseBodyTraversal.next();
        final GraphTraversal<Vertex, Vertex> ifElseBodyLiteral = getVertexAlongEdge(g, EdgeLabels.AST, ifElseBody, LiteralVertex.LABEL, "name", "4");
        assertTrue(ifElseBodyLiteral.hasNext());

    }

}

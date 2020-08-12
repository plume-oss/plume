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

public class LoopIntraproceduralTest {

    final static Logger logger = LogManager.getLogger();

    private static final File PATH;
    private static final String TEST_DIR = "/tmp/plume/plume-extractor-test.xml";
    private GraphTraversalSource g;
    private Vertex methodRoot;

    static {
        PATH = new File(Objects.requireNonNull(ArithmeticTest.class.getClassLoader().getResource("intraprocedural/loop")).getFile());
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
        String resourceDir = PATH.getAbsolutePath().concat("/Loop").concat(currentTestNumber).concat(".java");
        // Load test resource and project + export graph
        File f = new File(resourceDir);
        fileCannon.load(f);
        fileCannon.project();
        hook.exportCurrentGraph(TEST_DIR);

        g = TinkerGraph.open().traversal();
        g.io(TEST_DIR).read().iterate();

        final GraphTraversal<Vertex, Vertex> methodTraversal = g.V()
                .has("METHOD", "fullName", "intraprocedural.loop.Loop"
                        .concat(currentTestNumber).concat(".main"));
        assertTrue(methodTraversal.hasNext());
        methodRoot = methodTraversal.next();
    }

    @Test
    public void loop1Test() {
        // Get conditional root
        final GraphTraversal<Vertex, Vertex> whileRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, ControlStructureVertex.LABEL, "name", "WHILE");
        assertTrue(whileRootTraversal.hasNext());
        final Vertex whileRoot = whileRootTraversal.next();
        // Check while branch
        final GraphTraversal<Vertex, Vertex> whileBodyTraversal = g.V(whileRoot).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "IF_BODY").has("order", 21);
        assertTrue(whileBodyTraversal.hasNext());
        final Vertex whileBody = whileBodyTraversal.next();
        assertTrue(getVertexAlongEdgeFixed(g, EdgeLabels.AST, whileBody, BlockVertex.LABEL, "name", "STORE", 1).hasNext());
        assertTrue(getVertexAlongEdgeFixed(g, EdgeLabels.AST, whileBody, LocalVertex.LABEL, "name", "1", 2).hasNext());
        assertTrue(getVertexAlongEdgeFixed(g, EdgeLabels.AST, whileBody, BlockVertex.LABEL, "name", "ADD", 2).hasNext());
        assertTrue(getVertexAlongEdgeFixed(g, EdgeLabels.AST, whileBody, LiteralVertex.LABEL, "name", "1", 3).hasNext());
        assertTrue(getVertexAlongEdgeFixed(g, EdgeLabels.AST, whileBody, LocalVertex.LABEL, "name", "1", 3).hasNext());
    }

    @Test
    public void loop2Test() {
        // Get conditional root
        final GraphTraversal<Vertex, Vertex> whileRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, ControlStructureVertex.LABEL, "name", "DO_WHILE");
        assertTrue(whileRootTraversal.hasNext());
        final Vertex whileRoot = whileRootTraversal.next();
        // Check that there is no IF
        final GraphTraversal<Vertex, Vertex> ifRootCheckTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "IF");
        assertFalse(ifRootCheckTraversal.hasNext());
        // Check while branch
        final GraphTraversal<Vertex, Vertex> whileBodyTraversal = g.V(whileRoot).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "IF_BODY").has("order", 20);
        assertTrue(whileBodyTraversal.hasNext());
        final Vertex whileBody = whileBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileBody, LocalVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileBody, BlockVertex.LABEL, "name", "ADD").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileBody, LiteralVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileBody, LocalVertex.LABEL, "name", "1").hasNext());
    }

    @Test
    public void loop3Test() {
        // Get conditional root
        final GraphTraversal<Vertex, Vertex> whileRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, ControlStructureVertex.LABEL, "name", "WHILE");
        assertTrue(whileRootTraversal.hasNext());
        final Vertex whileRoot = whileRootTraversal.next();
        // Check while branch
        final GraphTraversal<Vertex, Vertex> whileBodyTraversal = g.V(whileRoot).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "IF_BODY").has("order", 21
                );
        assertTrue(whileBodyTraversal.hasNext());
        final Vertex whileBody = whileBodyTraversal.next();
        assertTrue(getVertexAlongEdgeFixed(g, EdgeLabels.AST, whileBody, BlockVertex.LABEL, "name", "STORE", 1).hasNext());
        assertTrue(getVertexAlongEdgeFixed(g, EdgeLabels.AST, whileBody, LocalVertex.LABEL, "name", "1", 2).hasNext());
        assertTrue(getVertexAlongEdgeFixed(g, EdgeLabels.AST, whileBody, BlockVertex.LABEL, "name", "ADD", 2).hasNext());
        assertTrue(getVertexAlongEdgeFixed(g, EdgeLabels.AST, whileBody, LiteralVertex.LABEL, "name", "1", 3).hasNext());
        assertTrue(getVertexAlongEdgeFixed(g, EdgeLabels.AST, whileBody, LocalVertex.LABEL, "name", "1", 3).hasNext());
        // Check method level store
        final GraphTraversal<Vertex, Vertex> postWhileStoreTraversal = getVertexAlongEdgeFixed(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "STORE", 2);
        assertTrue(postWhileStoreTraversal.hasNext());
        final Vertex postWhileStoreVertex = postWhileStoreTraversal.next();
        assertTrue(getVertexAlongEdgeFixed(g, EdgeLabels.AST, postWhileStoreVertex, LiteralVertex.LABEL, "name", "3", 1).hasNext());
        assertTrue(getVertexAlongEdgeFixed(g, EdgeLabels.AST, postWhileStoreVertex, LocalVertex.LABEL, "name", "2", 1).hasNext());
    }

    @Test
    public void loop4Test() {
        // Get conditional root
        final GraphTraversal<Vertex, Vertex> whileRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, ControlStructureVertex.LABEL, "name", "DO_WHILE");
        assertTrue(whileRootTraversal.hasNext());
        final Vertex whileRoot = whileRootTraversal.next();
        // Check while branch
        final GraphTraversal<Vertex, Vertex> whileBodyTraversal = g.V(whileRoot).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "IF_BODY").has("order", 20);
        assertTrue(whileBodyTraversal.hasNext());
        final Vertex whileBody = whileBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileBody, LocalVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileBody, BlockVertex.LABEL, "name", "ADD").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileBody, LiteralVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileBody, LocalVertex.LABEL, "name", "1").hasNext());
        // Check method level store
        final GraphTraversal<Vertex, Vertex> postWhileStoreTraversal = getVertexAlongEdgeFixed(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "order", 29, 2);
        assertTrue(postWhileStoreTraversal.hasNext());
        final Vertex postWhileStoreVertex = postWhileStoreTraversal.next();
        assertTrue(getVertexAlongEdgeFixed(g, EdgeLabels.AST, postWhileStoreVertex, LiteralVertex.LABEL, "name", "3", 1).hasNext());
        assertTrue(getVertexAlongEdgeFixed(g, EdgeLabels.AST, postWhileStoreVertex, LocalVertex.LABEL, "name", "2", 1).hasNext());
    }

    @Test
    public void loop5Test() {
        // Get conditional root
        final GraphTraversal<Vertex, Vertex> whileRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, ControlStructureVertex.LABEL, "name", "DO_WHILE");
        assertTrue(whileRootTraversal.hasNext());
        final Vertex whileRoot = whileRootTraversal.next();
        // Check while branch
        final GraphTraversal<Vertex, Vertex> whileBodyTraversal = g.V(whileRoot).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "IF_BODY").has("order", 20);
        assertTrue(whileBodyTraversal.hasNext());
        final Vertex whileBody = whileBodyTraversal.next();
        // Check nested-while branch
        final GraphTraversal<Vertex, Vertex> whileWhileBodyTraversal = g.V(whileBody).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "IF_BODY").has("order", 22);
        assertTrue(whileWhileBodyTraversal.hasNext());
        final Vertex whileWhileBody = whileWhileBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileWhileBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileWhileBody, LocalVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileWhileBody, BlockVertex.LABEL, "name", "ADD").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileWhileBody, LiteralVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileWhileBody, LocalVertex.LABEL, "name", "1").hasNext());
        // Check method level store
        final GraphTraversal<Vertex, Vertex> postWhileStoreTraversal = getVertexAlongEdgeFixed(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "STORE", 1).has("order", 34);
        assertTrue(postWhileStoreTraversal.hasNext());
        final Vertex postWhileStoreVertex = postWhileStoreTraversal.next();
        assertTrue(getVertexAlongEdgeFixed(g, EdgeLabels.AST, postWhileStoreVertex, LiteralVertex.LABEL, "name", "3", 1).hasNext());
        assertTrue(getVertexAlongEdgeFixed(g, EdgeLabels.AST, postWhileStoreVertex, LocalVertex.LABEL, "name", "2", 1).hasNext());
    }

    @Test
    public void loop6Test() {
        // Get conditional root
        final GraphTraversal<Vertex, Vertex> whileRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, ControlStructureVertex.LABEL, "name", "WHILE");
        assertTrue(whileRootTraversal.hasNext());
        final Vertex whileRoot = whileRootTraversal.next();
        // Check while branch
        final GraphTraversal<Vertex, Vertex> whileBodyTraversal = g.V(whileRoot).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "IF_BODY").has("order", 21);
        assertTrue(whileBodyTraversal.hasNext());
        final Vertex whileBody = whileBodyTraversal.next();
        // Check nested-while branch
        final GraphTraversal<Vertex, Vertex> whileWhileBodyTraversal = g.V(whileBody).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "IF_BODY").has("order", 26);
        assertTrue(whileWhileBodyTraversal.hasNext());
        final Vertex whileWhileBody = whileWhileBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileWhileBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileWhileBody, LocalVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileWhileBody, BlockVertex.LABEL, "name", "ADD").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileWhileBody, LiteralVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileWhileBody, LocalVertex.LABEL, "name", "1").hasNext());
        // Check method level store
        final GraphTraversal<Vertex, Vertex> postWhileStoreTraversal = getVertexAlongEdgeFixed(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "STORE", 1).has("order", 34);
        assertTrue(postWhileStoreTraversal.hasNext());
        final Vertex postWhileStoreVertex = postWhileStoreTraversal.next();
        assertTrue(getVertexAlongEdgeFixed(g, EdgeLabels.AST, postWhileStoreVertex, LiteralVertex.LABEL, "name", "3", 1).hasNext());
        assertTrue(getVertexAlongEdgeFixed(g, EdgeLabels.AST, postWhileStoreVertex, LocalVertex.LABEL, "name", "2", 1).hasNext());
    }

    @Test
    public void loop7Test() {
        // Get conditional root
        final GraphTraversal<Vertex, Vertex> whileRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, ControlStructureVertex.LABEL, "name", "DO_WHILE");
        assertTrue(whileRootTraversal.hasNext());
        final Vertex whileRoot = whileRootTraversal.next();
        // Check while branch
        final GraphTraversal<Vertex, Vertex> whileBodyTraversal = g.V(whileRoot).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "IF_BODY").has("order", 20);
        assertTrue(whileBodyTraversal.hasNext());
        final Vertex whileBody = whileBodyTraversal.next();
        // Check nested-while branch
        final GraphTraversal<Vertex, Vertex> whileWhileBodyTraversal = g.V(whileBody).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "IF_BODY").has("order", 23);
        assertTrue(whileWhileBodyTraversal.hasNext());
        final Vertex whileWhileBody = whileWhileBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileWhileBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileWhileBody, LocalVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileWhileBody, BlockVertex.LABEL, "name", "ADD").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileWhileBody, LiteralVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileWhileBody, LocalVertex.LABEL, "name", "1").hasNext());
        // Check method level store
        final GraphTraversal<Vertex, Vertex> postWhileStoreTraversal = getVertexAlongEdgeFixed(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "STORE", 1).has("order", 34);
        assertTrue(postWhileStoreTraversal.hasNext());
        final Vertex postWhileStoreVertex = postWhileStoreTraversal.next();
        assertTrue(getVertexAlongEdgeFixed(g, EdgeLabels.AST, postWhileStoreVertex, LiteralVertex.LABEL, "name", "3", 1).hasNext());
        assertTrue(getVertexAlongEdgeFixed(g, EdgeLabels.AST, postWhileStoreVertex, LocalVertex.LABEL, "name", "2", 1).hasNext());
    }

    @Test
    public void loop8Test() {
        // Get conditional root
        final GraphTraversal<Vertex, Vertex> whileRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, ControlStructureVertex.LABEL, "name", "WHILE");
        assertTrue(whileRootTraversal.hasNext());
        final Vertex whileRoot = whileRootTraversal.next();
        // Check while branch
        final GraphTraversal<Vertex, Vertex> whileBodyTraversal = g.V(whileRoot).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "IF_BODY").has("order", 21);
        assertTrue(whileBodyTraversal.hasNext());
        final Vertex whileBody = whileBodyTraversal.next();
        // Check nested-while branch
        final GraphTraversal<Vertex, Vertex> whileWhileBodyTraversal = g.V(whileBody).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "IF_BODY").has("order", 25);
        assertTrue(whileWhileBodyTraversal.hasNext());
        final Vertex whileWhileBody = whileWhileBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileWhileBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileWhileBody, LocalVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileWhileBody, BlockVertex.LABEL, "name", "ADD").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileWhileBody, LiteralVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileWhileBody, LocalVertex.LABEL, "name", "1").hasNext());
        // Check method level store
        final GraphTraversal<Vertex, Vertex> postWhileStoreTraversal = getVertexAlongEdgeFixed(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "STORE", 1).has("order", 34);
        assertTrue(postWhileStoreTraversal.hasNext());
        final Vertex postWhileStoreVertex = postWhileStoreTraversal.next();
        assertTrue(getVertexAlongEdgeFixed(g, EdgeLabels.AST, postWhileStoreVertex, LiteralVertex.LABEL, "name", "3", 1).hasNext());
        assertTrue(getVertexAlongEdgeFixed(g, EdgeLabels.AST, postWhileStoreVertex, LocalVertex.LABEL, "name", "2", 1).hasNext());
    }

    @Test
    public void loop9Test() {
        // Get conditional root
        final GraphTraversal<Vertex, Vertex> whileRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, ControlStructureVertex.LABEL, "name", "WHILE");
        assertTrue(whileRootTraversal.hasNext());
        final Vertex whileRoot = whileRootTraversal.next();
        // Check while branch
        final GraphTraversal<Vertex, Vertex> whileBodyTraversal = g.V(whileRoot).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "IF_BODY").has("order", 21);
        assertTrue(whileBodyTraversal.hasNext());
        final Vertex whileBody = whileBodyTraversal.next();
        // Check nested-while branch
        final GraphTraversal<Vertex, Vertex> whileWhileRootTraversal = g.V(whileBody).repeat(__.out("AST")).emit()
                .has(ControlStructureVertex.LABEL.toString(), "name", "DO_WHILE").has("order", 24);
        assertTrue(whileWhileRootTraversal.hasNext());
        final GraphTraversal<Vertex, Vertex> whileWhileBodyTraversal = g.V(whileBody).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "IF_BODY").has("order", 25);
        assertTrue(whileWhileBodyTraversal.hasNext());
        final Vertex whileWhileBody = whileWhileBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileWhileBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileWhileBody, LocalVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileWhileBody, BlockVertex.LABEL, "name", "ADD").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileWhileBody, LiteralVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileWhileBody, LocalVertex.LABEL, "name", "1").hasNext());
        // Check nested-while-while branch
        final GraphTraversal<Vertex, Vertex> whileWhileWhileRootTraversal = g.V(whileWhileBody).repeat(__.out("AST")).emit()
                .has(ControlStructureVertex.LABEL.toString(), "name", "WHILE").has("order", 31);
        assertTrue(whileWhileWhileRootTraversal.hasNext());
        final GraphTraversal<Vertex, Vertex> whileWhileWhileBodyTraversal = g.V(whileWhileBody).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "IF_BODY").has("order", 33);
        assertTrue(whileWhileWhileBodyTraversal.hasNext());
        final Vertex whileWhileWhileBody = whileWhileWhileBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileWhileWhileBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileWhileWhileBody, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileWhileWhileBody, BlockVertex.LABEL, "name", "ADD").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileWhileWhileBody, LiteralVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileWhileWhileBody, LocalVertex.LABEL, "name", "2").hasNext());
        // Check method level store
        final GraphTraversal<Vertex, Vertex> postWhileStoreTraversal = getVertexAlongEdgeFixed(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "STORE", 1).has("order", 44);
        assertTrue(postWhileStoreTraversal.hasNext());
        final Vertex postWhileStoreVertex = postWhileStoreTraversal.next();
        assertTrue(getVertexAlongEdgeFixed(g, EdgeLabels.AST, postWhileStoreVertex, LiteralVertex.LABEL, "name", "3", 1).hasNext());
        assertTrue(getVertexAlongEdgeFixed(g, EdgeLabels.AST, postWhileStoreVertex, LocalVertex.LABEL, "name", "2", 1).hasNext());
    }

    @Test
    public void loop10Test() {
        // Get conditional root
        final GraphTraversal<Vertex, Vertex> whileRootTraversal = getVertexAlongEdge(g, EdgeLabels.AST, methodRoot, ControlStructureVertex.LABEL, "name", "WHILE");
        assertTrue(whileRootTraversal.hasNext());
        final Vertex whileRoot = whileRootTraversal.next();
        // Check while branch
        final GraphTraversal<Vertex, Vertex> whileBodyTraversal = g.V(whileRoot).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "IF_BODY").has("order", 21);
        assertTrue(whileBodyTraversal.hasNext());
        final Vertex whileBody = whileBodyTraversal.next();
        // a = a - b;
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileBody, LocalVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileBody, BlockVertex.LABEL, "name", "SUB").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileBody, LocalVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileBody, LocalVertex.LABEL, "name", "2").hasNext());
        // Check nested-while branch
        final GraphTraversal<Vertex, Vertex> whileWhileRootTraversal = g.V(whileBody).repeat(__.out("AST")).emit()
                .has(ControlStructureVertex.LABEL.toString(), "name", "DO_WHILE").has("order", 29);
        assertTrue(whileWhileRootTraversal.hasNext());
        final GraphTraversal<Vertex, Vertex> whileWhileBodyTraversal = g.V(whileBody).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "IF_BODY").has("order", 30);
        assertTrue(whileWhileBodyTraversal.hasNext());
        final Vertex whileWhileBody = whileWhileBodyTraversal.next();
        // a ++;
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileWhileBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileWhileBody, LocalVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileWhileBody, BlockVertex.LABEL, "name", "ADD").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileWhileBody, LiteralVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileWhileBody, LocalVertex.LABEL, "name", "1").hasNext());
        // Check nested-while-while branch
        final GraphTraversal<Vertex, Vertex> whileWhileWhileRootTraversal = g.V(whileWhileBody).repeat(__.out("AST")).emit()
                .has(ControlStructureVertex.LABEL.toString(), "name", "WHILE").has("order", 36);
        assertTrue(whileWhileWhileRootTraversal.hasNext());
        final GraphTraversal<Vertex, Vertex> whileWhileWhileBodyTraversal = g.V(whileWhileBody).repeat(__.out("AST")).emit()
                .has(BlockVertex.LABEL.toString(), "name", "IF_BODY").has("order", 38);
        assertTrue(whileWhileWhileBodyTraversal.hasNext());
        final Vertex whileWhileWhileBody = whileWhileWhileBodyTraversal.next();
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileWhileWhileBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileWhileWhileBody, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileWhileWhileBody, BlockVertex.LABEL, "name", "ADD").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileWhileWhileBody, LiteralVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileWhileWhileBody, LocalVertex.LABEL, "name", "2").hasNext());
        // a = a + b
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileWhileBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileWhileBody, LocalVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileWhileBody, BlockVertex.LABEL, "name", "ADD").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileWhileBody, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileWhileBody, LocalVertex.LABEL, "name", "1").hasNext());
        // b = a / b;
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileBody, BlockVertex.LABEL, "name", "STORE").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileBody, LocalVertex.LABEL, "name", "2").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileBody, BlockVertex.LABEL, "name", "DIV").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileBody, LocalVertex.LABEL, "name", "1").hasNext());
        assertTrue(getVertexAlongEdge(g, EdgeLabels.AST, whileBody, LocalVertex.LABEL, "name", "2").hasNext());
        // Check method level store
        final GraphTraversal<Vertex, Vertex> postWhileStoreTraversal = getVertexAlongEdgeFixed(g, EdgeLabels.AST, methodRoot, BlockVertex.LABEL, "name", "STORE", 1).has("order", 59);
        assertTrue(postWhileStoreTraversal.hasNext());
        final Vertex postWhileStoreVertex = postWhileStoreTraversal.next();
        assertTrue(getVertexAlongEdgeFixed(g, EdgeLabels.AST, postWhileStoreVertex, LiteralVertex.LABEL, "name", "3", 1).hasNext());
        assertTrue(getVertexAlongEdgeFixed(g, EdgeLabels.AST, postWhileStoreVertex, LocalVertex.LABEL, "name", "2", 1).hasNext());
    }
}

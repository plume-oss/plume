package za.ac.sun.plume.hooks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.jetbrains.annotations.NotNull;
import za.ac.sun.plume.domain.enums.EdgeLabels;
import za.ac.sun.plume.domain.mappers.VertexMapper;
import za.ac.sun.plume.domain.models.GraPLVertex;
import za.ac.sun.plume.domain.models.MethodDescriptorVertex;
import za.ac.sun.plume.domain.models.vertices.FileVertex;
import za.ac.sun.plume.domain.models.vertices.MethodVertex;
import za.ac.sun.plume.domain.models.vertices.ModifierVertex;
import za.ac.sun.plume.domain.models.vertices.NamespaceBlockVertex;

import java.util.Map;
import java.util.UUID;

import static org.apache.tinkerpop.gremlin.process.traversal.Order.desc;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.inV;

public abstract class GremlinHook implements IHook {

    private static final Logger logger = LogManager.getLogger(GremlinHook.class);
    protected final Graph graph;
    protected GraphTraversalSource g;

    public GremlinHook(final Graph graph) {
        this.graph = graph;
    }

    protected void startTransaction() {
        g = graph.traversal();
    }

    protected void endTransaction() {
        try {
            this.g.close();
        } catch (Exception e) {
            logger.warn("Unable to close existing transaction! Object will be orphaned and a new traversal will continue.");
        }
    }

    public void close() {
        try {
            this.graph.close();
        } catch (Exception e) {
            logger.warn("Exception thrown while attempting to close graph.", e);
        }
    }

    public static boolean isValidExportPath(final String exportDir) {
        if (exportDir != null) {
            final String ext = exportDir.substring(exportDir.lastIndexOf('.') + 1).toLowerCase();
            return ("xml".equals(ext) || "json".equals(ext) || "kryo".equals(ext));
        } else return false;
    }

    /**
     * Finds the associated {@link Vertex} in the graph based on the given {@link MethodVertex}.
     *
     * @param from The {@link MethodVertex} to use in the search.
     * @return the associated {@link Vertex}.
     */
    private Vertex findVertex(final MethodVertex from) {
        return g.V().has(MethodVertex.LABEL.toString(), "fullName", from.getFullName())
                .has("signature", from.getSignature()).next();
    }

    /**
     * Finds the associated {@link Vertex} in the graph based on the given {@link FileVertex}.
     *
     * @param from The {@link FileVertex} to use in the search.
     * @return the associated {@link Vertex}.
     */
    private Vertex findVertex(final FileVertex from) {
        return g.V().has(FileVertex.LABEL.toString(), "name", from.getName())
                .has("order", from.getOrder()).next();
    }

    /**
     * Finds the associated {@link Vertex} in the graph based on the given {@link NamespaceBlockVertex}.
     *
     * @param from The {@link NamespaceBlockVertex} to use in the search.
     * @return the associated {@link Vertex}.
     */
    private Vertex findVertex(final NamespaceBlockVertex from) {
        return g.V().has(NamespaceBlockVertex.LABEL.toString(), "fullName", from.getFullName()).next();
    }

    /**
     * Checks if there is an associated {@link Vertex} with the given {@link NamespaceBlockVertex}.
     *
     * @param v the {@link NamespaceBlockVertex} to look up.
     * @return false if there is an associated vertex, true if otherwise.
     */
    private boolean vertexNotPresent(final NamespaceBlockVertex v) {
        return !g.V().has(NamespaceBlockVertex.LABEL.toString(), "fullName", v.getFullName())
                .has("name", v.getName()).hasNext();
    }

    /**
     * Checks if there is an associated {@link Vertex} with the given {@link FileVertex}.
     *
     * @param v the {@link FileVertex} to look up.
     * @return false if there is an associated vertex, true if otherwise.
     */
    private boolean vertexNotPresent(final FileVertex v) {
        return !g.V().has(FileVertex.LABEL.toString(), "name", v.getName())
                .has("order", v.getOrder()).hasNext();
    }

    @Override
    public void createAndAddToMethod(@NotNull final MethodVertex from, @NotNull final MethodDescriptorVertex to) {
        createAndJoinMethodToAnyAST(from, to);
    }

    @Override
    public void createAndAddToMethod(@NotNull final MethodVertex from, @NotNull final ModifierVertex to) {
        createAndJoinMethodToAnyAST(from, to);
    }

    private void createAndJoinMethodToAnyAST(final MethodVertex from, final GraPLVertex to) {
        startTransaction();
        createTinkerGraphEdge(findVertex(from), EdgeLabels.AST, createTinkerPopVertex(to));
        endTransaction();
    }

    @Override
    public void joinFileVertexTo(@NotNull final FileVertex to, @NotNull final NamespaceBlockVertex from) {
        startTransaction();
        if (vertexNotPresent(from)) {
            createTinkerPopVertex(from);
        }
        if (vertexNotPresent(to)) {
            createTinkerPopVertex(to);
        }
        createTinkerGraphEdge(findVertex(from), EdgeLabels.AST, findVertex(to));
        endTransaction();
    }

    @Override
    public void joinFileVertexTo(@NotNull final FileVertex from, @NotNull final MethodVertex to) {
        startTransaction();
        if (vertexNotPresent(from)) createTinkerPopVertex(from);
        if (!g.V(findVertex(from))
                .out(EdgeLabels.AST.toString())
                .has("fullName", to.getFullName())
                .has("signature", to.getSignature())
                .hasNext()) {
            createTinkerPopVertex(to);
        }
        createTinkerGraphEdge(findVertex(from), EdgeLabels.AST, findVertex(to));
        endTransaction();
    }

    @Override
    public void joinNamespaceBlocks(@NotNull final NamespaceBlockVertex from, @NotNull final NamespaceBlockVertex to) {
        startTransaction();
        if (vertexNotPresent(from)) createTinkerPopVertex(from);
        if (vertexNotPresent(to)) createTinkerPopVertex(to);
        Vertex n1 = findVertex(from);
        Vertex n2 = findVertex(to);
        if (!g.V(n1).outE(EdgeLabels.AST.toString()).filter(inV().is(n2)).hasNext()) {
            createTinkerGraphEdge(n1, EdgeLabels.AST, n2);
        }
        endTransaction();
    }

    @Override
    public void createAndAssignToBlock(@NotNull final MethodVertex parentVertex, @NotNull final GraPLVertex newVertex) {
        startTransaction();
        createTinkerGraphEdge(findASTVertex(parentVertex, parentVertex.getOrder()), EdgeLabels.AST, createTinkerPopVertex(newVertex));
        endTransaction();
    }

    @Override
    public void createAndAssignToBlock(@NotNull final GraPLVertex newVertex, final int blockOrder) {
        startTransaction();
        createTinkerGraphEdge(findASTVertex(blockOrder), EdgeLabels.AST, createTinkerPopVertex(newVertex));
        endTransaction();
    }

    @Override
    public void updateASTVertexProperty(final int order, @NotNull final String key, @NotNull final String value) {
        startTransaction();
        g.V(findASTVertex(order)).property(key, value).iterate();
        endTransaction();
    }

    @Override
    public void createVertex(@NotNull final GraPLVertex block) {
        startTransaction();
        createTinkerPopVertex(block);
        endTransaction();
    }

    @Override
    public void joinASTVerticesByOrder(final int blockFrom, final int blockTo, @NotNull final EdgeLabels edgeLabel) {
        startTransaction();
        createTinkerGraphEdge(findASTVertex(blockFrom), edgeLabel, findASTVertex(blockTo));
        endTransaction();
    }

    @Override
    public boolean areASTVerticesConnected(final int blockFrom, final int blockTo, final EdgeLabels edgeLabel) {
        startTransaction();
        final Vertex a = findASTVertex(blockFrom);
        final Vertex b = findASTVertex(blockTo);
        final Edge edge = g.V(a).outE(EdgeLabels.AST.toString()).filter(inV().is(b)).hasLabel(edgeLabel.name()).tryNext()
                .orElseGet(() -> g.V(b).outE(EdgeLabels.AST.toString()).filter(inV().is(a)).hasLabel(edgeLabel.name()).tryNext()
                        .orElse(null));
        endTransaction();
        return edge != null;
    }

    @Override
    public int maxOrder() {
        startTransaction();
        int result = 0;
        if (g.V().has("order").hasNext())
            result = (int) g.V().has("order").order().by("order", desc).limit(1).values("order").next();
        endTransaction();
        return result;
    }

    @Override
    public boolean isASTVertex(final int blockOrder) {
        startTransaction();
        boolean result = g.V().has("order", blockOrder).hasNext();
        endTransaction();
        return result;
    }

    protected void setTraversalSource(final GraphTraversalSource g) {
        this.g = g;
    }

    @Override
    public void clearGraph() {
        startTransaction();
        g.V().drop().iterate();
        endTransaction();
    }

    /**
     * Finds the associated {@link Vertex} in the graph to the block based on the {@link MethodVertex} and the AST order
     * under which this block occurs under this {@link MethodVertex}.
     *
     * @param root       the {@link MethodVertex} which is the root of the search.
     * @param blockOrder the AST order under which this block occurs.
     * @return the {@link Vertex} associated with the AST block.
     */
    private Vertex findASTVertex(final MethodVertex root, final int blockOrder) {
        if (root.getOrder() == blockOrder) return g.V(findVertex(root)).next();
        return g.V(findVertex(root)).repeat(__.out("AST")).emit()
                .has("order", blockOrder).next();
    }

    /**
     * Finds the associated {@link Vertex} in the graph to the block based on the AST order
     * under which this block occurs in the graph.
     *
     * @param order the AST order under which this block occurs.
     * @return the {@link Vertex} associated with the AST block.
     */
    private Vertex findASTVertex(final int order) {
        return g.V().has("order", order).next();
    }

    /**
     * Given a {@link GraPLVertex}, creates a {@link Vertex} and translates the object's field properties to key-value
     * pairs on the {@link Vertex} object. This is then added to this hook's {@link Graph}.
     *
     * @param gv the {@link GraPLVertex} to translate into a {@link Vertex}.
     * @return the newly created {@link Vertex}.
     */
    protected Vertex createTinkerPopVertex(final GraPLVertex gv) {
        final Map<String, Object> propertyMap = VertexMapper.propertiesToMap(gv);
        // Get the implementing class label parameter
        final String label = (String) propertyMap.remove("label");
        // Get the implementing classes fields and values
        final Vertex v = g.getGraph().addVertex(T.label, label, T.id, UUID.randomUUID());
        propertyMap.forEach(v::property);
        return v;
    }

    /**
     * Wrapper method for creating an edge between two vertices. This wrapper method assigns a random UUID as the ID
     * for the edge.
     *
     * @param v1        the from {@link Vertex}.
     * @param edgeLabel the CPG edge label.
     * @param v2        the to {@link Vertex}.
     * @return the newly created {@link Edge}.
     */
    private Edge createTinkerGraphEdge(final Vertex v1, final EdgeLabels edgeLabel, final Vertex v2) {

        if (this instanceof TinkerGraphHook) {
            return v1.addEdge(edgeLabel.name(), v2, T.id, UUID.randomUUID());
        } else {
            return g.V(v1.id()).addE(edgeLabel.name()).to(g.V(v2.id())).next();
        }
    }

}

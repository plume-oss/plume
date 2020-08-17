package za.ac.sun.plume.util;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import za.ac.sun.plume.domain.enums.EdgeLabels;
import za.ac.sun.plume.domain.enums.VertexLabels;
import za.ac.sun.plume.domain.models.vertices.BlockVertex;
import za.ac.sun.plume.domain.models.vertices.MethodParameterInVertex;
import za.ac.sun.plume.domain.models.vertices.MethodReturnVertex;
import za.ac.sun.plume.domain.models.vertices.ModifierVertex;

public class TestQueryBuilderUtil {

    public static GraphTraversal<Vertex, Vertex> buildASTRepeat(GraphTraversalSource g, EdgeLabels edge, Vertex rootVertex) {
        return g.V(rootVertex).repeat(__.out(edge.toString()));
    }

    public static GraphTraversal<Vertex, Vertex> getVertexAlongEdge(GraphTraversalSource g, EdgeLabels edge, Vertex rootVertex, VertexLabels label, String key, Object value) {
        return buildASTRepeat(g, edge, rootVertex).emit().has(label.toString(), key, value);
    }

    public static GraphTraversal<Vertex, Vertex> getVertexAlongEdgeFixed(GraphTraversalSource g, EdgeLabels edge, Vertex rootVertex, VertexLabels label, String key, Object value, int max) {
        return buildASTRepeat(g, edge, rootVertex).until(__.has(label.toString(), key, value).or().loops().is(max));
    }

    public static GraphTraversal<Vertex, Vertex> buildStoreTraversal(GraphTraversalSource g, EdgeLabels edge, Vertex rootVertex) {
        return getVertexAlongEdge(g, edge, rootVertex, BlockVertex.LABEL, "name", "STORE");
    }

    public static GraphTraversal<Vertex, Vertex> buildMethodModifierTraversal(GraphTraversalSource g, EdgeLabels edge, Vertex rootVertex) {
        return buildASTRepeat(g, edge, rootVertex).emit().hasLabel(ModifierVertex.LABEL.toString());
    }

    public static GraphTraversal<Vertex, Vertex> buildMethodReturnTraversal(GraphTraversalSource g, EdgeLabels edge, Vertex rootVertex) {
        return buildASTRepeat(g, edge, rootVertex).emit().hasLabel(MethodReturnVertex.LABEL.toString());
    }

    public static GraphTraversal<Vertex, Vertex> buildMethodParameterInTraversal(GraphTraversalSource g, EdgeLabels edge, Vertex rootVertex) {
        return buildASTRepeat(g, edge, rootVertex).emit().hasLabel(MethodParameterInVertex.LABEL.toString());
    }

}

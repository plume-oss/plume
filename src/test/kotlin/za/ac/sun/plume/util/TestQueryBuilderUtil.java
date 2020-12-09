package za.ac.sun.plume.util;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import za.ac.sun.plume.domain.enums.EdgeLabel;
import za.ac.sun.plume.domain.enums.VertexLabel;
import za.ac.sun.plume.domain.models.vertices.BlockVertex;
import za.ac.sun.plume.domain.models.vertices.MethodParameterInVertex;
import za.ac.sun.plume.domain.models.vertices.MethodReturnVertex;
import za.ac.sun.plume.domain.models.vertices.ModifierVertex;

import static za.ac.sun.plume.util.ExtractorConst.ASSIGN;

public class TestQueryBuilderUtil {

    public static GraphTraversal<Vertex, Vertex> buildASTRepeat(GraphTraversalSource g, EdgeLabel edge, Vertex rootVertex) {
        return g.V(rootVertex).repeat(__.out(edge.toString()));
    }

    public static GraphTraversal<Vertex, Vertex> getVertexAlongEdge(GraphTraversalSource g, EdgeLabel edge, Vertex rootVertex, VertexLabel label, String key, Object value) {
        return buildASTRepeat(g, edge, rootVertex).emit().has(label.toString(), key, value);
    }

    public static GraphTraversal<Vertex, Vertex> getVertexAlongEdgeFixed(GraphTraversalSource g, EdgeLabel edge, Vertex rootVertex, VertexLabel label, String key, Object value, int max) {
        return buildASTRepeat(g, edge, rootVertex).until(__.has(label.toString(), key, value).or().loops().is(max));
    }

    public static GraphTraversal<Vertex, Vertex> buildStoreTraversal(GraphTraversalSource g, EdgeLabel edge, Vertex rootVertex) {
        return getVertexAlongEdge(g, edge, rootVertex, BlockVertex.LABEL, "name", ASSIGN);
    }

    public static GraphTraversal<Vertex, Vertex> buildMethodModifierTraversal(GraphTraversalSource g, EdgeLabel edge, Vertex rootVertex) {
        return buildASTRepeat(g, edge, rootVertex).emit().hasLabel(ModifierVertex.LABEL.toString());
    }

    public static GraphTraversal<Vertex, Vertex> buildMethodReturnTraversal(GraphTraversalSource g, EdgeLabel edge, Vertex rootVertex) {
        return buildASTRepeat(g, edge, rootVertex).emit().hasLabel(MethodReturnVertex.LABEL.toString());
    }

    public static GraphTraversal<Vertex, Vertex> buildMethodParameterInTraversal(GraphTraversalSource g, EdgeLabel edge, Vertex rootVertex) {
        return buildASTRepeat(g, edge, rootVertex).emit().hasLabel(MethodParameterInVertex.LABEL.toString());
    }

}

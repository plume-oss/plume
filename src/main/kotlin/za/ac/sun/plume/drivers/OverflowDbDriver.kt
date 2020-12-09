package za.ac.sun.plume.drivers

import org.apache.logging.log4j.LogManager
import io.shiftleft.codepropertygraph.generated.nodes.*
import overflowdb.Config
import overflowdb.Edge
import overflowdb.Graph
import overflowdb.Node
import scala.Tuple2
import scala.runtime.AbstractFunction0
import za.ac.sun.plume.CpgDomainObjCreator.*
import za.ac.sun.plume.Traversals
import za.ac.sun.plume.domain.enums.DispatchType
import za.ac.sun.plume.domain.enums.EdgeLabel
import za.ac.sun.plume.domain.enums.EvaluationStrategy
import za.ac.sun.plume.domain.exceptions.PlumeSchemaViolationException
import za.ac.sun.plume.domain.models.PlumeGraph
import za.ac.sun.plume.domain.models.PlumeVertex
import za.ac.sun.plume.domain.models.vertices.*


/**
 * Driver to create an overflowDB database file from Plume's domain classes.
 *
 * TODO: the Plume domain classes and those provided by io.shiftleft.codepropertygraph
 * are so similar that it is worth investigating whether they can be used as a
 * replacement for Plume's domain classes. The advantage would be that (a) the
 * importer is backed by disk, meaning that we do not run into memory pressure for
 * large input programs, and (b) that no conversion from the Plume domain classes
 * is necessary when exporting to overflowdb.
 * */
class OverflowDbDriver : IDriver {

    private val logger = LogManager.getLogger(OverflowDbDriver::class.java)

    private var graph : Graph = createEmptyGraph()

    var dbfilename: String = ""
        private set

    fun dbfilename(value: String) = apply { dbfilename = value }

    fun connect() {
        graph = createEmptyGraph()
    }

    private fun createEmptyGraph() : Graph {
        val odbConfig = if (dbfilename != "") {
            Config.withDefaults().withStorageLocation(dbfilename)
        } else {
            Config.withDefaults()
        }
        return Graph.open(odbConfig,
                io.shiftleft.codepropertygraph.generated.nodes.Factories.allAsJava(),
                io.shiftleft.codepropertygraph.generated.edges.Factories.allAsJava())
    }

    override fun addVertex(v: PlumeVertex) {
        val id = v.hashCode()
        convert(v)?.let{ node ->
            val newNode = graph.addNode(id.toLong(), node.label())
            node.properties().foreachEntry { key, value ->
                newNode.setProperty(key, value)
            }
        }

    }

    private fun convert(v : PlumeVertex) : NewNode? {
        return when(v) {
            is ArrayInitializerVertex -> arrayInitializer(v.order)
            is BindingVertex -> binding(v.name, v.signature)
            is BlockVertex -> block(v.code, v.columnNumber, v.lineNumber, v.order, v.typeFullName, v.argumentIndex)
            is CallVertex -> call(v.code, v.name, v.columnNumber, v.lineNumber, v.order, v.methodFullName, v.argumentIndex, v.signature, v.dispatchType.name, v.dynamicTypeHintFullName, v.typeFullName)
            is ControlStructureVertex -> controlStructure(v.code, v.columnNumber, v.lineNumber, v.order)
            is FileVertex -> file(v.name, v.hash, v.order)
            is IdentifierVertex -> identifier(v.code, v.name, v.columnNumber, v.lineNumber, v.order, v.typeFullName, v.argumentIndex)
            is JumpTargetVertex -> jumpTarget(v.code, v.name, v.columnNumber, v.lineNumber, v.order)
            is LiteralVertex -> literal(v.code, v.columnNumber, v.lineNumber, v.order, v.typeFullName, v.argumentIndex)
            is LocalVertex -> local(v.code, v.name, v.columnNumber, v.lineNumber, v.order, v.typeFullName)
            is MetaDataVertex -> metaData(v.language, v.version)
            is MethodVertex ->
                method(v.code, v.name, v.fullName, v.signature, v.order, v.columnNumber, v.lineNumber)
            is MethodParameterInVertex -> methodParameter(v.code, v.name, v.lineNumber, v.order, v.evaluationStrategy.name, v.typeFullName)
            is MethodReturnVertex -> methodReturn(v.code, v.columnNumber, v.lineNumber, v.order, v.typeFullName, v.evaluationStrategy.name)
            is NamespaceBlockVertex -> namespaceBlock(v.name, v.fullName, v.order)
            is ReturnVertex -> returnNode(v.code, v.lineNumber, v.order, v.argumentIndex)
            is TypeVertex -> NewType(v.name, v.fullName, v.typeDeclFullName)
            is TypeArgumentVertex -> typeArgument(v.order)
            is TypeDeclVertex -> typeDecl(v.name, v.fullName, v.order, v.typeDeclFullName)
            is TypeParameterVertex -> typeParameter(v.name, v.order)
            else -> {
               logger.warn("Received unsupported vertex type."); null
           }

       }
    }

    private fun convert(v : Node) : PlumeVertex? {
        return when(v) {
            is ArrayInitializer -> ArrayInitializerVertex(v.order())
            is Block -> BlockVertex(v.typeFullName(), v.code(), v.order(), v.argumentIndex(), getOrElse(v.lineNumber(), 0), getOrElse(v.columnNumber(), 0))
            is Call -> CallVertex(v.methodFullName(), v.argumentIndex(),
                    convertDispatchType(v.dispatchType()), v.typeFullName(),
                    getOrElse(v.dynamicTypeHintFullName().headOption(), ""), v.name(), v.signature(), v.code(), v.order(),
                    getOrElse(v.lineNumber(), 0), getOrElse(v.columnNumber(), 0)
            )
            is File -> FileVertex(v.name(), getOrElse(v.hash(), ""), v.order())
            is Identifier -> IdentifierVertex(v.name(), v.typeFullName(), v.code(), v.order(), v.argumentIndex(), getOrElse(v.lineNumber(), 0), getOrElse(v.columnNumber(), 0))
            is MetaData -> MetaDataVertex(v.language(), v.version())
            is Method -> MethodVertex(v.name(), v.fullName(), v.signature(), v.code(),
                    getOrElse(v.lineNumber(), 0), getOrElse(v.columnNumber(), 0), v.order())
            is MethodParameterIn -> MethodParameterInVertex(v.code(), convertEvalStrategy(v.evaluationStrategy()) , v.typeFullName(), getOrElse(v.lineNumber(), 0), v.name(), v.order())
            is MethodReturn -> MethodReturnVertex(v.typeFullName(),
                    convertEvalStrategy(v.evaluationStrategy()), v.code(),
                    getOrElse(v.lineNumber(), 0), getOrElse(v.columnNumber(),0), v.order())
            is Literal -> LiteralVertex(v.typeFullName(), v.code(), v.order(), v.argumentIndex(), getOrElse(v.lineNumber(), 0), getOrElse(v.columnNumber(), 0))
            is Local -> LocalVertex(v.code(), v.typeFullName(), getOrElse(v.lineNumber(), 0), getOrElse(v.columnNumber(), 0), v.name(), v.order())
            is NamespaceBlock -> NamespaceBlockVertex(v.name(), v.fullName(), v.order())
            is Return -> ReturnVertex(getOrElse(v.lineNumber(), 0), getOrElse(v.columnNumber(), 0), v.order(), v.argumentIndex(), v.code())
            is TypeDecl -> TypeDeclVertex(v.name(), v.fullName(), v.astParentFullName(), v.order())
            else -> {
                logger.warn("Received unsupported vertex type."); null
            }
        }
    }

    private fun convertEvalStrategy(str : String) : EvaluationStrategy {
        return EvaluationStrategy.valueOf(str)
    }

    private fun convertDispatchType(str : String) : DispatchType {
        return DispatchType.valueOf(str)
    }

    private fun <T>getOrElse(opt : scala.Option<T>, def : T) : T {
        class F<T>(val x : T) : AbstractFunction0<T>() {
            override fun apply(): T {
                return x;
            }
        }
        return opt.getOrElse(F(def))
    }

    override fun exists(v: PlumeVertex): Boolean {
        return (graph.node(v.hashCode().toLong()) != null)
    }

    override fun exists(fromV: PlumeVertex, toV: PlumeVertex, edge: EdgeLabel): Boolean {
        val srcNode = graph.node(fromV.hashCode().toLong()) ?: return false
        val dstNode = graph.node(toV.hashCode().toLong()) ?: return false
        return srcNode.out(edge.name).asSequence().toList().any { node -> node.id().equals(dstNode.id()) }
    }

    override fun addEdge(fromV: PlumeVertex, toV: PlumeVertex, edge: EdgeLabel) {
        var srcNode = graph.node(fromV.hashCode().toLong())
        if (srcNode == null) {
            addVertex(fromV)
            srcNode = graph.node(fromV.hashCode().toLong())
        }
        var dstNode = graph.node(toV.hashCode().toLong())
        if (dstNode == null) {
            addVertex(toV)
            dstNode = graph.node(toV.hashCode().toLong())
        }

        try {
            srcNode.addEdge(edge.name, dstNode)
        } catch(exc : RuntimeException) {
            throw PlumeSchemaViolationException(fromV, toV, edge)
        }

    }

    override fun maxOrder(): Int {
        return Traversals.maxOrder(graph)
    }

    override fun clearGraph(): IDriver {
        Traversals.clearGraph(graph)
        return this
    }

    override fun getWholeGraph(): PlumeGraph {
        return nodesWithEdgesToPlumeGraph(Traversals.getWholeGraph(graph))
    }

    override fun getMethod(fullName: String, signature: String): PlumeGraph {
        return edgeListToPlumeGraph(Traversals.getMethod(graph, fullName, signature))
    }

    override fun getMethod(fullName: String, signature: String, includeBody: Boolean): PlumeGraph {
        if (includeBody) return getMethod(fullName, signature)
        return edgeListToPlumeGraph(Traversals.getMethodStub(graph, fullName, signature))
    }

    private fun nodesWithEdgesToPlumeGraph(nodesWithEdges : List<Tuple2<StoredNode, List<Edge>>>) : PlumeGraph {
        val plumeGraph = PlumeGraph()
        val plumeVertices = nodesWithEdges
                .map{ x -> x._1 }
                .distinct()
                .map{node -> Pair(node.id(), convert(node)) }
                .toMap()
        plumeVertices.values.forEach { v -> v?.let{ x -> plumeGraph.addVertex(x) } }
        val edges = nodesWithEdges.flatMap { x -> x._2 }
        edges.forEach { edge ->
            val srcNode = plumeVertices.get(edge.outNode().id())
            val dstNode = plumeVertices.get(edge.inNode().id())
            if (srcNode != null && dstNode != null) {
                plumeGraph.addEdge(srcNode, dstNode, EdgeLabel.valueOf(edge.label()))
            }
        }
        return plumeGraph
    }

    private fun edgeListToPlumeGraph(edges : List<Edge>) : PlumeGraph {
        val plumeGraph = PlumeGraph()

        val plumeVertices = edges.flatMap{ edge -> listOf(edge.inNode(), edge.outNode()) }
                .distinct()
                .map{node -> Pair(node.id(), convert(node)) }
                .toMap()

        plumeVertices.values.forEach { v ->  v?.let{ x -> plumeGraph.addVertex(x) } }
        edges.forEach { edge ->
            val srcNode = plumeVertices.get(edge.outNode().id())
            val dstNode = plumeVertices.get(edge.inNode().id())
            if (srcNode != null && dstNode != null) {
                plumeGraph.addEdge(srcNode, dstNode, EdgeLabel.valueOf(edge.label()))
            }
        }
        return plumeGraph
    }

    override fun getProgramStructure(): PlumeGraph {
        return edgeListToPlumeGraph(Traversals.getProgramStructure(graph))
    }

    override fun getNeighbours(v: PlumeVertex): PlumeGraph {
        return edgeListToPlumeGraph(Traversals.getNeighbours(graph, v.hashCode().toLong()))
    }

    override fun deleteVertex(v: PlumeVertex) {
        val node = graph.node(v.hashCode().toLong())
        if (node != null) {
            graph.remove(node)
        }
    }

    override fun deleteMethod(fullName: String, signature: String) {
        Traversals.deleteMethod(graph, fullName, signature)
    }

    override fun close() {
        graph.close()
    }

}
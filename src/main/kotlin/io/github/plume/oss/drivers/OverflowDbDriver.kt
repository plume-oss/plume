package io.github.plume.oss.drivers

import io.github.plume.oss.CpgDomainObjCreator.*
import io.github.plume.oss.Traversals
import io.github.plume.oss.domain.enums.DispatchType
import io.github.plume.oss.domain.enums.EdgeLabel
import io.github.plume.oss.domain.enums.EvaluationStrategy
import io.github.plume.oss.domain.enums.ModifierType
import io.github.plume.oss.domain.exceptions.PlumeSchemaViolationException
import io.github.plume.oss.domain.mappers.VertexMapper
import io.github.plume.oss.domain.models.PlumeGraph
import io.github.plume.oss.domain.models.PlumeVertex
import io.github.plume.oss.domain.models.vertices.*
import io.shiftleft.codepropertygraph.generated.nodes.*
import org.apache.logging.log4j.LogManager
import overflowdb.Config
import overflowdb.Edge
import overflowdb.Graph
import overflowdb.Node
import scala.Tuple2
import scala.runtime.AbstractFunction0
import io.shiftleft.codepropertygraph.generated.edges.Factories as EdgeFactories
import io.shiftleft.codepropertygraph.generated.nodes.Factories as NodeFactories


/**
 * Driver to create an OverflowDB database file from Plume's domain classes.
 */
class OverflowDbDriver : IDriver {

    private val logger = LogManager.getLogger(OverflowDbDriver::class.java)

    /**
     * Indicates whether the driver is connected to the graph database or not.
     */
    internal var connected = false

    private lateinit var graph: Graph

    /**
     * Where the database will be serialize/deserialize and overflow to disk.
     */
    var storageLocation: String = ""

    /**
     * Specifies if OverflowDb should write to disk when memory is constrained.
     */
    var overflow: Boolean = true

    /**
     * Percentage of the heap from when overflowing should begin to occur. Default is 80%.
     */
    var heapPercentageThreshold: Int = 80

    /**
     * If specified, OverflowDB will measure and report serialization/deserialization timing averages.
     */
    var serializationStatsEnabled: Boolean = false

    fun connect() {
        require(!connected) { "Please close the graph before trying to make another connection." }
        val odbConfig = Config.withDefaults()
            .apply {
                if (this@OverflowDbDriver.storageLocation.isNotBlank())
                    this.withStorageLocation(this@OverflowDbDriver.storageLocation)
            }
            .apply { if (!overflow) this.disableOverflow() }
            .apply { if (serializationStatsEnabled) this.withSerializationStatsEnabled() }
            .withHeapPercentageThreshold(heapPercentageThreshold)

        graph = Graph.open(
            odbConfig,
            NodeFactories.allAsJava(),
            EdgeFactories.allAsJava()
        )
        connected = true
    }

    override fun addVertex(v: PlumeVertex) {
        if (exists(v)) return
        val id = v.hashCode()
        convert(v)?.let { node ->
            val newNode = graph.addNode(id.toLong(), node.label())
            node.properties().foreachEntry { key, value ->
                newNode.setProperty(key, value)
            }
        }
    }

    private fun convert(v: PlumeVertex): NewNode? {
        return when (v) {
            is ArrayInitializerVertex -> arrayInitializer(v.order)
            is BindingVertex -> binding(v.name, v.signature)
            is BlockVertex -> block(v.code, v.columnNumber, v.lineNumber, v.order, v.typeFullName, v.argumentIndex)
            is CallVertex -> call(
                v.code,
                v.name,
                v.columnNumber,
                v.lineNumber,
                v.order,
                v.methodFullName,
                v.argumentIndex,
                v.signature,
                v.dispatchType.name,
                v.dynamicTypeHintFullName,
                v.typeFullName
            )
            is ControlStructureVertex -> controlStructure(v.code,
                v.columnNumber,
                v.lineNumber,
                v.order,
                v.argumentIndex)
            is FieldIdentifierVertex -> fieldIdentifier(
                v.canonicalName,
                v.code,
                v.argumentIndex,
                v.lineNumber,
                v.columnNumber,
                v.order
            )
            is FileVertex -> file(v.name, v.hash, v.order)
            is IdentifierVertex -> identifier(
                v.code,
                v.name,
                v.columnNumber,
                v.lineNumber,
                v.order,
                v.typeFullName,
                v.argumentIndex
            )
            is JumpTargetVertex -> jumpTarget(v.code, v.name, v.columnNumber, v.lineNumber, v.order, v.argumentIndex)
            is LiteralVertex -> literal(v.code, v.columnNumber, v.lineNumber, v.order, v.typeFullName, v.argumentIndex)
            is LocalVertex -> local(v.code, v.name, v.columnNumber, v.lineNumber, v.order, v.typeFullName)
            is MemberVertex -> member(v.code, v.name, v.typeFullName, v.order)
            is MetaDataVertex -> metaData(v.language, v.version)
            is MethodParameterInVertex -> methodParameterIn(
                v.code,
                v.name,
                v.lineNumber,
                v.order,
                v.evaluationStrategy.name,
                v.typeFullName
            )
            is MethodRefVertex -> methodRef(
                v.methodInstFullName,
                v.methodFullName,
                v.code,
                v.order,
                v.argumentIndex,
                v.lineNumber,
                v.columnNumber
            )
            is MethodReturnVertex -> methodReturn(
                v.code,
                v.columnNumber,
                v.lineNumber,
                v.order,
                v.typeFullName,
                v.evaluationStrategy.name
            )
            is MethodVertex ->
                method(v.code, v.name, v.fullName, v.signature, v.order, v.columnNumber, v.lineNumber)
            is ModifierVertex -> modifier(v.modifierType.name, v.order)
            is NamespaceBlockVertex -> namespaceBlock(v.name, v.fullName, v.order)
            is ReturnVertex -> returnNode(v.code, v.lineNumber, v.order, v.argumentIndex)
            is TypeArgumentVertex -> typeArgument(v.order)
            is TypeDeclVertex -> typeDecl(v.name, v.fullName, v.order, v.typeDeclFullName)
            is TypeParameterVertex -> typeParameter(v.name, v.order)
            is TypeRefVertex -> typeRef(
                v.typeFullName,
                v.dynamicTypeFullName,
                v.code,
                v.argumentIndex,
                v.lineNumber,
                v.columnNumber,
                v.order
            )
            is TypeVertex -> NewType(v.name, v.fullName, v.typeDeclFullName)
            is UnknownVertex -> unknown(v.typeFullName, v.code, v.order, v.argumentIndex, v.lineNumber, v.columnNumber)
            else -> {
                logger.warn("Received unsupported vertex type."); null
            }

        }
    }

    private fun convert(v: Node): PlumeVertex? {
        return when (v) {
            is ArrayInitializer -> ArrayInitializerVertex(v.order())
            is Binding -> BindingVertex(v.name(), v.signature())
            is Block -> BlockVertex(
                v.typeFullName(),
                v.code(),
                v.order(),
                v.argumentIndex(),
                getOrElse(v.lineNumber(), 0),
                getOrElse(v.columnNumber(), 0)
            )
            is Call -> CallVertex(
                v.methodFullName(), v.argumentIndex(),
                convertDispatchType(v.dispatchType()), v.typeFullName(),
                getOrElse(v.dynamicTypeHintFullName().headOption(), ""), v.name(), v.signature(), v.code(), v.order(),
                getOrElse(v.lineNumber(), 0), getOrElse(v.columnNumber(), 0)
            )
            is ControlStructure -> ControlStructureVertex(
                v.code(),
                getOrElse(v.lineNumber(), 0),
                getOrElse(v.columnNumber(), 0),
                v.order(),
                v.argumentIndex()
            )
            is FieldIdentifier -> FieldIdentifierVertex(
                v.canonicalName(),
                v.code(),
                v.argumentIndex(),
                getOrElse(v.lineNumber(), 0),
                getOrElse(v.columnNumber(), 0),
                v.order()
            )
            is File -> FileVertex(v.name(), getOrElse(v.hash(), ""), v.order())
            is Identifier -> IdentifierVertex(
                v.name(),
                v.typeFullName(),
                v.code(),
                v.order(),
                v.argumentIndex(),
                getOrElse(v.lineNumber(), 0),
                getOrElse(v.columnNumber(), 0)
            )
            is JumpTarget -> JumpTargetVertex(
                v.name(),
                v.argumentIndex(),
                getOrElse(v.lineNumber(), 0),
                getOrElse(v.columnNumber(), 0),
                v.code(),
                v.order()
            )
            is Literal -> LiteralVertex(
                v.typeFullName(),
                v.code(),
                v.order(),
                v.argumentIndex(),
                getOrElse(v.lineNumber(), 0),
                getOrElse(v.columnNumber(), 0)
            )
            is Local -> LocalVertex(
                v.code(),
                v.typeFullName(),
                getOrElse(v.lineNumber(), 0),
                getOrElse(v.columnNumber(), 0),
                v.name(),
                v.order()
            )
            is Member -> MemberVertex(v.code(), v.name(), v.typeFullName(), v.order())
            is MetaData -> MetaDataVertex(v.language(), v.version())
            is MethodParameterIn -> MethodParameterInVertex(
                v.code(),
                convertEvalStrategy(v.evaluationStrategy()),
                v.typeFullName(),
                getOrElse(v.lineNumber(), 0),
                v.name(),
                v.order()
            )
            is MethodRef -> MethodRefVertex(
                v.methodInstFullName().get(),
                v.methodFullName(),
                v.code(),
                v.order(),
                v.argumentIndex(),
                getOrElse(v.lineNumber(), 0),
                getOrElse(v.columnNumber(), 0)
            )
            is MethodReturn -> MethodReturnVertex(
                v.typeFullName(),
                convertEvalStrategy(v.evaluationStrategy()), v.code(),
                getOrElse(v.lineNumber(), 0), getOrElse(v.columnNumber(), 0), v.order()
            )
            is Method -> MethodVertex(
                v.name(), v.fullName(), v.signature(), v.code(),
                getOrElse(v.lineNumber(), 0), getOrElse(v.columnNumber(), 0), v.order()
            )
            is Modifier -> ModifierVertex(
                convertModifierType(v.modifierType()),
                v.order()
            )
            is NamespaceBlock -> NamespaceBlockVertex(v.name(), v.fullName(), v.order())
            is Return -> ReturnVertex(
                getOrElse(v.lineNumber(), 0),
                getOrElse(v.columnNumber(), 0),
                v.order(),
                v.argumentIndex(),
                v.code()
            )
            is TypeDecl -> TypeDeclVertex(v.name(), v.fullName(), v.astParentFullName(), v.order())
            is TypeParameter -> TypeParameterVertex(v.name(), v.order())
            is TypeRef -> TypeRefVertex(
                v.typeFullName(),
                v.dynamicTypeHintFullName().head(),
                v.code(),
                v.argumentIndex(),
                getOrElse(v.lineNumber(), 0),
                getOrElse(v.columnNumber(), 0),
                v.order()
            )
            is Type -> TypeVertex(v.name(), v.fullName(), v.typeDeclFullName())
            is Unknown -> UnknownVertex(
                v.typeFullName(),
                v.code(),
                v.order(),
                v.argumentIndex(),
                getOrElse(v.lineNumber(), 0),
                getOrElse(v.columnNumber(), 0)
            )
            else -> {
                logger.warn("Received unsupported vertex type."); null
            }
        }
    }

    private fun convertModifierType(str: String): ModifierType {
        return ModifierType.valueOf(str)
    }

    private fun convertEvalStrategy(str: String): EvaluationStrategy {
        return EvaluationStrategy.valueOf(str)
    }

    private fun convertDispatchType(str: String): DispatchType {
        return DispatchType.valueOf(str)
    }

    private fun <T> getOrElse(opt: scala.Option<T>, def: T): T {
        class F<T>(val x: T) : AbstractFunction0<T>() {
            override fun apply(): T {
                return x
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
        return srcNode.out(edge.name).asSequence().toList().any { node -> node.id() == dstNode.id() }
    }

    override fun addEdge(fromV: PlumeVertex, toV: PlumeVertex, edge: EdgeLabel) {
        if (!VertexMapper.checkSchemaConstraints(fromV, toV, edge)) throw PlumeSchemaViolationException(fromV,
            toV,
            edge)
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
        } catch (exc: RuntimeException) {
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

    private fun nodesWithEdgesToPlumeGraph(nodesWithEdges: List<Tuple2<StoredNode, List<Edge>>>): PlumeGraph {
        val plumeGraph = PlumeGraph()
        val plumeVertices = nodesWithEdges
            .map { x -> x._1 }
            .distinct()
            .map { node -> Pair(node.id(), convert(node)) }
            .toMap()

        plumeVertices.values.forEach { v -> v?.let { x -> plumeGraph.addVertex(x) } }
        val edges = nodesWithEdges.flatMap { x -> x._2 }
        serializePlumeEdges(edges, plumeVertices, plumeGraph)
        return plumeGraph
    }

    private fun edgeListToPlumeGraph(edges: List<Edge>): PlumeGraph {
        val plumeGraph = PlumeGraph()
        val plumeVertices = edges.flatMap { edge -> listOf(edge.inNode(), edge.outNode()) }
            .distinct()
            .map { node -> Pair(node.id(), convert(node)) }
            .toMap()

        plumeVertices.values.forEach { v -> v?.let { x -> plumeGraph.addVertex(x) } }
        serializePlumeEdges(edges, plumeVertices, plumeGraph)
        return plumeGraph
    }

    private fun serializePlumeEdges(
        edges: List<Edge>,
        plumeVertices: Map<Long, PlumeVertex?>,
        plumeGraph: PlumeGraph
    ) {
        edges.forEach { edge ->
            val srcNode = plumeVertices[edge.outNode().id()]
            val dstNode = plumeVertices[edge.inNode().id()]
            if (srcNode != null && dstNode != null) {
                plumeGraph.addEdge(srcNode, dstNode, EdgeLabel.valueOf(edge.label()))
            }
        }
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
        require(connected) { "Cannot close a graph that is not already connected!" }
        try {
            graph.close()
        } catch (e: Exception) {
            logger.warn("Exception thrown while attempting to close graph.", e)
        }  finally {
            connected = false
        }
    }

}
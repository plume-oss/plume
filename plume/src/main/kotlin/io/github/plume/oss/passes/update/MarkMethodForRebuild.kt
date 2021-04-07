package io.github.plume.oss.passes.update

import io.github.plume.oss.domain.mappers.VertexMapper
import io.github.plume.oss.drivers.IDriver
import io.github.plume.oss.store.DriverCache
import io.github.plume.oss.store.PlumeStorage
import io.github.plume.oss.util.HashUtil
import io.github.plume.oss.util.SootToPlumeUtil
import io.shiftleft.codepropertygraph.generated.NodeTypes
import io.shiftleft.codepropertygraph.generated.nodes.Call
import io.shiftleft.codepropertygraph.generated.nodes.Method
import io.shiftleft.codepropertygraph.generated.nodes.NewCallBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewMethodBuilder
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import overflowdb.Node
import soot.SootMethod

class MarkMethodForRebuild(private val driver: IDriver) {

    private val logger: Logger = LogManager.getLogger(MarkMethodForRebuild::javaClass)
    private val cache = DriverCache(driver)
    private var methodsToDelete = 0

    fun runPass(ms: Set<SootMethod>): Set<SootMethod> {
        val mPairs = ms.map(::checkIfMethodNeedsAnUpdate).toList()
        val msToUpdate = mPairs.filter { it.second }
        if (msToUpdate.isNotEmpty())
            logger.info("Methods to create/update is ${msToUpdate.size}. Methods to remove is $methodsToDelete.")
        return msToUpdate.map { it.first }.toSet()
    }

    private fun checkIfMethodNeedsAnUpdate(m: SootMethod): Pair<SootMethod, Boolean> {
        val (fullName, _, _) = SootToPlumeUtil.methodToStrings(m)
        val maybeMethod = cache.tryGetMethod(fullName)
        return if (maybeMethod == null) {
            // case for when there is no method found
            Pair(m, true)
        } else if (m.hasActiveBody()) {
            // case for if method hash may or may not differ
            val currentMethodHash = HashUtil.getMethodHash(m.activeBody).toString()
            val existingMethodHash = maybeMethod.build().hash().get()
            if (currentMethodHash == existingMethodHash) {
                Pair(m, false)
            } else {
                saveCallEdges(fullName)
                driver.deleteMethod(fullName)
                methodsToDelete++
                Pair(m, true)
            }
        } else {
            // case for method existing but is most likely an external method
            Pair(m, false)
        }
    }

    private fun saveCallEdges(methodFullName: String) {
        // TODO: This method head oculd be getten better
        driver.getMethod(methodFullName, false).use { g ->
            g.nodes { it == Method.Label() }.asSequence().firstOrNull()?.let { m1: Node ->
                val m2 = VertexMapper.mapToVertex(m1) as NewMethodBuilder
                val m2Build = m2.build()
                driver.getNeighbours(m2).use { ns ->
                    if (ns.V(m1.id()).hasNext()) {
                        ns.V(m1.id()).next().`in`(NodeTypes.CALL).asSequence()
                            .filterIsInstance<Call>()
                            .forEach {
                                PlumeStorage.storeCallEdge(
                                    m2Build.fullName(),
                                    VertexMapper.mapToVertex(it) as NewCallBuilder
                                )
                            }
                    }
                }
            }
        }
    }
}
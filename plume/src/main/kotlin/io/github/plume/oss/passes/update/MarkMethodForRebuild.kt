/*
 * Copyright 2021 Plume Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.plume.oss.passes.update

import io.github.plume.oss.drivers.IDriver
import io.github.plume.oss.store.DriverCache
import io.github.plume.oss.store.PlumeStorage
import io.github.plume.oss.util.ExtractorConst.UNKNOWN
import io.github.plume.oss.util.HashUtil
import io.github.plume.oss.util.SootToPlumeUtil
import io.shiftleft.codepropertygraph.generated.EdgeTypes
import io.shiftleft.codepropertygraph.generated.NodeTypes.METHOD
import io.shiftleft.codepropertygraph.generated.nodes.Call
import io.shiftleft.codepropertygraph.generated.nodes.Method
import io.shiftleft.codepropertygraph.generated.nodes.NewCallBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewMethodBuilder
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import soot.SootClass
import soot.SootMethod

/**
 * Will run through the methods in the given class to determine whether, if the class is already in the database, it
 * needs to be updated or not.
 */
class MarkMethodForRebuild(private val driver: IDriver) {

    private val logger: Logger = LogManager.getLogger(MarkMethodForRebuild::javaClass)
    private val cache = DriverCache(driver)

    /**
     * Given a set of classes, determines if its methods need to be updated or not.
     *
     * @param ms The set of classes.
     * @return A set of all the methods to generate.
     */
    fun runPass(ms: Set<SootClass>): Set<SootMethod> {
        val mPairs = ms.filter { it.isApplicationClass }.flatMap(::checkIfClassMethodsNeedUpdate).toList()
        val msToUpdate = mPairs.filter { it.second }
        if (msToUpdate.isNotEmpty())
            logger.debug("Methods to create/update is ${msToUpdate.size}.")
        return msToUpdate.map { it.first }.toSet()
    }

    private fun checkIfClassMethodsNeedUpdate(c: SootClass): List<Pair<SootMethod, Boolean>> {
        val cms = c.methods
        // Check for deleted methods
        val cmsNames = cms.map { SootToPlumeUtil.methodToStrings(it).first }.toSet()
        cache.tryGetTypeDecl(c.type.toQuotedString())?.let { appType ->
            driver.getNeighbours(appType).use { g ->
                // Remove methods not in the new class
                g.nodes(METHOD).asSequence()
                    .filterIsInstance<Method>()
                    .filterNot { cmsNames.contains(it.fullName()) }
                    .forEach { driver.deleteMethod(it.fullName()) }
            }
        }
        // Check for methods which need to be added or updated
        return cms.map(::checkIfMethodNeedsAnUpdate)
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
            when (currentMethodHash) {
                UNKNOWN -> Pair(m, false)
                existingMethodHash -> Pair(m, false)
                else -> {
                    saveCallEdges(fullName)
                    driver.deleteMethod(fullName)
                    Pair(m, true)
                }
            }
        } else {
            // case for method existing but is most likely an external method
            Pair(m, false)
        }
    }

    private fun saveCallEdges(methodFullName: String) {
        cache.tryGetMethod(methodFullName)?.let { m1: NewMethodBuilder ->
            val m1Build = m1.build()
            driver.getNeighbours(m1).use { ns ->
                ns.nodes(METHOD).next().`in`(EdgeTypes.CALL).asSequence()
                    .filterIsInstance<Call>()
                    .forEach {
                        PlumeStorage.storeCallEdge(
                            m1Build.fullName(),
                            NewCallBuilder().id(it.id())
                        )
                    }
            }
        }
    }
}
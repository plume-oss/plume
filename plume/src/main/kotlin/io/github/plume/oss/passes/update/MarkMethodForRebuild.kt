package io.github.plume.oss.passes.update

import io.github.plume.oss.drivers.IDriver
import io.github.plume.oss.store.DriverCache
import io.github.plume.oss.util.HashUtil
import io.github.plume.oss.util.SootToPlumeUtil
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import soot.SootMethod

class MarkMethodForRebuild(private val driver: IDriver) {

    private val logger: Logger = LogManager.getLogger(MarkMethodForRebuild::javaClass)
    private val cache = DriverCache(driver)
    private var methodsToDelete = 0

    fun runPass(ms: List<SootMethod>): List<SootMethod> {
        val mPairs = ms.map(::checkIfMethodNeedsAnUpdate).toList()
        val msToUpdate = mPairs.filter { it.second }
        if (msToUpdate.isNotEmpty())
            logger.info("Methods to create is ${msToUpdate.size}. Methods to remove is $methodsToDelete.")
        return msToUpdate.map { it.first }
    }

    private fun checkIfMethodNeedsAnUpdate(m: SootMethod): Pair<SootMethod, Boolean> {
        val (fullName, _, _) = SootToPlumeUtil.methodToStrings(m)
        val maybeMethod = cache.tryGetMethod(fullName)
        return if (maybeMethod == null) {
            // case for when there is no method found
            Pair(m, true)
        } else if (m.hasActiveBody()) {
            // case for if method hash may or may not differ
            val currentMethodhash = HashUtil.getMethodHash(m.activeBody).toString()
            val existingMethodHash = maybeMethod.build().hash().get()
            if (currentMethodhash == existingMethodHash) {
                Pair(m, false)
            } else {
                driver.deleteMethod(fullName)
                methodsToDelete++
                Pair(m, true)
            }
        } else {
            // case for method existing but is most likely an external method
            Pair(m, false)
        }
    }
}
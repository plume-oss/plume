package io.github.plume.oss.passes.update

import io.github.plume.oss.drivers.IDriver
import io.github.plume.oss.store.DriverCache
import io.github.plume.oss.util.SootParserUtil
import io.github.plume.oss.util.SootToPlumeUtil
import io.shiftleft.codepropertygraph.generated.NodeTypes
import io.shiftleft.codepropertygraph.generated.NodeTypes.MODIFIER
import io.shiftleft.codepropertygraph.generated.nodes.NewFileBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewMemberBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewModifierBuilder
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import soot.SootClass
import soot.SootField

class MarkFieldForRebuild(private val driver: IDriver) {

    private val logger: Logger = LogManager.getLogger(MarkFieldForRebuild::javaClass)
    private val cache = DriverCache(driver)
    private var fieldsToDelete = 0

    /**
     * This pass will check and remove any fields that require an update from the database.
     *
     * @return a list of fields which need to be built.
     */
    fun runPass(cs: List<SootClass>): List<SootField> {
        val fieldsToCreate = cs.asSequence()
            .map(::checkIfFieldNeedsAnUpdate)
            .flatten()
            .filter { it.second }
            .map { it.first }
            .toList()
        if (fieldsToCreate.isNotEmpty())
            logger.info("Fields to create is ${fieldsToCreate.size}. Fields to remove is $fieldsToDelete.")
        return fieldsToCreate
    }

    /**
     * Will run through fields to see if they require updates. If a field has a modification to it's modifiers, type, or
     * it is no longer present it will be deleted and rebuilt.
     *
     * @param c The class to check.
     * @return a list of ([SootField], [Boolean]) where the [Boolean] will be true if the field needs to be rebuilt,
     * false if otherwise.
     */
    private fun checkIfFieldNeedsAnUpdate(c: SootClass): List<Pair<SootField, Boolean>> {
        val cName = SootToPlumeUtil.sootClassToFileName(c)
        val output = mutableListOf<Pair<SootField, Boolean>>()
        cache.tryGetFile(cName)?.let { f: NewFileBuilder ->
            driver.getNeighbours(f).use { g ->
                val existingMembers = g.nodes(NodeTypes.MEMBER).asSequence().filterIsInstance<NewMemberBuilder>().toList()
                val newMembers = c.fields.toList().map { field ->
                    var modificationRequired = false
                    val foundMember = existingMembers
                        .firstOrNull { field.name == it.build().name() }
                    if (foundMember == null) {
                        // If null, then there is a new field to add
                        modificationRequired = true
                    } else {
                        // Check if types need update
                        if (foundMember.build().typeFullName() != field.type.toQuotedString()) {
                            modificationRequired = true
                            deleteField(foundMember)
                        }
                        else {
                            // Check if modifications need updates
                            driver.getNeighbours(f).use { memberG ->
                                val existingModifiers = memberG.nodes(MODIFIER).asSequence()
                                    .filterIsInstance<NewModifierBuilder>()
                                    .map { it.build().modifierType() }
                                    .toSortedSet()
                                val newModifiers = SootParserUtil.determineModifiers(field.modifiers).toSortedSet()
                                if (existingModifiers.minus(newModifiers).isNotEmpty())
                                    modificationRequired = true
                            }
                        }
                    }
                    output.add(Pair(field, modificationRequired))
                    field.name
                }.toSortedSet()
                // Remove fields that are not there
                fieldsToDelete = existingMembers.filter { !newMembers.contains(it.build().name()) }.map(::deleteField).size
            }
        }
        return output
    }

    private fun deleteField(f: NewMemberBuilder) {
        driver.getNeighbours(f).use { g -> g.nodes(MODIFIER).forEach { driver.deleteVertex(it.id(), it.label()) } }
        driver.deleteVertex(f.id(), f.build().label())
    }

}
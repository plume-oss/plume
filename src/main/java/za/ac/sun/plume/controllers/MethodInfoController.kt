/*
 * Copyright 2020 David Baker Effendi
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
package za.ac.sun.plume.controllers

import org.objectweb.asm.Label
import za.ac.sun.plume.domain.meta.JumpInfo
import za.ac.sun.plume.domain.meta.LocalVarInfo
import za.ac.sun.plume.util.ASMParserUtil
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashSet
import kotlin.collections.set

data class MethodInfoController(
        val methodName: String,
        val methodSignature: String,
        val access: Int,
        var lineNumber: Int? = -1
) : OpStackController() {

    private val allVariables = mutableListOf<LocalVarInfo>()
    private val allJumps = LinkedHashSet<JumpInfo>()
    private val ternaryPairs = LinkedHashMap<JumpInfo, JumpInfo>()
    private val jumpRoot = HashMap<Int, String>()

    fun addVariable(frameId: Int) {
        allVariables.add(LocalVarInfo(frameId))
    }

    fun addVarDebugInfo(frameId: Int, debugName: String, descriptor: String, startLabel: Label, endLabel: Label) {
        val existingVar = allVariables.find { it.frameId == frameId }
        if (existingVar != null) {
            existingVar.debugName = debugName
            existingVar.descriptor = descriptor
            existingVar.startLabel = startLabel
            existingVar.endLabel = endLabel
        }
    }

    fun addJump(jumpOp: String, destLabel: Label, currentLabel: Label) =
            allJumps.add(JumpInfo(jumpOp, destLabel, currentLabel, super.pseudoLineNo))


    fun addTernaryPair(gotoOp: String, destLabel: Label, currentLabel: Label) {
        val lastJump = allJumps.findLast { !ternaryPairs.containsKey(it) && it.jumpOp.contains("IF_")}!!
        addJump(gotoOp, destLabel, currentLabel)
        ternaryPairs[lastJump] = JumpInfo(gotoOp, destLabel, currentLabel, super.pseudoLineNo)
    }

    fun getAssociatedJumps(pseudoLineNo: Int): MutableList<JumpInfo> {
        val assocLineInfo = getLineInfo(pseudoLineNo) ?: return emptyList<JumpInfo>().toMutableList()
        return allJumps.filter { jInfo: JumpInfo -> assocLineInfo.associatedLabels.contains(jInfo.destLabel) }.toMutableList()
    }

    fun getAssociatedTernaryJump(pseudoLineNo: Int): Pair<JumpInfo, JumpInfo>? {
        return ternaryPairs.filter { it.value.pseudoLineNo == pseudoLineNo }.takeIf { it.isNotEmpty() }?.entries?.first()?.toPair()
    }

    fun getAssociatedTernaryJump(pseudoLineNo: Int, blacklist: Stack<Pair<JumpInfo, JumpInfo>>): Pair<JumpInfo, JumpInfo>? {
        return ternaryPairs.filter { ternPair -> ternPair.key.pseudoLineNo == pseudoLineNo && blacklist.none { it.first == ternPair.key } }.takeIf { it.isNotEmpty() }?.entries?.last()?.toPair()
    }

    fun getPseudoLineNumber(label: Label): Int = getLineInfo(label)?.pseudoLineNumber ?: -1

    fun upsertJumpRootAtLine(pseudoLineNo: Int, name: String) = if (jumpRoot.containsKey(pseudoLineNo)) jumpRoot.replace(pseudoLineNo, name) else jumpRoot.put(pseudoLineNo, name)

    fun getJumpRootName(currentLabel: Label?) = jumpRoot.getOrDefault(currentLabel?.let { getPseudoLineNumber(it) }, "IF")

    fun isLabelAssociatedWithLoops(label: Label): Boolean {
        val loopNames = listOf("WHILE", "DO_WHILE", "FOR")
        val rootName = jumpRoot.getOrDefault(getPseudoLineNumber(label), "IF")
        return !loopNames.none { name -> name == rootName }
    }

    fun findJumpLineBasedOnDestLabel(destLabel: Label): Int? {
        // Find associated labels with the dest label
        val associatedLabels = getLineInfo(destLabel)?.associatedLabels ?: return null
        // Match this with a jump
        val matchedJump = allJumps.find { jumpInfo -> associatedLabels.contains(jumpInfo.destLabel) } ?: return null
        // Get the current line of the jump current line
        return getLineInfo(matchedJump.currLabel)?.pseudoLineNumber
    }

    fun isJumpVertexAssociatedWithGivenLine(jumpBlockPseudoLineNo: Int, lineNumber: Int) =
            !getAssociatedJumps(jumpBlockPseudoLineNo).none { jumpInfo -> getPseudoLineNumber(jumpInfo.currLabel) == lineNumber }


    override fun toString() = "$lineNumber: ${ASMParserUtil.determineModifiers(access)} $methodName $methodSignature"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MethodInfoController

        if (methodName != other.methodName) return false
        if (methodSignature != other.methodSignature) return false
        if (access != other.access) return false

        return true
    }

    override fun hashCode(): Int {
        var result = methodName.hashCode()
        result = 31 * result + methodSignature.hashCode()
        result = 31 * result + access
        return result
    }
}
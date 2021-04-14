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
package io.github.plume.oss.passes.structure

import io.github.plume.oss.drivers.IDriver
import io.github.plume.oss.store.DriverCache
import io.github.plume.oss.util.ProgressBarUtil
import io.github.plume.oss.util.SootParserUtil
import io.shiftleft.codepropertygraph.generated.EdgeTypes.AST
import io.shiftleft.codepropertygraph.generated.EdgeTypes.EVAL_TYPE
import io.shiftleft.codepropertygraph.generated.nodes.NewMemberBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewModifierBuilder
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import soot.SootField

class MemberPass(private val driver: IDriver) {

    private val logger: Logger = LogManager.getLogger(MemberPass::javaClass)
    private val cache = DriverCache(driver)

    fun runPass(fs: List<SootField>) {
        ProgressBarUtil.runInsideProgressBar(
            logger.level, "Fields", fs.size.toLong()
        ) { pb -> fs.forEach { linkMembers(it); pb?.step() } }
    }

    /*
     * TYPE_DECL -(AST)-> MEMBER
     * MEMBER -(AST)-> MODIFIER
     * MEMBER -(EVAL_TYPE)-> TYPE
     */
    private fun linkMembers(f: SootField) {
        val c = f.declaringClass
        val t = cache.getOrMakeTypeDecl(c.type)
        c.fields.forEachIndexed { i, field ->
            projectMember(field, i + 1).let { memberVertex ->
                driver.addEdge(t, memberVertex, AST)
                // Add modifiers to member
                SootParserUtil.determineModifiers(field.modifiers)
                    .mapIndexed { i, m -> NewModifierBuilder().modifierType(m).order(i + 1) }
                    .forEach { m -> driver.addEdge(memberVertex, m, AST) }
                // Link member to type
                cache.tryGetGlobalType(field.type.toQuotedString())?.let { mType ->
                    driver.addEdge(memberVertex, mType, EVAL_TYPE)
                }
                cache.tryGetType(field.type.toQuotedString())?.let { mType ->
                    driver.addEdge(memberVertex, mType, EVAL_TYPE)
                }
            }
        }
    }

    private fun projectMember(field: SootField, childIdx: Int): NewMemberBuilder =
        NewMemberBuilder()
            .name(field.name)
            .code(field.declaration)
            .typeFullName(field.type.toQuotedString())
            .order(childIdx)

}
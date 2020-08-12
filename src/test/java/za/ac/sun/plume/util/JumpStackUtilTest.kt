package za.ac.sun.plume.util

import org.apache.commons.collections.CollectionUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.objectweb.asm.Label
import za.ac.sun.plume.domain.enums.JumpState
import za.ac.sun.plume.domain.stack.BlockItem
import za.ac.sun.plume.domain.stack.block.GotoBlock
import za.ac.sun.plume.domain.stack.block.IfCmpBlock
import za.ac.sun.plume.domain.stack.block.JumpBlock
import za.ac.sun.plume.domain.stack.block.NestedBodyBlock
import za.ac.sun.plume.domain.stack.block.StoreBlock
import za.ac.sun.plume.util.JumpStackUtil.getAssociatedJumps
import za.ac.sun.plume.util.JumpStackUtil.getJumpHistory
import za.ac.sun.plume.util.JumpStackUtil.getLastJump
import za.ac.sun.plume.util.JumpStackUtil.isJumpDestination
import java.util.*
import kotlin.collections.HashSet

class JumpStackUtilTest {

    @Test
    fun getLastJumpTest() {
        Assertions.assertNull(getLastJump(EMPTY_HISTORY))
        Assertions.assertEquals(TARGET_IFCMP_2, getLastJump(NON_EMPTY_HIST))
    }

    @Test
    fun getAssociatedJumpsTest() {
        Assertions.assertTrue(getAssociatedJumps(EMPTY_JUMP_SET, DEST_LABEL_1).isEmpty())
        Assertions.assertEquals(CollectionUtils.getCardinalityMap(listOf(TARGET_IFCMP_1, TARGET_GOTO_1)),
                CollectionUtils.getCardinalityMap(getAssociatedJumps(NON_EMPTY_JUMP_SET, DEST_LABEL_1)))
        Assertions.assertEquals(CollectionUtils.getCardinalityMap(listOf(TARGET_IFCMP_2)),
                CollectionUtils.getCardinalityMap(getAssociatedJumps(NON_EMPTY_JUMP_SET, DEST_LABEL_2)))
    }

    @Test
    fun getJumpHistoryTest() {
        Assertions.assertTrue(getJumpHistory(EMPTY_HISTORY).isEmpty())
        val expected: Stack<JumpBlock> = Stack()
        expected.addAll(listOf(TARGET_IFCMP_1, TARGET_IFCMP_2))
        Assertions.assertEquals(expected, getJumpHistory(NON_EMPTY_HIST))
    }

    @Test
    fun isJumpDestinationTest() {
        Assertions.assertFalse(isJumpDestination(EMPTY_JUMP_SET, Label()))
        Assertions.assertFalse(isJumpDestination(NON_EMPTY_JUMP_SET, Label()))
        Assertions.assertTrue(isJumpDestination(NON_EMPTY_JUMP_SET, DEST_LABEL_1))
    }

    companion object {
        private val DEST_LABEL_1: Label = Label()
        private val DEST_LABEL_2: Label = Label()
        private val TARGET_IFCMP_1: IfCmpBlock = IfCmpBlock(2, Label(), DEST_LABEL_1, JumpState.METHOD_BODY)
        private val TARGET_IFCMP_2: IfCmpBlock = IfCmpBlock(3, Label(), DEST_LABEL_2, JumpState.METHOD_BODY)
        private val TARGET_GOTO_1: GotoBlock = GotoBlock(3, Label(), DEST_LABEL_1, JumpState.METHOD_BODY)
        private val EMPTY_HISTORY: Stack<BlockItem> = Stack()
        private val NON_EMPTY_HIST: Stack<BlockItem> = Stack()
        private val EMPTY_JUMP_SET: HashSet<JumpBlock> = HashSet()
        private val NON_EMPTY_JUMP_SET: HashSet<JumpBlock> = hashSetOf(TARGET_IFCMP_1, TARGET_IFCMP_2, TARGET_GOTO_1)

        @BeforeAll
        @JvmStatic
        fun setUpAll() {
            NON_EMPTY_HIST.addAll(listOf(
                    StoreBlock(1, Label()),
                    TARGET_IFCMP_1,
                    TARGET_IFCMP_2,
                    NestedBodyBlock(1, Label(), JumpState.IF_BODY)
            ))
        }
    }
}
package za.ac.sun.plume.util

import org.objectweb.asm.Label
import za.ac.sun.plume.domain.stack.BlockItem
import za.ac.sun.plume.domain.stack.block.GotoBlock
import za.ac.sun.plume.domain.stack.block.IfCmpBlock
import za.ac.sun.plume.domain.stack.block.JumpBlock
import java.util.*
import java.util.stream.Collectors

object JumpStackUtil {

    /**
     * Returns the latest {@link JumpBlock} in the block history.
     */
    @JvmStatic
    fun getLastJump(blockHistory: Stack<BlockItem>): JumpBlock? {
        val listIterator: ListIterator<BlockItem> = blockHistory.listIterator(blockHistory.size)
        while (listIterator.hasPrevious()) {
            val prev = listIterator.previous()
            if (prev is IfCmpBlock || prev is GotoBlock) return prev as JumpBlock
        }
        return null
    }

    /**
     * Returns all the {@link JumpBlock}s who share the given destination.
     *
     * @param jumpSet all the jumps to check.
     * @param destination the destination label to associate jumps by.
     */
    @JvmStatic
    fun getAssociatedJumps(jumpSet: HashSet<JumpBlock>, destination: Label): List<JumpBlock?> {
        return jumpSet.parallelStream()
                .filter { j: JumpBlock -> j.destination === destination }
                .collect(Collectors.toList())
    }

    /**
     * Returns the all the {@link JumpBlock}s found in the {@link BlockItem} history.
     *
     * @param blockHistory the history of {@link BlockItem}s.
     */
    @JvmStatic
    fun getJumpHistory(blockHistory: Stack<BlockItem>): Stack<JumpBlock> {
        val stack = Stack<JumpBlock>()
        blockHistory.stream()
                .filter { j: BlockItem? -> j is JumpBlock }
                .forEachOrdered { j: BlockItem -> stack.push(j as JumpBlock) }
        return stack
    }

    /**
     * Using the given jump set, determines if the label is a jump destination.
     *
     * @param jumpSet all the jumps to check.
     * @param label the label to check destinations against.
     */
    @JvmStatic
    fun isJumpDestination(jumpSet: HashSet<JumpBlock>, label: Label): Boolean {
        return jumpSet.parallelStream()
                .map { j: JumpBlock -> j.destination === label }
                .filter { b: Boolean? -> b!! }
                .findFirst()
                .orElse(false)
    }
}
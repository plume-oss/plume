package io.github.plume.oss.passes

import io.shiftleft.passes.KeyPool

import java.util.concurrent.atomic.AtomicLong

class IncrementalKeyPool(val first: Long, val last: Long, private val usedIds: Set[Long])
    extends KeyPool {

  override def next: Long = {
    if (!valid) {
      throw new IllegalStateException("Call to `next` on invalidated IncrementalKeyPool.")
    }
    var n = cur.incrementAndGet()
    while (n < last) {
      if (!usedIds.contains(n)) return n
      else n = cur.incrementAndGet()
    }
    throw new RuntimeException("Pool exhausted")
  }

  def split(numberOfPartitions: Int): Iterator[IncrementalKeyPool] = {
    valid = false
    if (numberOfPartitions == 0) {
      Iterator()
    } else {
      val curFirst = cur.get()
      val k        = (last - curFirst) / numberOfPartitions
      (1 to numberOfPartitions).map { i =>
        val poolFirst = curFirst + (i - 1) * k
        new IncrementalKeyPool(poolFirst, poolFirst + k - 1, usedIds)
      }.iterator
    }
  }

  private var valid: Boolean  = true
  private val cur: AtomicLong = new AtomicLong(first - 1)

}

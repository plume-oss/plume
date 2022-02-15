package com.github.plume.oss

import scala.collection.mutable

/** Holds statistics regarding the performance of certain stages of the extraction process.
  */
object PlumeStatistics extends Enumeration {

  /** Represents a measureable feature that can be timed.
    */
  type PlumeStatistic = Value

  val TIME_OPEN_DRIVER, TIME_CLOSE_DRIVER, TIME_EXTRACTION, TIME_REACHABLE_BY_QUERYING = Value

  private val time: mutable.Map[PlumeStatistic, Long] =
    PlumeStatistics.values.map((_, 0L)).to(collection.mutable.Map)

  /** Measures the time for a given code block and saves it under the [[PlumeStatistic]] heading.
    * @param stat the statistic to save the elapsed time under.
    * @param block the code block to measure.
    * @tparam R the return value of the code block.
    * @return the result of the code block.
    */
  def time[R](stat: PlumeStatistic, block: => R): R = {
    val t0     = System.nanoTime()
    val result = block
    val t1     = System.nanoTime()
    time.put(stat, t1 - t0)
    result
  }

  /** The results of the measured time per process.
    * @return a map of each [[PlumeStatistic]] and the time per process in nanoseconds.
    */
  def results(): Map[PlumeStatistic, Long] = time.toMap

  /** Sets all the clocks back to 0.
    */
  def reset(): Unit = {
    time.clear()
    PlumeStatistics.values.map((_, 0L)).foreach { case (v, t) => time.put(v, t) }
  }

}

package com.github.plume.oss

import scala.collection.mutable

/** Holds statistics regarding the performance of certain stages of the extraction process.
  */
object PlumeStatistics extends Enumeration {

  /** Represents a measureable feature that can be timed.
    */
  type PlumeStatistic = Value

  val TIME_OPEN_DRIVER, TIME_CLOSE_DRIVER, TIME_EXTRACTION, TIME_REACHABLE_BY_QUERYING,
  TIME_REMOVING_OUTDATED_GRAPH, TIME_REMOVING_OUTDATED_CACHE, TIME_RETRIEVING_CACHE,
  TIME_STORING_CACHE, PROGRAM_CLASSES, PROGRAM_METHODS = Value

  private val statistics: mutable.Map[PlumeStatistic, Long] =
    PlumeStatistics.values.map((_, 0L)).to(collection.mutable.Map)

  /** Measures the time for a given code block and saves it under the [[PlumeStatistic]] heading.
    * @param statistic the statistic to save the elapsed time under.
    * @param block the code block to measure.
    * @tparam R the return value of the code block.
    * @return the result of the code block.
    */
  def time[R](statistic: PlumeStatistic, block: => R): R = {
    val t0     = System.nanoTime()
    val result = block
    val t1     = System.nanoTime()
    statistics.put(statistic, t1 - t0)
    result
  }

  /** Sets the value of a statistic.
    * @param statistic The statistic to set.
    * @param newValue The new value to associate with the statistic.
    */
  def setMetric(statistic: PlumeStatistic, newValue: Long): Unit =
    statistics.put(statistic, newValue)

  /** The results of the measured time per process.
    * @return a map of each [[PlumeStatistic]] and the time per process in nanoseconds.
    */
  def results(): Map[PlumeStatistic, Long] = statistics.toMap

  /** Sets all the clocks back to 0.
    */
  def reset(): Unit = {
    statistics.clear()
    PlumeStatistics.values.map((_, 0L)).foreach { case (v, t) => statistics.put(v, t) }
  }

}

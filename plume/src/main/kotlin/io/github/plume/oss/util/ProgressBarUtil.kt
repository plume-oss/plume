package io.github.plume.oss.util

import me.tongfei.progressbar.ProgressBar
import me.tongfei.progressbar.ProgressBarStyle
import org.apache.logging.log4j.Level
import java.time.Duration
import java.time.temporal.ChronoUnit

object ProgressBarUtil {

    fun runInsideProgressBar(level: Level, pbName: String, barMax: Long, f: (pb: ProgressBar?) -> Unit) {
        if (level == Level.TRACE || level == Level.DEBUG || level == Level.INFO) {
            ProgressBar(
                pbName,
                barMax,
                1000,
                System.out,
                ProgressBarStyle.ASCII,
                "",
                1,
                false,
                null,
                ChronoUnit.SECONDS,
                0L,
                Duration.ZERO
            ).use { pb -> f(pb) }
        } else {
            f(null)
        }
    }
}
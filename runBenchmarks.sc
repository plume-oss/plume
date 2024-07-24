import java.io.{BufferedReader, File, FileReader}
import java.nio.file.{Files, Path}
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, blocking}
import scala.jdk.CollectionConverters.*
import scala.sys.process.*
import scala.util.boundary
import scala.util.boundary.break

// Combinations of driver, project, Gb mem, known to OOM
val oomCombinations: Set[(String, String, Int)] = Set(("tinkergraph", "compress", 2))
val drivers                                     = Seq("flatgraph")

@main def main(): Unit = {
  println("[info] Ensuring compilation status and benchmark dataset availability...")
  "sbt compile benchmarkDownloadTask".!

  val datasetDir = Path.of("workspace", "defects4j")
  val resultsDir = Path.of("results").createDirIfNotExists

  def benchmarkArgs(driver: String, project: String, memGb: Int): JmhProcessInfo = {
    val projectDir       = Path.of(datasetDir.toString, project)
    val projectName      = project.toLowerCase.stripSuffix(".jar")
    val driverResultsDir = Path.of(resultsDir.toString, driver, projectName).createDirIfNotExists
    val resultsPath      = Path.of(driverResultsDir.toString, s"results-Xmx${memGb}G")
    val outputPath       = Path.of(driverResultsDir.toString, s"output-Xmx${memGb}G")
    val (writeOutputFile, readOutputFile) =
      (Path.of(s"$outputPath-write.txt").toFile, Path.of(s"$outputPath-read.txt").toFile)
    val resultsExist =
      Path.of(s"$resultsPath-read.csv").toFile.exists() && Path.of(s"$outputPath-read.txt").toFile.exists()
    val cmd =
      s"Jmh/runMain com.github.plume.oss.Benchmark $driver ${projectDir.toAbsolutePath} -o ${outputPath.toAbsolutePath} -r ${resultsPath.toAbsolutePath} -m $memGb"
    JmhProcessInfo(cmd, resultsExist, writeOutputFile, readOutputFile)
  }

  println("[info] Available projects:")
  val projects = Files.list(datasetDir).filter(_.toString.endsWith(".jar")).toList.asScala.toList
  projects.foreach(p => println(s" - ${p.getFileName.toString}"))

  println("[info] Drivers to be benchmarked:")
  drivers.foreach(d => println(s" - $d"))

  val memoryConfigs = Seq(2, 4, 6, 8)

  memoryConfigs.reverse.foreach { memConfig =>
    drivers.foreach { driver =>
      projects.foreach { project =>
        val projectName = project.getFileName.toString.toLowerCase.stripSuffix(".jar")
        if (oomCombinations.contains(driver, projectName, memConfig)) {
          println(
            s"[info] '$driver' on project '$project' with `-Xmx${memConfig}G` will cause an OutOfMemoryException. Skipping..."
          )
        } else {
          val JmhProcessInfo(cmd, resultsExist, writeOutputFile, readOutputFile) =
            benchmarkArgs(driver, project.getFileName.toString, memConfig)
          if (resultsExist) {
            println(
              s"[info] Results for '$driver' on project '$project' with `-Xmx${memConfig}G` already exist. Skipping..."
            )
          } else {
            println(s"[info] Benchmarking '$driver' on project '$project' with `-Xmx${memConfig}G`")
            runAndMonitorBenchmarkProcess(cmd, writeOutputFile, readOutputFile)
          }
        }
      }
    }
  }
}

def sendCtrlCSignal(processId: Long): Unit = {
  val osName = System.getProperty("os.name").toLowerCase
  if (osName.contains("win")) {
    // Windows command to send Ctrl+C signal
    Runtime.getRuntime.exec(Array("taskkill", "-/PID", processId.toString, "/T", "/F"))
  } else {
    // Unix-based command to send SIGINT signal (Ctrl+C)
    Runtime.getRuntime.exec(Array("kill", "-SIGINT", processId.toString))
  }
}

def runAndMonitorBenchmarkProcess(cmd: String, writeOutputFile: File, readOutputFile: File): Unit = {
  writeOutputFile.createIfNotExists
  readOutputFile.createIfNotExists

  val sbtFile = File(writeOutputFile.getAbsolutePath.stripSuffix("write.txt") + "sbt.txt")
  val processBuilder = new java.lang.ProcessBuilder("sbt", cmd).redirectOutput(sbtFile)

  // Ignore locks for aborted JMH processes
  val env = processBuilder.environment
  env.put("SBT_OPTS", "-Djmh.ignoreLock=true")

  // Start the process
  val process   = processBuilder.start()
  val processId = process.pid()

  def readLogsForErrors(file: File): Boolean = {
    val reader          = new BufferedReader(new FileReader(file))
    var shouldTerminate = false
    try {
      var line: String = null
      while ({ line = reader.readLine(); line != null }) {
//        if (line.contains("benchmark timed out")) {
//          println("Timeout detected. Sending Ctrl+C signal to process...")
//          shouldTerminate = true
//        }
        if (line.contains("java.lang.OutOfMemoryError")) {
          println("OutOfMemoryError detected. Sending Ctrl+C signal to process...")
          shouldTerminate = true
        }

        if (shouldTerminate) {
          sendCtrlCSignal(processId)
        }
      }
    } finally {
      reader.close()
    }
    shouldTerminate
  }

  // Monitor the output file for timeout/error messages
  var shouldTerminate = false
  while (!shouldTerminate && process.isAlive) {
    Thread.sleep(5000)
    shouldTerminate = readLogsForErrors(writeOutputFile) || readLogsForErrors(readOutputFile) || readLogsForErrors(sbtFile)
  }
}

implicit class PathExt(x: Path) {
  def createDirIfNotExists: Path = {
    if (!Files.exists(x)) Files.createDirectories(x)
    x
  }
}

implicit class FileExt(x: File) {
  def createIfNotExists: File = {
    if (!x.exists()) {
      if (!x.getParentFile.exists()) x.getParentFile.mkdirs()
      x.createNewFile()
    }
    x
  }
}

case class JmhProcessInfo(cmd: String, resultsExist: Boolean, writeOutputFile: File, readOutputFile: File)

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
val drivers = Seq("flatgraph")

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
    val sbtFile = Path.of(s"$outputPath-sbt.txt").toFile
    val existingAttempt =
      sbtFile.exists() && !readLogsForJmhClassLoaderError(sbtFile) // If there is a jmh error we retry

    val driverArgs = databaseToStorageLocation(driver)
    val cmd =
      s"Jmh/runMain com.github.plume.oss.Benchmark $driver ${projectDir.toAbsolutePath} -o ${outputPath.toAbsolutePath} -r ${resultsPath.toAbsolutePath} -m $memGb $driverArgs"
    JmhProcessInfo(cmd, existingAttempt, writeOutputFile, readOutputFile)
  }

  println("[info] Available projects:")
  val projects =
    Files.list(datasetDir).filter(_.toString.endsWith(".jar")).toList.asScala.sortBy(_.toFile.length()).toList
  projects.foreach(p => println(s" - ${p.getFileName.toString}"))

  println("[info] Drivers to be benchmarked:")
  drivers.foreach(d => println(s" - $d"))

  val memoryConfigs = Seq(2, 4, 6, 8)

  memoryConfigs.reverse.foreach { memConfig =>
    drivers.foreach { driver =>
      projects.foreach { project =>
        val projectName = project.getFileName.toString.toLowerCase.stripSuffix(".jar")
        val JmhProcessInfo(cmd, resultsExist, writeOutputFile, readOutputFile) =
          benchmarkArgs(driver, project.getFileName.toString, memConfig)
        if (resultsExist) {
          println(
            s"[info] An attempt for '$driver' on project '$projectName' with `-Xmx${memConfig}G` already exist. Skipping..."
          )
        } else {
          println(s"[info] Benchmarking '$driver' on project '$projectName' with `-Xmx${memConfig}G`")
          runAndMonitorBenchmarkProcess(cmd, driver, writeOutputFile, readOutputFile)
        }
      }
    }
  }
}

def readLogsForJmhClassLoaderError(file: File): Boolean = {
  val reader   = new BufferedReader(new FileReader(file))
  var hasError = false
  try {
    var line: String = null
    while ({
      line = reader.readLine();
      line != null
    }) {
      if (line.contains("jmhType.class (No such file or directory)")) {
        hasError = true
      }
    }
  } finally {
    reader.close()
  }
  hasError
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

def runAndMonitorBenchmarkProcess(cmd: String, driver: String, writeOutputFile: File, readOutputFile: File): Unit = {
  writeOutputFile.createIfNotExists
  readOutputFile.createIfNotExists

  val sbtFile        = File(writeOutputFile.getAbsolutePath.stripSuffix("write.txt") + "sbt.txt")
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
    shouldTerminate =
      readLogsForErrors(writeOutputFile) || readLogsForErrors(readOutputFile) || readLogsForErrors(sbtFile)
  }
  // Check file size if termination ended without error
  if (!shouldTerminate) {
    var retryCount = 0
    val storageLoc = File(databaseToStorageLocation(driver).split(' ').last)
    val outputPath = Path.of(writeOutputFile.getParentFile.getAbsolutePath, "storage_size.txt")
    while (!storageLoc.exists() && retryCount < 3) {
      println(s"$storageLoc not found, waiting and retrying...")
      Thread.sleep(2000)
      retryCount += 1
    }
    if (!outputPath.toFile.exists() && storageLoc.exists()) {
      val size = getFileSize(storageLoc)
      println(s"${storageLoc.getAbsolutePath} is $size bytes large")
      outputPath.toFile.createIfNotExists
      Files.writeString(outputPath, size.toString)
      // clear storage
      deleteFileOrDir(storageLoc)
    }
  }

}

def deleteFileOrDir(file: File): Unit = {
  Option(file.listFiles).foreach { contents =>
    contents.filterNot(f => Files.isSymbolicLink(f.toPath)).foreach(deleteFileOrDir)
  }
  file.delete
}

def getFileSize(f: File) = {
  if (f.isFile) {
    f.length()
  } else {
    Files.walk(f.toPath).toList.asScala.map(_.toFile.length()).sum
  }
}

def databaseToStorageLocation(d: String): String = {
  d match {
    case "overflowdb"     => "--storage-location cpg.odb"
    case "flatgraph"      => "--storage-location cpg.fg"
    case "tinkergraph"    => "--export-path cpg.xml"
    case "neo4j-embedded" => "--databaseDir neo4j-db"
    case _                => ""
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

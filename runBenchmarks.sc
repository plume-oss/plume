import java.io.{BufferedReader, File, FileReader}
import scala.sys.process.*
import java.nio.file.Files
import java.nio.file.Path
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, blocking}
import scala.jdk.CollectionConverters.*
import scala.concurrent.ExecutionContext.Implicits.global

// Combinations of driver, project, Gb mem, known to OOM
val oomCombinations: Set[(String, String, Int)] = Set(("tinkergraph", "compress", 2))
val drivers = Seq(
  "overflowdb",
  "tinkergraph",
  "neo4j-embedded"
)

@main def main(): Unit = {
  println("[info] Ensuring compilation status and benchmark dataset availability...")
  "sbt compile benchmarkDownloadTask".!

  val datasetDir = Path.of("workspace", "defects4j")
  val resultsDir = Path.of("results").createIfNotExists

  def benchmarkArgs(driver: String, project: String, memGb: Int): JmhProcessInfo = {
    val projectDir       = Path.of(datasetDir.toString, project)
    val projectName      = project.toLowerCase.stripSuffix(".jar")
    val driverResultsDir = Path.of(resultsDir.toString, driver, projectName).createIfNotExists
    val resultsPath      = Path.of(driverResultsDir.toString, s"results-Xmx${memGb}G")
    val outputPath       = Path.of(driverResultsDir.toString, s"output-Xmx${memGb}G")
    val resultsExist =
      Path.of(s"$resultsPath-read.csv").toFile.exists() && Path.of(s"$outputPath-read.txt").toFile.exists()
    val cmd =
      s"Jmh/runMain com.github.plume.oss.Benchmark $driver $projectDir -o ${outputPath.toAbsolutePath} -r ${resultsPath.toAbsolutePath} -m $memGb"
    JmhProcessInfo(cmd, resultsExist, outputPath.toFile)
  }

  println("[info] Available projects:")
  val projects = Files.list(datasetDir).filter(_.toString.endsWith(".jar")).toList.asScala.toList
  projects.foreach(p => println(s" - ${p.getFileName.toString}"))

  println("[info] Available drivers:")
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
          val JmhProcessInfo(cmd, resultsExist, outputFile) =
            benchmarkArgs(driver, project.getFileName.toString, memConfig)
          if (resultsExist) {
            println(
              s"[info] Results for '$driver' on project '$project' with `-Xmx${memConfig}G` already exist. Skipping..."
            )
          } else {
            println(s"[info] Benchmarking '$driver' on project '$project' with `-Xmx${memConfig}G`")
            runAndMonitorBenchmarkProcess(cmd, outputFile)
          }
        }
      }
    }
  }
}

def runAndMonitorBenchmarkProcess(cmd: String, outputFile: File): Unit = {
  val processBuilder = Process(s"sbt \"$cmd\"")

  // Start the process
  val process = processBuilder.#>(outputFile).run()

  // Monitor the output file for timeout messages
  val timeoutFuture = Future {
    blocking {
      val reader = new BufferedReader(new FileReader(outputFile))
      try {
        var line: String = null
        while ({
          line = reader.readLine(); line != null
        }) {
          println(line) // Log the output
          if (line.contains("benchmark timed out")) {
            println("Timeout detected. Terminating process...")
            process.destroy()
            return
          } else if (line.contains("java.lang.OutOfMemoryError")) {
            println("OutOfMemory error detected. Terminating process...")
            process.destroy()
            return
          }
        }
      } finally {
        reader.close()
      }
    }
  }

  // Wait for the process to finish or timeout monitoring to detect a timeout
  Await.result(timeoutFuture, Duration.Inf)
}

implicit class PathExt(x: Path) {
  def createIfNotExists: Path = {
    if (!Files.exists(x)) Files.createDirectories(x)
    x
  }
}

case class JmhProcessInfo(cmd: String, resultsExist: Boolean, outputFile: File)

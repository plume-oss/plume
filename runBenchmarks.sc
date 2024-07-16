import scala.sys.process.*
import java.nio.file.Files
import java.nio.file.Path
import scala.jdk.CollectionConverters.*

@main def main(): Unit = {
  println("[info] Ensuring compilation status and benchmark dataset availability...")
  "sbt compile benchmarkDownloadTask".!

  val datasetDir = Path.of("workspace", "defects4j")
  val resultsDir = Path.of("results").createIfNotExists

  def benchmarkArgs(driver: String, project: String, memGb: Int): String = {
    val projectDir = Path.of(datasetDir.toString, project)
    val projectName = project.toLowerCase.stripSuffix(".jar")
    val driverResultsDir = Path.of(resultsDir.toString, driver, projectName).createIfNotExists
    val resultsPath = Path.of(driverResultsDir.toString, s"results-Xmx${memGb}G")
    val outputPath = Path.of(driverResultsDir.toString, s"output-Xmx${memGb}G")
    s"Jmh/runMain com.github.plume.oss.Benchmark $driver $projectDir -o ${outputPath.toAbsolutePath} -r ${resultsPath.toAbsolutePath} -m $memGb"
  }

  println("[info] Available projects:")
  val projects = Files.list(datasetDir).filter(_.toString.endsWith(".jar")).toList.asScala.toList
  projects.foreach(p => println(s" - ${p.getFileName.toString}"))

  println("[info] Available drivers:")
  val drivers = Seq("overflowdb", "tinkergraph", "neo4j-embedded")
  drivers.foreach(d => println(s" - $d"))

  val memoryConfigs = Seq(4, 8, 16)

  memoryConfigs.foreach { memConfig =>
    drivers.foreach { driver =>
      projects.foreach { project =>
        val cmd = benchmarkArgs(driver, project.getFileName.toString, memConfig)
        println(s"[info] Benchmarking '$driver' on project '$project' with `-Xmx${memConfig}G`")
        s"sbt \"$cmd\"".!
      }
    }
  }
}

implicit class PathExt(x: Path) {
  def createIfNotExists: Path = {
    if (!Files.exists(x)) Files.createDirectories(x)
    x
  }
}
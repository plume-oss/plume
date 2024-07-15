import scala.sys.process.*
import java.nio.file.Files
import java.nio.file.Path
import scala.jdk.CollectionConverters.*

@main def main(): Unit = {
  println("[info] Ensuring compilation status and benchmark dataset availability...")
  "sbt compile benchmarkDownloadTask".!

  val datasetDir = Path.of("workspace", "defects4j")
  val resultsDir = Path.of("results")

  if (!Files.exists(resultsDir)) Files.createDirectory(resultsDir)

  def benchmarkArgs(driver: String, project: String): String = {
    val projectDir = Path.of(datasetDir.toString, project)
    val projectName = project.toLowerCase.stripSuffix(".jar")
    val resultsPath = Path.of(resultsDir.toString, s"results-$driver-$projectName")
    val outputPath = Path.of(resultsDir.toString, s"output-$driver-$projectName")
    s"Jmh/runMain com.github.plume.oss.Benchmark $driver $projectDir -o ${outputPath.toAbsolutePath} -r ${resultsPath.toAbsolutePath}"
  }

  println("[info] Available projects:")
  val projects = Files.list(datasetDir).filter(_.toString.endsWith(".jar")).toList.asScala.toList
  projects.foreach(p => println(s" - ${p.getFileName.toString}"))

  println("[info] Available drivers:")
  val drivers = Seq("overflowdb")
  drivers.foreach(d => println(s" - $d"))

  drivers.foreach { driver =>
    projects.foreach { project =>
      val cmd = benchmarkArgs(driver, project.getFileName.toString)
      println(s"[info] Benchmarking '$driver' on project '$project'")
      s"sbt \"$cmd\"".!
    }
  }
}

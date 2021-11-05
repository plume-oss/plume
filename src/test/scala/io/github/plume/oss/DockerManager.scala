package io.github.plume.oss


import org.slf4j.LoggerFactory
import java.io.{File => JavaFile}
import scala.collection.mutable.ListBuffer
import scala.sys.process.{Process, ProcessLogger, stringSeqToProcess}

object DockerManager {

  private val logger = LoggerFactory.getLogger(classOf[Jimple2Cpg])

  def toDockerComposeFile(dbName: String): JavaFile =
    new JavaFile(getClass.getResource(s"/docker/$dbName.yml").toURI)

  def closeAnyDockerContainers(dbName: String): Unit = {
    logger.info(s"Stopping Docker services for $dbName...")
    val dockerComposeUp = Process(Seq("docker-compose", "-f", toDockerComposeFile(dbName).getAbsolutePath, "down"))
    dockerComposeUp.run(ProcessLogger(_ => ()))
  }

  def startDockerFile(dbName: String, containers: List[String] = List.empty[String]): Unit = {
    val healthChecks = ListBuffer.empty[String]
    if (containers.isEmpty) healthChecks += dbName else healthChecks ++= containers
    logger.info(s"Docker Compose file found for $dbName, starting...")
    closeAnyDockerContainers(dbName) // Easiest way to clear the db
    Thread.sleep(3000)
    println(toDockerComposeFile(dbName).getAbsolutePath)
    val dockerComposeUp = Process(
      Seq("docker-compose", "-f", toDockerComposeFile(dbName).getAbsolutePath, "up", "--remove-orphans")
    )
    logger.info(s"Starting process $dockerComposeUp")
    dockerComposeUp.run(ProcessLogger(_ => ()))
    var status: Byte = 1
    while (status == 1) {
      Thread.sleep(4000)
      status = healthChecks.map(performHealthCheck).foldRight(0)(_ | _).toByte
    }
  }

  def performHealthCheck(containerName: String): Byte = {
    val healthCheck = Seq("docker", "inspect", "--format='{{json .State.Health}}'", containerName)
    try {
      val x: String = healthCheck.!!.trim
      val jsonRaw = x.substring(1, x.length - 1)
      io.circe.parser.parse(jsonRaw) match {
        case Left(failure) => logger.warn(failure.message, failure.underlying)
        case Right(json) =>
          json.\\("Status").head.asString match {
            case Some("unhealthy") => logger.info(s"$containerName is unhealthy")
            case Some("starting")  => logger.info(s"$containerName is busy starting")
            case Some("healthy")   => logger.info(s"$containerName is healthy!"); return 0
            case Some(x)           => logger.info(s"Container $containerName is $x")
            case None              => logger.warn(s"Unable to obtain container health for $containerName.")
          }
      }
    } catch {
      case _: StringIndexOutOfBoundsException => // Usually happens just as the services have been created
      case e: IllegalAccessError              => e.printStackTrace()
      case e: RuntimeException =>
        logger.warn(s"${e.getMessage}. This may be due to the container still being created.")
    }
    1
  }

}
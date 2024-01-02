ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.12.18"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "3.5.0",
  "org.apache.spark" %% "spark-sql" % "3.5.0",
  "org.apache.logging.log4j" % "log4j-api" % "2.20.0",
  "org.apache.logging.log4j" % "log4j-core" % "2.20.0",
  "org.mongodb.scala" %% "mongo-scala-driver" % "4.9.0",
  "org.mongodb.spark" %% "mongo-spark-connector" % "3.0.1",
  "org.apache.spark" %% "spark-streaming" % "3.5.0" % "provided",
  "org.apache.spark" %% "spark-streaming-kafka-0-10" % "3.5.0",
  "org.apache.kafka" %% "kafka" % "3.5.0", // Kafka for Spark Streaming
  "org.apache.kafka" % "kafka-clients" % "3.1.0" // Kafka clients for producer and consumer
)

lazy val root = (project in file("."))
  .settings(
    name := "KafkaProj"
  )
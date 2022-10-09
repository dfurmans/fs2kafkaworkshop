val scala3Version = "3.2.0"

lazy val root = project
  .in(file("."))
  .settings(
    name := "fs2KafkaWorkshop",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "0.7.29" % Test,
      "com.github.fd4s" %% "fs2-kafka" % "3.0.0-M8",
      "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.19.0"
    )
  )

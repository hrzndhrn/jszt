lazy val root = (project in file(".")).settings(
  name := "jszt",
  version := "0.0.0",
  scalaVersion := "2.11.7",

  libraryDependencies ++= Seq(
    "com.github.scopt" %% "scopt" % "3.3.0"
  )
)



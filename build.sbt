lazy val root = (project in file(".")).settings(
  name := "jszt",
  version := "0.0.2",
  scalaVersion := "2.11.7",

  libraryDependencies ++= Seq(
    "com.github.scopt" %% "scopt" % "3.3.0",
    "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4",
    "com.typesafe" % "config" % "1.3.0",
    "org.mozilla" % "rhino" % "1.7.7",
    "com.google.javascript" % "closure-compiler" % "v20141023"
  )
)



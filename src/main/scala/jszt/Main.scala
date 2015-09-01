package jszt

import java.io.File

case class Config(
  path: File,
  libPath: File,
  mainScript: File
)

case class Options(
  configFileName: Option[String] = None,
  path: Option[String] = None
)




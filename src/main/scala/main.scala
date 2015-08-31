import java.io.File

import com.typesafe.config.{ConfigValueFactory, ConfigFactory}
import jszt.CmdLine
import scopt._

object main {
  def main(args: Array[String]) = {
    val parser = new OptionParser[CmdLine]("scopt") {
      head("jszt", "0.0.1")
      opt[String]("conf") action { (value, opts) =>
        opts.copy(configFileName = Option(value))
      } text "The jszt config file."
      arg[String]("<path>") unbounded() optional() action { (value, opts) =>
        opts.copy(path = Option(value))
      } text "The path to the jsz-project."
    }

    parser.parse(args, CmdLine()) match {
      case Some(cmdLine) => run(cmdLine)
      case None => sys.exit(1)
    }
  }

  def getCurrentDirectory = new java.io.File(".").getCanonicalPath

  def run(cmdLine: CmdLine) = {
    val config = cmdLine match {
      case CmdLine(Some(configFileName), Some(projectDir)) =>
        ConfigFactory.parseFile(new File(configFileName))
          .withFallback(ConfigFactory.load())
      case CmdLine(None, Some(projectDir)) =>
        ConfigFactory.load()
          .withValue("project.dir", ConfigValueFactory.fromAnyRef(projectDir))
      case CmdLine(Some(configFileName), None) =>
        ConfigFactory.parseFile(new File(configFileName))
          .withFallback(ConfigFactory.load())
      case _ => ConfigFactory.load()
    }

    println(config.getConfig("project").root().render())
  }

}
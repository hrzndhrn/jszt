import java.io.File

import com.typesafe.config.{Config, ConfigValueFactory, ConfigFactory}
import jszt.Scripts
import scopt._
import java.nio.file.Paths
import scala.collection.JavaConverters._

object main {
  def main(args: Array[String]) = {
    optionParser.parse(args, Options()) match {
      case Some(cmdLine) => run(config(cmdLine))
      case None => sys.exit(1)
    }
  }

  private val optionParser = new OptionParser[Options]("jszt") {
    head("jszt", "0.0.1")
    opt[String]("conf") action { (value, opts) =>
      opts.copy(configFileName = Option(value))
    } text "The jszt config file."
    arg[String]("<path>") unbounded() optional() action { (value, opts) =>
      opts.copy(path = Option(value))
    } text "The path to the jsz-project."
  }

  private def config(cmdLine: Options): Config = {
    cmdLine match {
      case Options(Some(configFileName), Some(projectDir)) =>
        ConfigFactory.parseFile(new File(configFileName))
          .withFallback(ConfigFactory.load())
      case Options(None, Some(projectDir)) =>
        ConfigFactory.load()
          .withValue("project.dir", ConfigValueFactory.fromAnyRef(projectDir))
      case Options(Some(configFileName), None) =>
        ConfigFactory.parseFile(new File(configFileName))
          .withFallback(ConfigFactory.load())
      case _ => ConfigFactory.load()
    }
  }

  private def run(config: Config): Unit = {
    val project: Config = config.getConfig("project")
    val dir = new File(project.getString("dir"))
    val libPath = project.getString("libPath")
    val mainScriptName = Paths.get(libPath,
      project.getString("mainScript")).toString

    val scripts = new Scripts(dir, mainScriptName)

    project.getConfigList("concat").asScala foreach { concat: Config =>
      scripts.concat(
        concat.getString("script"),
        concat.getStringList("scripts").asScala.toList,
        concat.getBoolean("infos"),
        concat.getString("preamble")
      ).save
    }

    /*
    scripts.get("lib.jsz.core")
      .pack.save
      .minify.save
    */

    /*
    scripts.get("demo.jsz.loadedScripts")
      .pack.save
      .minify.save
    */
  }

}

case class Options(configFileName: Option[String] = None,
                   path: Option[String] = None)

import java.io.File
import java.util

import com.typesafe.config.{Config, ConfigValueFactory, ConfigFactory}
import jszt.{closure, Scripts, Options}
import scopt._
import jszt.io.{exists, listFilesInDir, listJsFilesInTree, read}
import java.nio.file.Paths
import scala.collection.JavaConverters._
import scala.collection.mutable


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

  private def config(cmdLine: Options):Config = {
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

  private def run(config:Config):Unit = {
    val project:Config = config.getConfig("project")
    // println(project.root().render())

    // val dir = new File(project.getString("dir"))
    val dir = new File("/Users/kruse/Projects/jsz/public/javascripts")
    val libPath = project.getString("libPath")
    val mainScriptName = Paths
      .get(libPath, project.getString("mainScript")).toString

    val scripts = new Scripts(dir, mainScriptName)
    // scripts.debug
    // val concats =

    println("dir: " + dir.getCanonicalPath)

    project.getConfigList("concat").asScala foreach { concat:Config =>
      val script = scripts.concat(
        concat.getString("script"),
        concat.getStringList("scripts").asScala.toList,
        concat.getBoolean("infos"),
        concat.getString("preamble")
      )
      script.save
      println("-----")
      println(script.content)
      println("-----")
      println( script.minify.content)
      println(script.minify.file.getCanonicalPath)
      // println( script.nodes(0).toSource().replaceAll("""\s+""", " ").replaceAll("""\n""", ""))
      // println(closure.minify(script.content))
      // closure.minify(script.content)

    }

    scripts.pack("lib.jsz.core")
    scripts.pack("demo.jsz.loadedScripts")

    println(">> ready")

/*
    concats.forEach({
      con: Config => println(con) //
    });*/
  }



}
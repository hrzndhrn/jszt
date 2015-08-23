import jszt.CommandLineOptions
import scopt._

object main {
  def main(args: Array[String]) = {
    val parser = new OptionParser[CommandLineOptions]("scopt") {
      head("jszt", "0.0.1")
      opt[String]("conf") action { (value, opts) =>
        opts.copy(configFileName = Option(value))
      } text "The jszt config file."
      arg[String]("<path>") unbounded() optional() action { (value, opts) =>
        opts.copy(path = Option(value))
      } text "The path to the jsz-project."
    }

    parser.parse(args, CommandLineOptions()) match {
      case Some(commandLineOptions) => run(commandLineOptions)
      case None => sys.exit(1)
    }
  }

  def run(commandLineOptions: CommandLineOptions) = {
    println(commandLineOptions)
  }

}
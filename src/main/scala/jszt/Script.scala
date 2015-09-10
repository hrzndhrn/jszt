package jszt

import java.io.File

import closure.compiler
import jszt.io._
import org.mozilla.javascript.Token
import org.mozilla.javascript.ast._
import rhino.{parser, NodeToListVisitor}
import scala.collection.JavaConverters._


class Script(private val scripts: Scripts,
             private val file: File,
             val initialContent: Option[String]) {

  // The content of the js-script. 
  val content: String = initialContent.getOrElse(read(file))

  val nodes = {
    val visitor = new NodeToListVisitor

    try {
      parser.parse(content).visitAll(visitor)
    } catch {
      case error: Exception =>
        throw new RuntimeException("Parse error!" + error.getMessage)
    }

    visitor.getNodes
  }

  // Search the function call "script" in the js-file and read options.
  private val scriptOptions = getFunctionCallsByName("script").headOption match {
    case Some(node: FunctionCall) => objectLiteralToMap(node.getArguments.get(0))
    case _ => Map[String, Any]()
  }

  /**
   * The script name in dot notation
   */
  // val name = scriptOptions.getOrElse("name", fileNameToScriptName).toString
  val name = fileNameToScriptName

  /**
   *   A list of required script names in dot notation.
   */
  val require = scriptOptions.get("require") match {
    case Some(list: List[Any]) => list map {
      _.toString
    }
    case _ => List[String]()
  }

  /**
   * This methods converts the file name to a script name.
   * e.g.
   * @return
   */
  def fileNameToScriptName: String = {
    val fileName = file.getCanonicalPath
    val dirPath = scripts.dir.getCanonicalPath

    fileName
      .substring(dirPath.length + 1, fileName.length - 3)
      .replace("/", ".")
  }

  /**
   * Pack this script.
   * @return A new script with the suffix pack.js
   */
  def pack:Script = scripts.pack(this.name)

  /**
   * The method pack generate a list of script names of dependencies for this
   * script.
   *
   * @param listOfScripts A list of scripts to pack together.
   * @return
   */
  def pack(listOfScripts: List[String]): List[String] = {
    def _pack(l: List[String]): List[String] =
      l.flatMap({ req => scripts.get(req).pack(List()) }) ++ l

    (_pack(listOfScripts) // Pack all scripts and their requirements together.
      ++ _pack(require) // Add this requirements and their requirements.
      ++ List(name) // Add the actual script.
      ).distinct
  }

  /**
   * This methods returns a minimized script of this script. The new script file
   * name ends with min.js.
   * @return
   */
  def minify: Script = {
    val newFileName: String = file.getCanonicalPath match {
      case name: String if name.endsWith("pack.js") =>
        name.replace("pack.js", "min.js")
      case name: String if name.endsWith(".js") =>
        name.replace(".js", "min.js")
      case name: String =>
        throw new RuntimeException("Wrong file name: " + name)
      case _ => throw new RuntimeException("Error can not get file name.")
    }

    Script(scripts, new File(newFileName), compiler.minify(content))
  }

  private def getFunctionCallsByName(functionName: String): List[FunctionCall] = {
    nodes.filter({
      case node: FunctionCall =>
        node.getTarget match {
          case name: Name => name.getIdentifier == functionName
          case _ => false
        }
      case _ => false
    }).map {
      node => node.asInstanceOf[FunctionCall]
    }
  }

  private def objectLiteralToMap(objectLiteral: AstNode): Map[String, Any] = {
    objectLiteral match {
      case obj: ObjectLiteral => {
        obj.getElements.asScala.toList map { property: ObjectProperty =>
          val name = property.getLeft.asInstanceOf[Name].getIdentifier
          property.getRight match {
            case array: ArrayLiteral => name -> arrayLiteralToList(array)
            case string: StringLiteral => name -> string.getValue
            case keyword: KeywordLiteral => name -> keywordToValue(keyword)
            case unary: UnaryExpression => {
              unary.toSource() match {
                case "!0" => name -> true
                case "!1" => name -> false
                case _ => name -> unary
              }
            }
            case node => name -> node
          }
        }
      }.toMap
      case _ => Map[String, Any]()
    }
  }

  private def arrayLiteralToList(array: ArrayLiteral): List[Any] = {
    array.getElements.asScala.toList map {
      case value: StringLiteral => value.getValue()
      case _ => null
    }
  }

  private def keywordToValue(keyword: KeywordLiteral): Any = {
    if (keyword.isBooleanLiteral) {
      keyword.getType == Token.TRUE
    }
    else {
      null
    }
  }

  def save(): Script = {
    write(file, content)
    this
  }

  // Concat two scripts.
  def +(script: Script): Script = {
    Script(this.scripts, this.file, this.content + "\n\n" + script.content)
  }

  override def toString = {
    val requires = require.mkString(", ")
    s"script: ${file.getCanonicalPath}\nname: $name\nrequire: $requires"
  }


}

object Script {
  def apply(scripts: Scripts, file: File) =
    new Script(scripts, file, None)

  def apply(scripts: Scripts, file: File, content: String) =
    new Script(scripts, file, Some(content))

  def apply(scripts: Scripts, fileName: String, content: String) =
    new Script(scripts, new File(scripts.dir, fileName), Some(content))
}
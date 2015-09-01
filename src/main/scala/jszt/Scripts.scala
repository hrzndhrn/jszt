package jszt

import java.io.File
import io.{listJsFilesInTree, read}
import org.mozilla.javascript.Token
import org.mozilla.javascript.ast._
import rhino.{parser, NodeToListVisitor}
import scala.collection.JavaConverters._

class Scripts(val dir: File, val mainScriptName: String) {
  private val scripts = listJsFilesInTree(dir) map {
    file => new Script(file)
  }

  def getMainScript:Option[Script] = scripts.find { script =>
    script.path.endsWith(mainScriptName)
  }

  def debug = {
    scripts.foreach {println(_)}
    println("mainScript: " + mainScriptName)
    println(getMainScript.getOrElse("-"))
  }
}


class Script(val file: File) {
  val content = read(file)

  val path = file.getCanonicalPath

  val nodes = {
    val visitor = new NodeToListVisitor
    parser.parse(content).visitAll(visitor)
    visitor.getNodes
  }


  val scriptNode = getFunctionCallsByName("script").head
  val scriptOptions = objectLiteralToMap(scriptNode.getArguments.get(0))

  private def getFunctionCallsByName(callName: String): List[FunctionCall] = {
    nodes.filter({ node => node match {
      case node: FunctionCall => {
        node.getTarget match {
          case name: Name => name.getIdentifier == callName
          case _ => false
        }
      }
      case _ => false
    }
    }).map({ node => node.asInstanceOf[FunctionCall] })
  }

  private def objectLiteralToMap(objectLiteral: AstNode):Map[String,Any] = {
    objectLiteral match {
      case obj: ObjectLiteral => {
        obj.getElements.asScala.toList map { property: ObjectProperty =>
          val name = property.getLeft.asInstanceOf[Name].getIdentifier
          property.getRight match {
            case array: ArrayLiteral => (name -> arrayLiteralToList(array))
            case string: StringLiteral => (name -> string.getValue)
            case keyword:KeywordLiteral => (name -> keywordToValue(keyword))
          }
        }
      }.toMap
      case _ => Map[String, Any]()
    }
  }

  private def arrayLiteralToList(array:ArrayLiteral):List[Any] = {
    array.getElements.asScala.toList map { element =>
      element match {
        case value:StringLiteral => value.getValue()
        case _ => null
      }
    }
  }

  private def keywordToValue(keyword:KeywordLiteral):Any = {
    if (keyword.isBooleanLiteral) {
      keyword.getType == Token.TRUE
    }
    else {
      null
    }
  }

  override def toString = scriptOptions.mkString(", ")
}
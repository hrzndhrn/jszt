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

  def debug = {
    println(scripts(0).scriptOptions)
  }
}


class Script(val file: File) {
  println(file.getCanonicalPath)
  val content = read(file)

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
    println(objectLiteral.getAbsolutePosition)
    println(objectLiteral.toSource)
    objectLiteral match {
      case obj: ObjectLiteral => {
        obj.getElements.asScala.toList map { property: ObjectProperty =>
          val name = property.getLeft.asInstanceOf[Name].getIdentifier
          println(property.getRight.getClass.getSimpleName)
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
      keyword == Token.TRUE
    }
    else {
      null
    }
  }
}
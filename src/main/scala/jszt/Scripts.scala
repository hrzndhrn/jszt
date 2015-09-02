package jszt

import java.io.File
import io.{listJsFilesInTree, read}
import org.mozilla.javascript.Token
import org.mozilla.javascript.ast._
import rhino.{parser, NodeToListVisitor}
import scala.collection.JavaConverters._

class Scripts(val dir: File, val mainScriptName: String) {
  private val scripts = listJsFilesInTree(dir) map {
    file => new Script(this, file)
  }

  def getMainScript:Script = scripts.find { script =>
    script.path.endsWith(mainScriptName)
  }.get

  def debug = {
    scripts.foreach {println(_)}
    println("mainScript: " + mainScriptName)
    // println(getMainScript.getOrElse("-"))
    println("-------------")
    // println(getMainScript.pack)
    val pack = getMainScript.pack(List(getMainScript.name)).distinct
    pack.foreach({p => println(p) })
    println("--------")
    val pack2 = get("lib.jsz.core.dom").pack(List(getMainScript.name)).distinct
    pack2.foreach({p => println(p) })
    println("--------")
    val pack3 = get("demo.lightsOut").pack(List(getMainScript.name)).distinct
    pack3.foreach({p => println(p) })
    println("--------")
    val pack4 = get("test.jsz.observer").pack(List(getMainScript.name)).distinct
    pack4.foreach({p => println(p) })

    println(pack == pack2)
  }

  def get(name:String):Script = {
    scripts.find({ script => script.name == name}).get
  }
}


class Script(val scripts:Scripts, val file: File) {
  val content = read(file)

  val path = file.getCanonicalPath

  val nodes = {
    val visitor = new NodeToListVisitor
    parser.parse(content).visitAll(visitor)
    visitor.getNodes
  }


  val scriptNode = getFunctionCallsByName("script").head
  val scriptOptions = objectLiteralToMap(scriptNode.getArguments.get(0))
  val name = scriptOptions.getOrElse("name", "").toString
  val require = scriptOptions.get("require") match {
    case Some(list:List[Any]) => list map {_.toString}
    case _ => List[String]()
  }

  def pack(list:List[String]):List[String] = {
    //  val reqs = list ++ require
    def packIt(l:List[String]):List[String] = {
      { l flatMap({
        req => scripts.get(req).pack(List())
      })} ++ l
    }

    packIt(list) ++ packIt(require)
  }

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

  override def toString =
    scriptOptions.mkString(", ")
    // path
}
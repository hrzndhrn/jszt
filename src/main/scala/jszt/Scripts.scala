package jszt

import java.io.File
import com.google.javascript.jscomp.CompilerOptions.LanguageMode
import io.{listJsFilesInTree, read, write}
import org.mozilla.javascript.Token
import org.mozilla.javascript.ast._
import rhino.{parser, NodeToListVisitor}
import scala.collection.JavaConverters._
import com.google.javascript.jscomp.{Compiler, CompilerOptions, CompilationLevel, SourceFile}

class Scripts(val dir: File, val mainScriptName: String) {
  private val scripts = listJsFilesInTree(dir) map {
    file => Script(this, file)
  }

  def getMainScript: Script = scripts.find { script =>
    script.path.endsWith(mainScriptName)
  }.get

  def debug = {

    scripts.foreach {
      println(_)
    }
    println("mainScript: " + mainScriptName)
    // println(getMainScript.getOrElse("-"))

    println("-------------")
    // println(getMainScript.pack)
    val pack = getMainScript.pack(List(getMainScript.name)).distinct
    pack.foreach({ p => println(p) })
    println("--------")
    val pack2 = get("lib.jsz.core.dom").pack(List(getMainScript.name)).distinct
    pack2.foreach({ p => println(p) })
    println("--------")
    println(pack == pack2)
    /*val pack3 = get("demo.lightsOut").pack(List(getMainScript.name)).distinct
    pack3.foreach({p => println(p) })
    println("--------")
    val pack4 = get("test.jsz.observer").pack(List(getMainScript.name)).distinct
    pack4.foreach({p => println(p) })
    */

    /* merge
    scripts.foreach {println(_)}
    println("-----------------")
    val g =
      get("lib.jsz.core") +
      get("lib.jsz.core.script") +
      get("lib.jsz.core.config") +
      get("lib.jsz.core")
    // println(g.content)
    */
  }

  def concat(scriptFileName: String, scriptFileNames: List[String],
             infos: Boolean, preamble: String):Script = {

    val infoText =
      if (infos)
        "concat:\n" + scriptFileNames.mkString("\n")
      else
        ""

    val script = Script(this, scriptFileName,
      lineComment(preamble) + lineComment(infoText))

    scriptFileNames.foldLeft[Script](script){
      (script,name) =>
        script + get(fileNameToScriptName(name))
    }
  }

  def pack:Unit = pack(None)

  def pack(scriptName:String):Unit = pack(Some(scriptName))

  def pack(scriptName:Option[String]):Unit = {
    val packs:List[List[String]] = scriptName match {
      case Some(scriptName) =>
        List(get(scriptName).pack(List(getMainScript.name)))
      case None =>
        scripts.foldLeft (List[List[String]]()) { (list,script) =>
          script.pack(List(getMainScript.name)) :: list
        }.distinct
    }

    packs.foreach { pack =>
      val fileName = pack.last.replace(".", "/") + ".pack.js"
      val script = concat(fileName, pack, true,
        "DO NOT EDIT THIS FiLE! THIS FILE WILL BE GENERATED AN OVERWRITTEN.")
      script.save
      println(script.file.getCanonicalPath)
    }
  }

  def lineComment(string:String):String =
    if (string.trim == "")
      ""
    else
      string.split("\n").map({line => f"// $line" }).mkString("\n") + "\n"

  def fileNameToScriptName(fileName: String): String = {
    val dirName = dir.getCanonicalPath
    val startIndex = if (fileName.startsWith(dirName)) dirName.length - 1 else 0

    if (fileName.endsWith(".js"))
      fileName
        .substring(startIndex, fileName.length - 3)
        .replace("/", ".")
    else
      fileName

  }

  def get(name: String): Script = {
    // println("get " + name);
    scripts.find({ script => script.name == name }) match {
      case Some(script) => script
      case None => throw new RuntimeException(f"Can not find script $name.")
    }
  }
}


class Script(val scripts: Scripts, val file: File, con: Option[String]) {
  val content: String = con.getOrElse(read(file))

  val path = file.getCanonicalPath

  val nodes = {
    val visitor = new NodeToListVisitor

    try {
      parser.parse(content).visitAll(visitor)
    } catch {
      case e: Exception => {
        throw new RuntimeException("Parse error!")
      };
    }

    visitor.getNodes
  }


  val scriptNode = getFunctionCallsByName("script").headOption
  val scriptOptions = scriptNode match {
    case Some(node: FunctionCall) => objectLiteralToMap(node.getArguments.get(0))
    case _ => Map[String, Any]()
  }

  // println("> " + getScriptName)

  val name = scriptOptions.getOrElse("name", getScriptName).toString
  val require = scriptOptions.get("require") match {
    case Some(list: List[Any]) => list map {
      _.toString
    }
    case _ => List[String]()
  }

  def getScriptName: String = {
    val fileName = file.getCanonicalPath
    val dirPath = scripts.dir.getCanonicalPath

    fileName
      .substring(dirPath.length + 1, fileName.length - 3)
      .replace("/", ".")
  }

  def pack(list: List[String]): List[String] = {
    //  val reqs = list ++ require
    def packIt(l: List[String]): List[String] = {
      {
        l flatMap ({
          req => scripts.get(req).pack(List())
        })
      } ++ l
    }

    (packIt(list) ++ packIt(require) ++ List(name)).distinct
  }

  val jsPrefixRegEx = """.js$""".r

  def minify:Script =
    Script(
      scripts,
      jsPrefixRegEx.replaceAllIn( file.getCanonicalPath, ".min.js"),
      closure.minify(content))

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

  private def objectLiteralToMap(objectLiteral: AstNode): Map[String, Any] = {
    objectLiteral match {
      case obj: ObjectLiteral => {
        obj.getElements.asScala.toList map { property: ObjectProperty =>
          val name = property.getLeft.asInstanceOf[Name].getIdentifier
          property.getRight match {
            case array: ArrayLiteral => (name -> arrayLiteralToList(array))
            case string: StringLiteral => (name -> string.getValue)
            case keyword: KeywordLiteral => (name -> keywordToValue(keyword))
            case unary: UnaryExpression => {
              unary.toSource() match {
                case "!0" => (name -> true)
                case "!1" => (name -> false)
                case _ => (name -> unary)
              }
            }
            case node => (name -> node)
          }
        }
      }.toMap
      case _ => Map[String, Any]()
    }
  }

  private def arrayLiteralToList(array: ArrayLiteral): List[Any] = {
    array.getElements.asScala.toList map { element =>
      element match {
        case value: StringLiteral => value.getValue()
        case _ => null
      }
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

  def save:Unit = write(file, content)

  def +(script: Script): Script = {
    Script(this.scripts, this.file, this.content + "\n\n" + script.content)
  }

  override def toString =
    scriptOptions.mkString(", ")

  // path
}

object Script {
  def apply(scripts: Scripts, file: File) =
    new Script(scripts, file, None)

  def apply(scripts: Scripts, file: File, content: String) =
    new Script(scripts, file, Some(content))

  def apply(scripts: Scripts, fileName:String, content: String) =
    new Script(scripts, new File(scripts.dir, fileName), Some(content))
}

object closure {
  def minify(code:String):String = {
    val compiler = new Compiler
    compiler.disableThreads

    val options = new CompilerOptions
    CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(options)
    options.setLanguageIn(LanguageMode.ECMASCRIPT5_STRICT)

    val extern = SourceFile.builder.buildFromCode("extern.js", "")
    val input = SourceFile.builder.buildFromCode( "input.js", code)

    var min:String = ""
    try {
      compiler.compile( extern, input, options)
      min = compiler.toSource
    }
    catch {
      case e: Exception => println("exception caught: " + e);
    }

    // compiler.finalize
    println("--- minify ---")
    // println(min)
    // println("--------------")
    min

  }
}
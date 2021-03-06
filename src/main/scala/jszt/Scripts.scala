package jszt

import java.io.File
import io.listJsFilesInTree

class Scripts(val dir: File, val mainScriptFileName: String) {
  private val scripts = listJsFilesInTree(dir) map {
    file => Script(this, file)
  }

  val mainScript: Script = get(fileNameToScriptName(mainScriptFileName))

  def concat(scriptFileName: String, scriptFileNames: List[String],
             info: Boolean, preamble: String): Script = {

    val infoText =
      if (info)
        "concat:\n" + scriptFileNames.mkString("\n")
      else
        ""

    val script = Script(this, scriptFileName,
      lineComment(preamble) + lineComment(infoText))

    scriptFileNames.foldLeft[Script](script) {
      (script, name) =>
        script + get(fileNameToScriptName(name))
    }
  }

  private def concat(pack: List[String]):Script =
    concat(
      pack.last.replace(".", "/") + ".pack.js",
      pack,
      info = true,
      "DO NOT EDIT THIS FILE! THIS FILE WILL BE GENERATED AN OVERWRITTEN.")

  /**
   * Pack all scripts.
   * @return
   */
  def pack: List[Script] = {
    scripts.foldLeft(List[List[String]]())((list, script) =>
      script.pack(List(mainScript.name)) :: list
    ).distinct.map(pack => concat(pack))
  }

  /**
   * Pack a specific script.
   * @param scriptName The name of the script to pack.
   * @return
   */
  def pack(scriptName: String): Script =
    concat(get(scriptName).pack(List(mainScript.name)))

  def lineComment(string: String): String =
    if (string.trim == "")
      ""
    else
      string.split("\n").map({ line => f"// $line" }).mkString("\n") + "\n"

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
    scripts.find({ script => script.name == name }) match {
      case Some(script) => script
      case None => throw new RuntimeException(f"Can not find script $name.")
    }
  }
}
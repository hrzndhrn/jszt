package jszt

import java.io.File

import scala.io.Source

/**
 * Created by kruse on 01.09.15.
 */
object io {
  def exists(fileName:String) = new File(fileName).exists

  def listFilesInDir(file:File):List[File] =
    if(file.isDirectory) file.listFiles.toList else List[File]()

  def listFilesInTree(file:File):List[File] = {
    if (file.isDirectory) {
      val files = file.listFiles.toList
      val tree = files flatMap { file => listFilesInTree(file) }
      files ++ tree
    }
    else List[File]()
  }

  type FileFilter = (File => Boolean)

  def listJsFilesInTree(file:File):List[File] =
    listJsFilesInTree(file, jsztFilter)

  def listJsFilesInTree(file:File,fileFilter:FileFilter):List[File] =
    listFilesInTree(file) filter {
      file => jsFilter(file) && fileFilter(file)
    }

  def jsFilter(file:File) = file.getCanonicalPath.endsWith(".js")

  def jsztFilter(file:File) = {
    val path = file.getCanonicalPath
    !path.endsWith("min.js") && !path.endsWith("pack.js")
  }

  def read(file:File) = Source.fromFile(file).getLines.mkString("\n")

}

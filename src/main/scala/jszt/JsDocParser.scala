package jszt

import scala.util.parsing.combinator._


class JsDocParser extends JavaTokenParsers {

  def text: Parser[String] = """[^@]*""".r

  def static: Parser[String] = """@static""".r

  def version: Parser[String] = """@version""".r

  def desc: Parser[String] = """@desc""".r

  def description: Parser[String] = """@description""".r

  def staticTag: Parser[JsDocTag] = static ^^ {
    case _ => JsDocTagStatic(None)
  }

  def versionTag: Parser[JsDocTag] = version ~ opt(text) ^^ {
    case _ ~ text => JsDocTagVersion(text)
  }

  def descTag: Parser[JsDocTag] = (desc | description) ~ opt(text) ^^ {
    case _ ~ text => JsDocTagDesc(text)
  }

  def tag: Parser[JsDocTag] = staticTag | versionTag

  def start: Parser[JsDocTag] = tag | text ^^ { value: Any =>
    value match {
      case value: JsDocTag => value
      case value: String => JsDocTagDesc(Option(value))
    }
  }

  def doc: Parser[List[JsDocTag]] = start ~ tag.+ ^^ {
    case start ~ tags => start :: tags
  }
}


object jsDocParser extends JsDocParser {
  def parse(string: String) = parseAll(doc, string.trim)
}

trait JsDocTag {
  val text: Option[String]
}

case class JsDocTagUnknown(name: String, text: Option[String]) extends JsDocTag

case class JsDocTagDesc(text: Option[String]) extends JsDocTag

case class JsDocTagStatic(text: Option[String]) extends JsDocTag

case class JsDocTagVersion(text: Option[String]) extends JsDocTag


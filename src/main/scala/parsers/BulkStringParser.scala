package parsers

object BulkStringParser {
  def parseText(text: String): String = {
    text match {
      case "" => "$-1\r\n"
      case _ => s"$$${text.length}\r\n$text\r\n"
    }
  }

  def parseArray(array: Array[String]): String = {
    s"*${array.length}\r\n${array.map(parseText).mkString}"
  }

  def parse(value: Any): String = {
    value match {
      case text: String => parseText(text)
      case array: Array[String] => parseArray(array)
      case _ => ""
    }
  }
}

package parsers

trait ResponseTrait {
  private val b64EmptyRDB = "UkVESVMwMDEx+glyZWRpcy12ZXIFNy4yLjD6CnJlZGlzLWJpdHPAQPoFY3RpbWXCbQi8ZfoIdXNlZC1tZW3CsMQQAPoIYW9mLWJhc2XAAP/wbjv+wP9aog=="

  def getParsedResponse: String

  def getBytes: Array[Byte] = getParsedResponse.getBytes()

  def empty: Array[Byte] = {
    java.util.Base64.getDecoder.decode(b64EmptyRDB.getBytes())
  }
}

case class BulkResponse(response: String) extends ResponseTrait {
  def getParsedResponse: String = {
    response match {
      case "" => "$-1\r\n"
      case _ => s"$$${response.length}\r\n$response\r\n"
    }
  }
}

case class ArrayResponse(response: Array[String]) extends ResponseTrait {
  override def getParsedResponse: String = {
    response match {
      case Array() => "*-1\r\n"
      case _ => s"*${response.length}\r\n${response.map(parseText).mkString}"
    }
  }

  def parseText(text: String): String = {
    BulkResponse(text).getParsedResponse
  }
}

case class SimpleTextResponse(response: String) extends ResponseTrait {
  def getParsedResponse: String = {
    s"+$response\r\n"
  }
}

// TODO: these are just for empty RDBs
case class RDBSizeResponse() extends ResponseTrait {

  def getParsedResponse: String = {
    s"$$${empty.length.toString}\r\n"
  }
}

case class RDBFileResponse() extends ResponseTrait {
  def getParsedResponse: String = {
    s"$${empty.length}\r\n"
  }
  override def getBytes: Array[Byte] = {
    empty
  }
}

package parsers

class ParsedCommand(val command: String, val args: Array[String]) {
  def stringify(): String = {
    val argsString = args.mkString(" ")
    s"$command $argsString"
  }
}

class CommandParser {
  def parse(message: String): ParsedCommand = {
    val messages = message.split("\r\n")
    val inputCount = messages(0).drop(1).toInt
    if (messages.length != 2 * inputCount + 1) {
      throw new Exception("Invalid number of arguments")
    }
    for (i <- 1 to inputCount) {
      if (messages(2 * i - 1).drop(1).toInt != messages(2 * i).length) {
        throw new Exception("Invalid argument length")
      }
    }
    val command = messages(2)
    val args = new Array[String](inputCount - 1)
    for (i <- 1 until inputCount) {
      args(i - 1) = messages(2 + 2 * i)
    }
    val parsedCommand = new ParsedCommand(command, args)
    return parsedCommand
  }
}

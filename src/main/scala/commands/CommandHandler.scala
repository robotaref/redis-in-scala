package commands

import parsers.ParsedCommand

trait CommandHandler {
  def handle(): String
}

class PINGHandler(parsedCommand: ParsedCommand) extends CommandHandler {
  override def handle(): String = {
    "+PONG\r\n"
  }
}

class ECHOHandler(parsedCommand: ParsedCommand) extends CommandHandler {
  override def handle(): String = {
    s"+${parsedCommand.args(0)}\r\n"
  }
}

class CommandHandlerFactory {
  def getHandler(parsedCommand: ParsedCommand): CommandHandler = {
    parsedCommand.command match {
      case "PING" => new PINGHandler(parsedCommand)
      case "ECHO" => new ECHOHandler(parsedCommand)
      case _ => throw new Exception("Unknown command")
    }
  }
}
package commands

import config.Config
import parsers.ParsedCommand

import java.util.logging.Logger

trait CommandHandler {
  def handle(): String
}

class PINGHandler(DBClient: db.DBClient, parsedCommand: ParsedCommand) extends CommandHandler {
  override def handle(): String = {
    "+PONG\r\n"
  }
}

class ECHOHandler(DBClient: db.DBClient, parsedCommand: ParsedCommand) extends CommandHandler {
  override def handle(): String = {
    s"+${parsedCommand.args(0)}\r\n"
  }
}

class SETHandler(DBClient: db.DBClient, parsedCommand: ParsedCommand) extends CommandHandler {
  override def handle(): String = {
    var TTL: Long = -1
    for (i <- 2 until parsedCommand.args.length) {
      if (parsedCommand.args(i) == "px") {
        TTL = parsedCommand.args(i + 1).toLong
      }
    }
    DBClient.set(parsedCommand.args(0), parsedCommand.args(1), TTL)
    s"+OK\r\n"
  }
}

class GETHandler(DBClient: db.DBClient, parsedCommand: ParsedCommand) extends CommandHandler {
  override def handle(): String = {
    val value = DBClient.get(parsedCommand.args(0))
    value match {
      case "" => "$-1\r\n"
      case _ => s"$$${value.length}\r\n$value\r\n"
    }
  }
}

class CONFIGHandler(DBClient: db.DBClient, parsedCommand: ParsedCommand) extends CommandHandler {
  override def handle(): String = {
    val config = Config
    parsedCommand.args(0) match {
      case "GET" =>
        if (parsedCommand.args.length != 2) {
          return "-ERR wrong number of arguments for 'CONFIG' command\r\n"
        }
        val searchedKey = parsedCommand.args(1)
        val answer = config.get(searchedKey)
        answer match {
          case "" => "$-1\r\n"
          case _ => s"*2\r\n$$${searchedKey.length}\r\n$searchedKey\r\n$$${answer.length}\r\n$answer\r\n"
        }
      case _ => "$-1\r\n"
    }
  }
}

class CommandHandlerFactory {
  def getHandler(DBClient: db.DBClient, parsedCommand: ParsedCommand): CommandHandler = {
    parsedCommand.command match {
      case "PING" => new PINGHandler(DBClient, parsedCommand)
      case "ECHO" => new ECHOHandler(DBClient, parsedCommand)
      case "SET" => new SETHandler(DBClient, parsedCommand)
      case "GET" => new GETHandler(DBClient, parsedCommand)
      case "CONFIG" => new CONFIGHandler(DBClient, parsedCommand)
      case _ => throw new Exception("Unknown command")
    }
  }
}
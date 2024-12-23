package commands

import config.Config
import db.DBAdaptor
import parsers.ParsedCommand

import java.nio.file.Path

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

class SETHandler(parsedCommand: ParsedCommand) extends CommandHandler {
  override def handle(): String = {
    var TTL: Long = -1
    for (i <- 2 until parsedCommand.args.length) {
      if (parsedCommand.args(i) == "px") {
        TTL = parsedCommand.args(i + 1).toLong + System.currentTimeMillis()
      }
    }
    DBAdaptor.set(parsedCommand.args(0), parsedCommand.args(1), TTL)
    s"+OK\r\n"
  }
}

class GETHandler(parsedCommand: ParsedCommand) extends CommandHandler {
  override def handle(): String = {
    val value = DBAdaptor.get(parsedCommand.args(0))
    value match {
      case "" => "$-1\r\n"
      case _ => s"$$${value.length}\r\n$value\r\n"
    }
  }
}

class CONFIGHandler(parsedCommand: ParsedCommand) extends CommandHandler {
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

class KEYSHandler(parsedCommand: ParsedCommand) extends CommandHandler {
  override def handle(): String = {
    val keys = DBAdaptor.keys()
    if (keys.isEmpty) {
      return "*0\r\n"
    }
    println(keys)
    val response = keys.map(key => s"$$${key.length}\r\n$key\r\n").mkString
    s"*${keys.length}\r\n$response"
  }
}

class SAVEHandler(parsedCommand: ParsedCommand) extends CommandHandler {
  override def handle(): String = {
    DBAdaptor.save(Path.of(Config.dir, Config.DBFileName))
    "+OK\r\n"
  }
}

class LOADHandler(parsedCommand: ParsedCommand) extends CommandHandler {
  override def handle(): String = {
    DBAdaptor.load(Path.of(Config.dir, Config.DBFileName))
    "+OK\r\n"
  }
}

class CommandHandlerFactory {
  private val commandHandlers: Map[String, ParsedCommand => CommandHandler] = Map(
    "PING" -> (new PINGHandler(_)),
    "ECHO" -> (new ECHOHandler(_)),
    "SET" -> (new SETHandler(_)),
    "GET" -> (new GETHandler(_)),
    "CONFIG" -> (new CONFIGHandler(_)),
    "KEYS" -> (new KEYSHandler(_)),
    "SAVE" -> (new SAVEHandler(_)),
    "LOAD" -> (new LOADHandler(_)),
  )

  def getHandler(parsedCommand: ParsedCommand): CommandHandler = {
    commandHandlers.get(parsedCommand.command) match {
      case Some(handlerFactory) => handlerFactory(parsedCommand)
      case None => throw new Exception("Unknown command")
    }
  }
}
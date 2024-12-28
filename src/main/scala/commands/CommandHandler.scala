package commands

import config.Config
import db.DBAdaptor
import parsers.{BulkStringParser, ParsedCommand}

import java.nio.file.Path

trait CommandHandler {
  def handle(): Any
}

class PINGHandler(parsedCommand: ParsedCommand) extends CommandHandler {
  override def handle(): String = {
    "PONG"
  }
}

class ECHOHandler(parsedCommand: ParsedCommand) extends CommandHandler {
  override def handle(): String = {
    s"${parsedCommand.args(0)}"
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
    "OK"
  }
}

class GETHandler(parsedCommand: ParsedCommand) extends CommandHandler {
  override def handle(): String = {
    val value = DBAdaptor.get(parsedCommand.args(0))
    value match {
      case "" => ""
      case _ => value
    }
  }
}

class CONFIGHandler(parsedCommand: ParsedCommand) extends CommandHandler {
  override def handle(): Any = {
    val config = Config
    parsedCommand.args(0) match {
      case "GET" =>
        if (parsedCommand.args.length != 2) {
          return "-ERR wrong number of arguments for 'CONFIG' command"
        }
        val searchedKey = parsedCommand.args(1)
        val answer = config.get(searchedKey)
        answer match {
          case "" => Array()
          case _ => Array(searchedKey, answer)
        }
      case _ => Array()
    }
  }
}

class KEYSHandler(parsedCommand: ParsedCommand) extends CommandHandler {
  override def handle(): Array[String] = {
    val keys = DBAdaptor.keys()
    keys.toArray
  }
}

class SAVEHandler(parsedCommand: ParsedCommand) extends CommandHandler {
  override def handle(): String = {
    DBAdaptor.save(Path.of(Config.dir, Config.DBFileName))
    "OK"
  }
}

class LOADHandler(parsedCommand: ParsedCommand) extends CommandHandler {
  override def handle(): String = {
    DBAdaptor.load(Path.of(Config.dir, Config.DBFileName))
    "OK"
  }
}

class INFOHandler(parsedCommand: ParsedCommand) extends CommandHandler {
  private def replication = {
    s"role:${config.Config.role}" +
      s"master_replid:${config.Config.replicationID}" +
      s"master_repl_offset:${config.Config.replicationOffset}"
  }

  override def handle(): String = {
    parsedCommand.args(0) match {
      case "replication" => replication
      case _ => "-ERR unknown subcommand or wrong number of arguments for 'INFO' command\r\n"
    }
  }
}

class REPLCONFHandler(parsedCommand: ParsedCommand) extends CommandHandler {
  override def handle(): String = {
    println("in repl handler", parsedCommand.args(0))
    parsedCommand.args(0) match {
      case "listening-port" =>
        //        config.port = parsedCommand.args(1).toInt
        "OK"
      case "capa" =>
        if (parsedCommand.args(1) == "psync2") {
          "OK"
        } else {
          "-ERR unsupported capability"
        }
      case _ => "-ERR unsupported subcommand"
    }
  }
}

class PSYNCHandler(parsedCommand: ParsedCommand) extends CommandHandler {
  override def handle(): String = { // return value should be simpleString
    s"FULLRESYNC ${Config.replicationID} 0"
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
    "INFO" -> (new INFOHandler(_)),
    "REPLCONF" -> (new REPLCONFHandler(_)),
    "PSYNC" -> (new PSYNCHandler(_)),
  )

  def getHandler(parsedCommand: ParsedCommand): CommandHandler = {
    commandHandlers.get(parsedCommand.command) match {
      case Some(handlerFactory) => handlerFactory(parsedCommand)
      case None => throw new Exception("Unknown command")
    }
  }
}
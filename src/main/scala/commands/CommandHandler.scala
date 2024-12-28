package commands

import config.Config
import db.DBAdaptor
import parsers.{ArrayResponse, BulkResponse, ParsedCommand, RDBFileResponse, RDBSizeResponse, ResponseTrait, SimpleTextResponse}

import java.nio.file.Path

trait CommandHandler {
  def handle(): Array[ResponseTrait]
}

class PINGHandler(parsedCommand: ParsedCommand) extends CommandHandler {
  override def handle(): Array[ResponseTrait] = {
    Array(BulkResponse("PONG"))
  }
}

class ECHOHandler(parsedCommand: ParsedCommand) extends CommandHandler {
  override def handle(): Array[ResponseTrait] = {
    Array(SimpleTextResponse(s"${parsedCommand.args(0)}"))
  }
}

class SETHandler(parsedCommand: ParsedCommand) extends CommandHandler {
  override def handle(): Array[ResponseTrait] = {
    var TTL: Long = -1
    for (i <- 2 until parsedCommand.args.length) {
      if (parsedCommand.args(i) == "px") {
        TTL = parsedCommand.args(i + 1).toLong + System.currentTimeMillis()
      }
    }
    DBAdaptor.set(parsedCommand.args(0), parsedCommand.args(1), TTL)
    Array(SimpleTextResponse("OK"))
  }
}

class GETHandler(parsedCommand: ParsedCommand) extends CommandHandler {
  override def handle(): Array[ResponseTrait] = {
    val value = DBAdaptor.get(parsedCommand.args(0))
    value match {
      case "" => Array(BulkResponse(""))
      case _ => Array(BulkResponse(value))
    }
  }
}

class CONFIGHandler(parsedCommand: ParsedCommand) extends CommandHandler {
  override def handle(): Array[ResponseTrait] = {
    val config = Config
    parsedCommand.args(0) match {
      case "GET" =>
        if (parsedCommand.args.length != 2) {
          return Array(SimpleTextResponse("-ERR wrong number of arguments for 'CONFIG' command"))
        }
        val searchedKey = parsedCommand.args(1)
        val answer = config.get(searchedKey)
        answer match {
          case "" => Array(ArrayResponse(Array()))
          case _ => Array(ArrayResponse(Array(searchedKey, answer)))
        }
      case _ => Array(ArrayResponse(Array()))
    }
  }
}

class KEYSHandler(parsedCommand: ParsedCommand) extends CommandHandler {
  override def handle(): Array[ResponseTrait] = {
    val keys = DBAdaptor.keys()
    Array(ArrayResponse(keys.toArray))
  }
}

class SAVEHandler(parsedCommand: ParsedCommand) extends CommandHandler {
  override def handle(): Array[ResponseTrait] = {
    DBAdaptor.save(Path.of(Config.dir, Config.DBFileName))
    Array(SimpleTextResponse("OK"))
  }
}

class LOADHandler(parsedCommand: ParsedCommand) extends CommandHandler {
  override def handle(): Array[ResponseTrait] = {
    DBAdaptor.load(Path.of(Config.dir, Config.DBFileName))
    Array(SimpleTextResponse("OK"))
  }
}

class INFOHandler(parsedCommand: ParsedCommand) extends CommandHandler {
  private def replication = {
    s"role:${config.Config.role}" +
      s"master_replid:${config.Config.replicationID}" +
      s"master_repl_offset:${config.Config.replicationOffset}"
  }

  override def handle(): Array[ResponseTrait] = {
    parsedCommand.args(0) match {
      case "replication" => Array(BulkResponse(replication))
      case _ => Array(BulkResponse("-ERR unknown subcommand or wrong number of arguments for 'INFO' command\r\n"))
    }
  }
}

class REPLCONFHandler(parsedCommand: ParsedCommand) extends CommandHandler {
  override def handle(): Array[ResponseTrait] = {
    println("in repl handler", parsedCommand.args(0))
    parsedCommand.args(0) match {
      case "listening-port" =>
        config.Config.replicaConfig = config.Config.replicaConfig :+ new config.SlaveConfig {
          slaveHost = config.Config.host
          slavePort = parsedCommand.args(1).toInt
        }
        Array(SimpleTextResponse("OK"))
      case "capa" =>
        if (parsedCommand.args(1) == "psync2") {
          Array(SimpleTextResponse("OK"))
        } else {
          Array(SimpleTextResponse("-ERR unsupported capability"))
        }
      case _ => Array(SimpleTextResponse("-ERR unsupported subcommand"))
    }
  }
}

class PSYNCHandler(parsedCommand: ParsedCommand) extends CommandHandler {
  override def handle(): Array[ResponseTrait] = {
    Array(
      SimpleTextResponse(s"FULLRESYNC ${Config.replicationID} 0"),
      RDBSizeResponse(),
      RDBFileResponse(),
    )
  }
}

class CommandHandlerFactory {
  private val commandHandlers: Map[String, ParsedCommand => CommandHandler] = Map(
    "SET" -> (new SETHandler(_)),
    "GET" -> (new GETHandler(_)),
    "PING" -> (new PINGHandler(_)),
    "ECHO" -> (new ECHOHandler(_)),
    "KEYS" -> (new KEYSHandler(_)),
    "SAVE" -> (new SAVEHandler(_)),
    "LOAD" -> (new LOADHandler(_)),
    "INFO" -> (new INFOHandler(_)),
    "PSYNC" -> (new PSYNCHandler(_)),
    "CONFIG" -> (new CONFIGHandler(_)),
    "REPLCONF" -> (new REPLCONFHandler(_)),
  )

  def getHandler(parsedCommand: ParsedCommand): CommandHandler = {
    commandHandlers.get(parsedCommand.command) match {
      case Some(handlerFactory) => handlerFactory(parsedCommand)
      case None => throw new Exception("Unknown command")
    }
  }
}
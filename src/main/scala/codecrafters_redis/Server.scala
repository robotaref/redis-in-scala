package codecrafters_redis

import config.Config
import db.DBAdaptor
import parsers.BulkStringParser

import java.io.{BufferedReader, InputStreamReader, OutputStreamWriter, PrintWriter}
import java.net._
import java.nio.file.Path

object Server {
  def main(args: Array[String]): Unit = {
    val config = Config
    config.loadConfig(args)
    if (config.dir != "" && config.DBFileName != "") {
      DBAdaptor.load(Path.of(config.dir, config.DBFileName))
    }
    if (config.role == "slave") {
      initiateReplication()
    }

    val serverSocket = new ServerSocket()
    serverSocket.bind(new InetSocketAddress(config.host, config.port))

    while (true) {
      val clientSocket = serverSocket.accept()
      new Thread(new ClientHandler(clientSocket)).start()
    }
  }

  private def initiateReplication(): Unit = {
    val host = Config.masterHost.get
    val port = Config.masterPort.get
    val clientSocket = new Socket(host, port)
    val out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream), true)
    val in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream))
    try {
      ensurePing(in, out)
      ensureReplConf(in, out)
      ensurePsync(in, out)
    } catch {
      case e: Exception =>
        e.printStackTrace()
    } finally {
      println("Replication started")
      clientSocket.close()
    }
  }

  private def ensurePing(in: BufferedReader, out: PrintWriter): Unit = {
    val response: String = callMaster(in, out, Array("PING"))
  }

  private def callMaster(in: BufferedReader, out: PrintWriter, messages: Array[String]): String = {

    try {
      val pingMessage = BulkStringParser.parse(messages)
      out.print(pingMessage)
      out.flush()
      val buffer = new Array[Char](10240)
      val bytesRead = in.read(buffer)
      val message = new String(buffer, 0, bytesRead)
    } catch {
      case e: Exception =>
        e.printStackTrace()
    }
    ""
  }

  private def ensureReplConf(in: BufferedReader, out: PrintWriter): Unit = {
    callMaster(in, out, Array("REPLCONF", "listening-port", Config.port.toString))
    callMaster(in, out, Array("REPLCONF", "capa", "psync2"))
  }

  private def ensurePsync(in: BufferedReader, out: PrintWriter): Unit = {
    callMaster(in, out, Array("PSYNC", "?", "-1"))
  }
}


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

    try {
      val clientSocket = new Socket(host, port)
      val out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream), true)
      val in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream))

      val pingMessage = BulkStringParser.parse(Array("PING"))
      out.print(pingMessage)
      out.flush()
      val response = in.readLine()
      if (response != BulkStringParser.parse("PONG")) {
        throw new Exception("Master is not responding")
      }
      clientSocket.close()
    } catch {
      case e: Exception =>
        e.printStackTrace()
    }
  }
}

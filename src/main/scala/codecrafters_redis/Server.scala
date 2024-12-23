package codecrafters_redis

import config.Config
import db.DBAdaptor

import java.net._
import java.nio.file.Path

object Server {
  def main(args: Array[String]): Unit = {
    val config = Config
    config.loadConfig(args)
    if (config.dir != "" && config.DBFileName != "") {
      DBAdaptor.load(Path.of(config.dir, config.DBFileName))
    }

    val serverSocket = new ServerSocket()
    serverSocket.bind(new InetSocketAddress(config.host, config.port))

    while (true) {
      val clientSocket = serverSocket.accept()
      new Thread(new ClientHandler(clientSocket)).start()
    }
  }
}

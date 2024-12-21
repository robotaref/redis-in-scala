package codecrafters_redis

import db.InMemoryDBClient
import config.Config
import java.net._

object Server {
  def main(args: Array[String]): Unit = {
    val config = Config
    config.loadConfig(args)

    val serverSocket = new ServerSocket()
    serverSocket.bind(new InetSocketAddress("localhost", 6379))
    val DBClient = new InMemoryDBClient()
    while (true) {
      val clientSocket = serverSocket.accept()
      new Thread(new ClientHandler(DBClient, clientSocket)).start()
    }
  }
}

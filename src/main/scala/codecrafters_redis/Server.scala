package codecrafters_redis

import db.MemoryDBClient

import java.net._

object Server {
  def main(args: Array[String]): Unit = {
    val serverSocket = new ServerSocket()
    serverSocket.bind(new InetSocketAddress("localhost", 6379))
    val DBClient = new MemoryDBClient()
    while (true) {
      val clientSocket = serverSocket.accept()
      new Thread(new ClientHandler(DBClient, clientSocket)).start()
    }
  }
}

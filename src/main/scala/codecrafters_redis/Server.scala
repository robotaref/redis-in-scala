package codecrafters_redis

import java.net._

object Server {
  def main(args: Array[String]): Unit = {
    val serverSocket = new ServerSocket()
    serverSocket.bind(new InetSocketAddress("localhost", 6379))
    while (true) {
      val clientSocket = serverSocket.accept()
      new Thread(new ClientHandler(clientSocket)).start()
    }
  }
}

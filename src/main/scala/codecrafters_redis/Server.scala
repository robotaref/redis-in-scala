package codecrafters_redis

import java.io._
import java.net._

object Server {
  def main(args: Array[String]): Unit = {
    val serverSocket = new ServerSocket()
    serverSocket.bind(new InetSocketAddress("localhost", 6379))
    while (true) {
      val clientSocket = serverSocket.accept()
      new Thread(() => handleClient(clientSocket)).start()
    }
  }

  def handleClient(clientSocket: Socket): Unit = {
    val in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream))
    val out = new PrintWriter(clientSocket.getOutputStream, true)

    try {
      var running = true
      while (running) {
        val message = in.readLine()
        if (message == null) {
          running = false
        } else {
          println(s"Received: $message")
          if (message == "PING") {
            out.println("+PONG\r") // Respond to ping
          }
        }
      }
    } catch {
      case e: IOException => println(s"Connection error: ${e.getMessage}")
    } finally {
      println(s"Closing connection with ${clientSocket.getInetAddress}")
      clientSocket.close()
    }
  }
}


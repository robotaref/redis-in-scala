package codecrafters_redis

import parsers.CommandParser
import commands.CommandHandlerFactory
import java.io.{BufferedReader, IOException, InputStreamReader}
import java.net.Socket


class ClientHandler(clientSocket: Socket) extends Runnable {
  override def run(): Unit = {
    val in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream))
    val buffer = new Array[Char](1024)
    try {
      var running = true
      while (running) {
        val bytesRead = in.read(buffer)
        if (bytesRead == -1) {
          running = false
        } else {
          val message = new String(buffer, 0, bytesRead)
          val parser = new CommandParser()
          val parsedCommand = parser.parse(message)
          val handler = new CommandHandlerFactory().getHandler(parsedCommand)
          val response = handler.handle()
          clientSocket.getOutputStream.write(response.getBytes())
        }
      }
    }
    catch {
      case e: IOException => println(s"Connection error: ${e.getMessage}")
    } finally {
      println(s"Closing connection with ${clientSocket.getInetAddress}")
      clientSocket.close()
    }
  }
}
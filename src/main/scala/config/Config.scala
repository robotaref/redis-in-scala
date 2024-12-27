package config

object Config {
  var dir: String = ""
  var DBFileName: String = ""
  var host: String = "localhost"
  var port: Int = 6379

  def get(key: String): String = {
    key match {
      case "dir" => dir
      case "DBFileName" => DBFileName
      case "host" => host
      case "port" => port.toString
      case _ => ""
    }
  }

  def loadConfig(args: Array[String]): Unit = {
    for (i <- args.indices) {
      args(i) match {
        case "--dir" => dir = args(i + 1)
        case "--dbfilename" => DBFileName = args(i + 1)
        case "--host" => host = args(i + 1)
        case "--port" => port = args(i + 1).toInt
        case _ =>
      }
    }
  }
}


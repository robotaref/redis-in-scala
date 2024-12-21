package config

object Config {
  var dir: String = ""
  var DBFileName: String = ""

  def get(key: String): String = {
    key match {
      case "dir" => dir
      case "DBFileName" => DBFileName
      case _ => ""
    }
  }

  def loadConfig(args: Array[String]): Unit = {
    for (i <- args.indices) {
      if (args(i) == "--dir") {
        Config.dir = args(i + 1)
      } else if (args(i) == "--dbfilename") {
        Config.DBFileName = args(i + 1)
      }
    }
  }
}


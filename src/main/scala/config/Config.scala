package config

class MasterConfig {
  var masterHost: String = ""
  var masterPort: Int = 0
  var replicationID: String = ""
  var replicationOffset: Long = 0
}

class SlaveConfig {
  var slaveHost: String = ""
  var slavePort: Int = 0
}

object Config {
  var dir: String = ""
  var DBFileName: String = ""
  var host: String = "localhost"
  var port: Int = 6379
  var role: String = "master"
  var masterConfig: Option[MasterConfig] = None
  var replicaConfig: Array[SlaveConfig] = Array()
  var replicationID: String = "8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb"
  var replicationOffset: Long = 0

  def get(key: String): String = {
    key match {
      case "dir" => dir
      case "DBFileName" => DBFileName
      case "host" => host
      case "port" => port.toString
      case "role" => role
      case "replication-id" => replicationID
      case "replication-offset" => replicationOffset.toString
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
        case "--replicaof" =>
          role = "slave"
          val relatedMaster = args(i + 1).split(" ")
          masterConfig = Some(new MasterConfig {
            masterHost = relatedMaster(0)
            masterPort = relatedMaster(1).toInt
          })
        case _ =>
      }
    }
  }
}


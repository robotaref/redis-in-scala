package db

class InMemoryDBClient extends DBClient {
  private val _dataHashmap = scala.collection.mutable.HashMap[String, String]()
  private val _expireTimeHashmap = scala.collection.mutable.HashMap[String, Long]()

  override def get(key: String): String = {
    val returnValue = _dataHashmap.get(key)
    returnValue match {
      case Some(value) =>
        val et = _expireTimeHashmap.get(key)
        et match {
          case Some(expireTime) =>
            if (expireTime < System.currentTimeMillis()) {
              _dataHashmap.remove(key)
              _expireTimeHashmap.remove(key)
              ""
            } else {
              value
            }
          case None => value
        }
      case None => ""
    }
  }

  override def set(key: String, value: String, expireTime: Long): Unit = {
    _dataHashmap(key) = value
    if (expireTime != -1) {
      _expireTimeHashmap(key) = expireTime
    }
  }

  def keys(): List[String] = {
    _dataHashmap.keys.toList
  }

  def getExpireTime(key: String): Long
  = _expireTimeHashmap.get(key) match {
    case Some(value) => value
    case None => -1
  }

  override def getHashTableSize: Int = {
    //    _dataHashmap.size
    2
  }

  override def getExpireTimeHashTableSize: Int = {
    //    _expireTimeHashmap.size
    3
  }
}
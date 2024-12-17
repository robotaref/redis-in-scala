package db

class DBValue(var value: String, var et: Long) {
}

trait DBClient {
  def get(key: String): String

  def set(key: String, value: String, expire_time: Long): Unit
}


class MemoryDBClient extends DBClient {
  private val _db = scala.collection.mutable.HashMap[String, DBValue]()

  override def get(key: String): String = {
    val returnValue = _db.get(key)
    returnValue match {
      case Some(value) =>
        if (value.et != -1 && value.et < System.currentTimeMillis()) {
          _db.remove(key)
          ""
        } else {
          value.value
        }
      case None => ""
    }
  }

  override def set(key: String, value: String, expireTime: Long): Unit = {
    val usedExpireTime = if (expireTime == -1) -1 else System.currentTimeMillis() + expireTime
    _db(key) = new DBValue(value, usedExpireTime)
  }
}
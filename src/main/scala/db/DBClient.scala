package db

class DBValue(var value: String, var ttl: Long) {
}

trait DBClient {
  def get(key: String): String

  def set(key: String, value: String): Unit
}


class MemoryDBClient extends DBClient {
  private val _db = scala.collection.mutable.HashMap[String, DBValue]()

  override def get(key: String): String = {
    _db.get(key) match {
      case Some(value) => value.value
      case None => ""
    }
  }

  override def set(key: String, value: String): Unit = {
    _db(key) = new DBValue(value, -1)
  }
}
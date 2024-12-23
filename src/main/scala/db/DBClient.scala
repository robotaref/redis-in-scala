package db


trait DBClient {
  def get(key: String): String

  def getExpireTime(key: String): Long

  def set(key: String, value: String, expire_time: Long): Unit

  def keys(): List[String]

  def getHashTableSize: Int

  def getExpireTimeHashTableSize: Int
}



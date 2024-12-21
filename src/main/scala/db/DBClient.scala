package db

class DBValue(var value: String, var et: Long) {
}

trait DBClient {
  def get(key: String): String
  def set(key: String, value: String, expire_time: Long): Unit
}



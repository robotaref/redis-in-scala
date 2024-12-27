package db

import java.io.{BufferedReader, FileOutputStream}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}
import java.nio.{ByteBuffer, ByteOrder}

sealed trait OpCode

object OpCode {
  final case object EOF extends OpCode

  final case object SELECTDB extends OpCode

  final case object EXPIRETIME extends OpCode

  final case object EXPIRETIMEMS extends OpCode

  final case object RESIZEDB extends OpCode

  final case object AUX extends OpCode

  final case object UNKNOWN extends OpCode

  def from_int(i: Int): OpCode = i match {
    case 0xff => EOF
    case 0xfe => SELECTDB
    case 0xfd => EXPIRETIME
    case 0xfc => EXPIRETIMEMS
    case 0xfb => RESIZEDB
    case 0xfa => AUX
    case _ => UNKNOWN
  }
}


object DBAdaptor {

  import OpCode._

  private val _dbClient: DBClient = new InMemoryDBClient()

  private def writeStringLine(fileOutputStream: FileOutputStream, string: String): Unit = {
    fileOutputStream.write(s"$string\n".getBytes(StandardCharsets.US_ASCII))
  }

  private def writeLongLine(fileOutputStream: FileOutputStream, long: Long): Unit = {
    val bytes = java.nio.ByteBuffer
      .allocate(8)
      .order(java.nio.ByteOrder.LITTLE_ENDIAN)
      .putLong(long)
      .array()

    fileOutputStream.write(bytes)
    writeStringLine(fileOutputStream, "")
  }

  private def readLongLine(fileReader: BufferedReader): Long = {
    val charBuffer = new Array[Char](8)
    fileReader.read(charBuffer, 0, 8)
    val byteBuffer = new Array[Byte](8)
    charBuffer.zipWithIndex.foreach { case (char, index) =>
      byteBuffer(index) = char.toByte
    }
    val expireTime = ByteBuffer.wrap(byteBuffer)
      .order(ByteOrder.LITTLE_ENDIAN)
      .getLong
    println(s"expireTime: $expireTime")
    fileReader.readLine()
    expireTime
  }

  private def writeSizeLine(fileOutputStream: FileOutputStream, size: Int): Unit = {
    fileOutputStream.write(0)
    writeStringLine(fileOutputStream, "")
  }

  def save(address: Path): Unit = {

    val fileOutputStream = new FileOutputStream(address.toString)
    try {
      // save header
      writeStringLine(fileOutputStream, "REDIS0011")
      //  save meta data
      writeStringLine(fileOutputStream, "FA")
      writeStringLine(fileOutputStream, "redis-ver")
      writeStringLine(fileOutputStream, "1.0.0")
      // data base section
      writeStringLine(fileOutputStream, "FE")
      writeStringLine(fileOutputStream, "0") // TODO
      writeStringLine(fileOutputStream, "FB")
      writeSizeLine(fileOutputStream, _dbClient.getHashTableSize)
      writeSizeLine(fileOutputStream, _dbClient.getExpireTimeHashTableSize)
      keys().foreach(key => {
        val expireTime = _dbClient.getExpireTime(key)
        println(s"key: $key, expireTime: $expireTime")
        if (expireTime != -1) {
          writeStringLine(fileOutputStream, "FC")
          writeLongLine(fileOutputStream, expireTime)
          println(s"expireTime: $expireTime")
        }
        writeStringLine(fileOutputStream, "0")
        writeStringLine(fileOutputStream, key)
        writeStringLine(fileOutputStream, get(key))
      })
      // save end of file marker
      writeStringLine(fileOutputStream, "FF")
      fileOutputStream.write(0) // TODO: An 8-byte CRC64 checksum of the entire file.
    }
    finally {
      fileOutputStream.close()
    }
  }

  private def read_header(buf: ByteBuffer) = {
    var tmp = Array.fill(5) {
      buf.get()
    }
    new String(tmp, "ASCII")
    tmp = Array.fill(4) {
      buf.get()
    }
    new String(tmp, "ASCII")
  }

  private def read_metadata(buf: ByteBuffer): List[(String, String)] = {
    if (get_opcode(buf) != AUX) {
      List.empty
    } else {
      buf.get // Step past AUX
      (read_string(buf) -> read_string(buf)) :: read_metadata(buf)
    }
  }

  def get_opcode(buf: ByteBuffer) = {
    OpCode.from_int(buf.get(buf.position) & 0xff)
  }

  def load(address: Path): Unit = {
    if (!Files.exists(address)) {
      Files.createFile(address)
      return
    }
    val buffer = ByteBuffer
      .wrap(Files.readAllBytes(Paths.get(address.toString)))
      .order(java.nio.ByteOrder.LITTLE_ENDIAN)
    read_header(buffer)
    read_metadata(buffer)
    // metadata foreach println
    read_databases(buffer)
    // dbs foreach println
    // TODO: Verify the correct number of pairs and exprirys have been read
    // TODO: Verify EOF and checksum
    // Load the data into the store.
  }

  def read_databases(buf: ByteBuffer) {
    if (get_opcode(buf) != SELECTDB) {
      List.empty
    } else {
      buf.get
      val index = decode_length(buf) // TODO: Use this to check
      buf.get
      val data_table_size = decode_length(buf)
      val expiry_table_size = decode_length(buf)
      read_pairs(buf, data_table_size)
    }
  }

  def read_pairs(buf: ByteBuffer, n_pairs: Int) = {
    List.fill(n_pairs) {
      val exp = read_expiry(buf)
      val (k, v) = read_pair(buf)
      set(k, v, exp)
    }
  }

  def read_expiry(buf: ByteBuffer): Long = {
    get_opcode(buf) match {
      case EXPIRETIMEMS => {
        buf.get
        buf.getLong
      }
      case EXPIRETIME => {
        buf.get
        buf.getInt * 1000
      }
      case _ => -1L
    }
  }

  def read_pair(buf: ByteBuffer) = {
    // The value of the next byte determines the type of the value.
    val value_type = buf.get
    val key = read_string(buf)
    val value = value_type match {
      case 0 => read_string(buf)
    }
    (key, value)
  }

  private def decode_length(buf: ByteBuffer) = {
    val b = buf.get()
    // First two bits determine enc type
    (b >>> 6) & 0x3 match {
      case 0 => b & 63
      // TODO case 1 => {
      // TODO: case 2 =>
    }
  }

  def get(key: String): String = {
    _dbClient.get(key)
  }

  def set(key: String, value: String, expireTime: Long): Unit = {
    _dbClient.set(key, value, expireTime)
  }

  def keys(): List[String] = {
    _dbClient.keys()
  }

  private def read_string(buf: ByteBuffer) = {
    val b = buf.get()
    // First two bits determine enc type
    (b >>> 6) & 0x3 match {
      case 0 => {
        val len = b & 63
        new String(Array.fill(len) {
          buf.get
        }, "ASCII")
      }
      // case 1 => {
      //   val b2 = buf.get()
      //   // Clear the first two bits and combine with the second byte.
      //   val len = ((b >>> 2) << 8) + b2
      // }
      // TODO: case 2 =>
      case 3 => {
        // Move the pointer one byte forwards.
        // Compare remaining 6 bits (of the original flag) to read an integer as string.
        b & 63 match {
          case 0 => (buf.get & 0xff).toString
          // case 1 => {
          //   val b1 = buf.get & 0xff
          //   val b2 = buf.get & 0xff
          //   b1 << 8 + b2
          // }
          case 2 => buf.getInt().toString
          // case 3 =>
        }
      }
    }
  }
}

package io.github.tomykaira.withlog

import akka.util.ByteString

trait ByteStringHelper {
  protected def readInt(data: ByteString): Int =
    (0 to 3).foldLeft(0) { (sum, idx) => ((data(idx) & 0xff) << (idx*8)) + sum }

  protected def readLong(data: ByteString): Long =
    (0 to 7).foldLeft(0L) { (sum, idx) => ((data(idx) & 0xff).toLong << (idx*8)) + sum }

  protected def readSequence(data: ByteString): (ByteString, ByteString) = {
    val length = readInt(data)
    (data.slice(4, 4+length), data.drop(4+length))
  }

  protected def readString(data: ByteString): (String, ByteString) = {
    val length = readInt(data)
    (data.slice(4, 4+length).utf8String, data.drop(4+length))
  }
}

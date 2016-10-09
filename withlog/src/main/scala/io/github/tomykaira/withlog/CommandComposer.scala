package io.github.tomykaira.withlog

import akka.util.{ByteStringBuilder, ByteString}
import java.nio.ByteOrder

class CommandComposer {
  private[this] implicit val byteOrder = ByteOrder.LITTLE_ENDIAN

  def read(key: Long, count: Int, tags: Iterable[ByteString]): ByteString = {
    val builder = new ByteStringBuilder
    builder.putByte(1)
      .putLong(key)
      .putInt(count)
      .putInt(tags.size)
    tags.foreach { tag =>
      builder.putInt(tag.length)
      builder ++= tag
    }
    builder.result()
  }
}

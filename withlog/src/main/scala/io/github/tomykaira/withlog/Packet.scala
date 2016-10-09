package io.github.tomykaira.withlog

import akka.util.ByteString
import java.text.SimpleDateFormat
import java.util.Date

case class Packet(key: Long, message: Message) {
  lazy val displayable: String = "[%s] %s" format (Packet.formatKeyAsDate(key), message.displayable)
}

object Packet {
  private[this] val dateFormat = new SimpleDateFormat("dd HH:mm:ss")

  def apply(key: Long, data: ByteString): Packet = Packet(key, Message(data))

  def formatKeyAsDate(key: Long): String =
    dateFormat.format(new Date(key / 1000))
}


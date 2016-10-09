package io.github.tomykaira.withlog

import akka.util.ByteString
import scala.annotation.tailrec

class ResponseDecoder extends ByteStringHelper {
  var count = 0
  var remaining: ByteString = ByteString()
  var lastPacket: Option[Packet] = None
  var readPackets: Vector[Packet] = Vector()

  def decode(received: ByteString) {
    val data = remaining ++ received

    @tailrec
    def consumeData(data: ByteString): ByteString = {
      readPacket(data) match {
        case Some((packet, rem)) =>
          readPackets = readPackets :+ packet
          count = count - 1
          if (count <= 0)
            rem
          else
            consumeData(rem)
        case None =>
          data
      }
    }

    val body = if (processing) {
      data
    } else {
      if (data.length <= 6)
        return
      assert(data(0) == 0)
      assert(data(1) == 1)
      count = readInt(data.drop(2))
      assert(count >= 0)
      data.drop(6)
    }

    remaining = consumeData(body)
    lastPacket = readPackets.lastOption.orElse(lastPacket)
  }

  def popPackets: Vector[Packet] = {
    val packets = readPackets
    readPackets = Vector()
    packets
  }

  def lastKey: Long = lastPacket map (_.key) getOrElse 0L

  def hasDone: Boolean = !processing

  def processing: Boolean = count > 0

  private def readPacket(data: ByteString): Option[(Packet, ByteString)] = {
    if (data.length <= 12)
      return None
    val key = readLong(data)
    val length = readInt(data.drop(8))
    if (data.length < 12 + length)
      None
    else
      try {
        Some((Packet(key, data.slice(12, 12 + length)), data.drop(12 + length)))
      } catch {
        case e: ArrayIndexOutOfBoundsException =>
          System.err.println("Decode error in " + key)
          System.err.println(data.slice(12, 12+length))
          e.printStackTrace()
          None
      }
  }
}

package io.github.tomykaira.withlog

import akka.actor.Actor
import akka.util.ByteString
import akka.io.Tcp.Connected

trait PacketSubscriber extends Actor {
  private[this] val composer: CommandComposer = new CommandComposer
  private[this] val decoder: ResponseDecoder = new ResponseDecoder

  protected val requestSize: Int
  protected val concernedTags: Iterable[ByteString]

  def receive = {
    case c: Connected =>
      println("Connection established")
      sender ! readRequest
    case data: ByteString =>
      decoder.decode(data)
      process(decoder.popPackets)
      if (decoder.hasDone) {
        sender ! readRequest
      }
    case "failed" =>
      println("Client failed")
  }

  private def readRequest: ByteString = {
    composer.read(decoder.lastKey, requestSize, concernedTags)
  }
  protected def process(packets: Seq[Packet]): Unit
}

package io.github.tomykaira.withlog

import scala.collection.mutable.ArrayBuffer
import io.github.tomykaira.constraintscala.StaticConstraint

class LogAccumulator(constraint: StaticConstraint[ArrayBuffer[Packet]]) extends PacketSubscriber {
  protected val requestSize = -1
  protected val concernedTags =
    Seq(Message.Banner.tag, Message.ChaosLog.tag, Message.ChaosStatus.tag, Message.Speech.tag)

  protected def process(received: Seq[Packet]) {
    constraint.update(constraint.get ++= received.filter { p => !p.message.isInstanceOf[Message.Unknown] })
  }
}

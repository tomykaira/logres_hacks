package io.github.tomykaira.withlog

import akka.actor.Props
import scala.collection.mutable.ArrayBuffer
import io.github.tomykaira.constraintscala.StaticConstraint

class MessageLog {
  private[this] val messageBuffer = new StaticConstraint[ArrayBuffer[Packet]](ArrayBuffer())

  def subscriber: Props = Props(classOf[LogAccumulator], messageBuffer)

  def newPanel: LogPanel = new LogPanel(messageBuffer)
}

package io.github.tomykaira.withlog

import akka.util.ByteString
import akka.io.{IO, Tcp}
import akka.actor.{Props, ActorRef, Actor}
import java.net.InetSocketAddress


object Client {
  def props(remote: InetSocketAddress, replies: ActorRef) =
    Props(classOf[Client], remote, replies)
}

class Client(remote: InetSocketAddress, listener: ActorRef) extends Actor {

  import Tcp._
  import context.system

  IO(Tcp) ! Connect(remote)

  def receive = {
    case CommandFailed(_: Connect) =>
      listener ! "failed"
      context stop self

    case c @ Connected(remote, local) =>
      listener ! c
      val connection = sender
      connection ! Register(self)
      context become {
        case data: ByteString => connection ! Write(data)
        case CommandFailed(w: Write) => // O/S buffer was full
        case Received(data) => listener ! data
        case "close" => connection ! Close
        case _: ConnectionClosed => context stop self
      }
  }
}
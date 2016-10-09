package io.github.tomykaira.withlog.db

import io.github.tomykaira.withlog.{Packet, PacketSubscriber, Message}
import scala.slick.driver.H2Driver.simple._
import scala.annotation.tailrec

class CoreAcquisitionLogger extends PacketSubscriber {
  protected val requestSize = -1
  protected val concernedTags = Seq(Message.Speech.tag)

  protected def process(received: Seq[Packet]) {
    val dropRegex = "^([^は]*)コアを手に入れました。$".r
    val treasureBoxRegex = "^コアの宝箱 レベル(.*)から(.*)コアを獲得しました。$".r

    @tailrec
    def collect(received: List[Packet], cores: List[(Long, String, String)]): List[(Long, String, String)] = {
      received match {
        case Nil => cores
        case h :: t =>
          val message = h.message match {
            case m: Message.Speech if m.typ == "Sys" =>
              Some(m.message)
            case _ =>
              None
          }
          val next = message flatMap { m =>
            dropRegex.findFirstIn(m) match {
              case Some(dropRegex(name)) => Some((h.key, "Battle", name))
              case None => None
            }
          } orElse {
            message flatMap { m =>
              treasureBoxRegex.findFirstIn(m) match {
                case Some(treasureBoxRegex(level, name)) => Some((h.key, level, name))
                case None => None
              }
            }
          } map (_ :: cores) getOrElse cores
          collect(t, next)
      }
    }

    Manager.db.withSession { implicit session: Session =>
      collect(received.toList, List()) filter { core =>
        Query(CoreAcquisitions)
          .filter { _.key === core._1 }
          .firstOption.isEmpty
      } foreach { core =>
        val fullName = core._3 + "コア"
        val q = for {
          i <- Items if i.name === fullName
        } yield i
        val gameId = q.firstOption map { i =>
          i.gameId
        } getOrElse {
          println(s"GameId of $fullName did not found")
          0L
        }
        CoreAcquisitions.forInsert insert CoreAcquisition(None, core._1, gameId, fullName, core._2)
      }
    }
  }
}

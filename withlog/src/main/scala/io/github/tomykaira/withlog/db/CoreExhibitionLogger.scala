package io.github.tomykaira.withlog.db

import io.github.tomykaira.withlog.{Packet, PacketSubscriber, Message}
import scala.slick.driver.H2Driver.simple._
import scala.annotation.tailrec

class CoreExhibitionLogger extends PacketSubscriber {
  protected val requestSize = -1
  protected val concernedTags = Seq(Message.Speech.tag)

  protected def process(received: Seq[Packet]) {
    val regex = "^(.*)コア(\\d*個)?を(\\d*)Poroで市場に出品しました。$".r

    @tailrec
    def collect(received: List[Packet], cores: List[(Long, String, Int, Int)]): List[(Long, String, Int, Int)] = {
      received match {
        case Nil => cores
        case h :: t =>
          val next = (h.message match {
            case m: Message.Speech if m.typ == "Sys" =>
              Some(m.message)
            case _ =>
              None
          }) flatMap { message =>
            regex.findFirstIn(message)
          } flatMap {
            case regex(name, countNullable, price) =>
              val count = if (countNullable == null) 1 else countNullable.slice(0, countNullable.length-1).toInt
              Some(name, count, price.toInt)
            case _ => None
          } map { t =>
            (h.key, t._1, t._2, t._3) :: cores
          } getOrElse cores
          collect(t, next)
      }
    }

    Manager.db.withSession { implicit session: Session =>
      collect(received.toList, List()) foreach { core =>
        val fullName = core._2 + "コア"
        val q = Query(CoreAcquisitions) filter { t1 =>
          t1.name === fullName &&
            !CoreExhibitions.filter { t2 => t2.acquisitionId === t1.id }.exists
        }
        q.firstOption flatMap { c => c.id } map { id =>
          CoreExhibitions.forInsert insert CoreExhibition(None, id, core._3, core._4, core._1, None)
        } getOrElse {
          println(s"Acquisition entry for $fullName is not found.  Skipping")
        }
      }
    }
  }
}

package io.github.tomykaira.withlog.db

import io.github.tomykaira.withlog._
import scala.slick.driver.H2Driver.simple._

class ItemDbConstructor extends PacketSubscriber {
  protected val requestSize = -1
  protected val concernedTags = Seq(Message.Item.tag)

  protected def process(received: Seq[Packet]) {
    val items = received flatMap { p =>
      p.message match {
        case m: Message.Item => m.items filter { _.shopId != 0 }
        case _ => Seq()
      }
    }
    Manager.db.withSession { implicit session: Session =>
      items foreach { item =>
        val q = for {
          i <- Items if i.gameId === item.id
        } yield i.length
        if (q.firstOption.getOrElse(0) == 0) {
          Items.forInsert insert Item(item)
        }
      }
    }
  }
}

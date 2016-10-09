package io.github.tomykaira.withlog.db

import scala.slick.driver.H2Driver.simple._
import scala.slick.driver.H2Driver.Table
import io.github.tomykaira.withlog.Message

case class Item(id: Option[Int], gameId: Long, shopId: Int, name: String, kana: String)

object Item {
  def apply(detail: Message.ItemDetail): Item =
    Item(None, detail.id, detail.shopId, detail.name, detail.kana)
}

object Items extends Table[Item]("items") {
  def id = column[Int]("id", O PrimaryKey, O AutoInc)
  def gameId = column[Long]("game_id", O DBType "Long unique")
  def shopId = column[Int]("shop_id", O DBType "Int")
  def name = column[String]("name", O DBType "varchar(255)")
  def kana = column[String]("kana", O DBType "varchar(255)")

  def * = id.? ~ gameId ~ shopId ~ name ~ kana <> ({ t => Item(t._1, t._2, t._3, t._4, t._5) }, Item.unapply _)

  def forInsert = gameId ~ shopId ~ name ~ kana <>
    ({ t => Item.apply(None, t._1, t._2, t._3, t._4) },
     { (i: Item) => Some((i.gameId, i.shopId, i.name, i.kana)) })
}
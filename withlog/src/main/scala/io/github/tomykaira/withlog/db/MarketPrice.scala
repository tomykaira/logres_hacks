package io.github.tomykaira.withlog.db

import scala.slick.driver.H2Driver.simple._

case class MarketPrice(id: Option[Int], shopId: Int, prefixLevel: Int, saleStartDate: java.sql.Date,
                        sellerCUID: String, sellerName: String, itemUID: String, coreCount: Int,
                        saleCount: Int, price: Int, endState: Int)

object MarketPrices extends Table[MarketPrice]("market_prices") {
  def id = column[Int]("id", O PrimaryKey, O AutoInc)
  def shopId = column[Int]("shop_id", O DBType "Int")
  def prefixLevel = column[Int]("prefix_level")
  def saleStartDate = column[java.sql.Date]("sale_start_date")
  def sellerCUID = column[String]("seller_cuid")
  def sellerName = column[String]("seller_name")
  def itemUID = column[String]("item_uid")
  def coreCount = column[Int]("core_count")
  def saleCount = column[Int]("sale_count")
  def price = column[Int]("price")
  def endState = column[Int]("end_state")

  def * = id.? ~ shopId ~ prefixLevel ~ saleStartDate ~
    sellerCUID ~ sellerName ~ itemUID ~ coreCount ~
    saleCount ~ price ~ endState <> (MarketPrice.apply _, MarketPrice.unapply _)

  def forInsert = shopId ~ prefixLevel ~ saleStartDate ~
    sellerCUID ~ sellerName ~ itemUID ~ coreCount ~
    saleCount ~ price ~ endState <>
    ({ t => MarketPrice.apply(None, t._1, t._2, t._3, t._4, t._5, t._6, t._7, t._8, t._9, t._10) },
     { (m: MarketPrice) => Some((m.shopId, m.prefixLevel, m.saleStartDate, m.sellerCUID, m.sellerName,
                                 m.itemUID, m.coreCount, m.saleCount, m.price, m.endState)) })
}
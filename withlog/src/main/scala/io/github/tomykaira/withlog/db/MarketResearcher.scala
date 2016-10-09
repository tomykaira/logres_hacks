package io.github.tomykaira.withlog.db

import dispatch._
import scala.util.parsing.json.{JSONObject, JSONArray, JSON}
import scala.concurrent.ExecutionContext.Implicits.global

class MarketResearcher {
  private[this] val host = "211.130.154.19"
  private[this] val referer = "http://cdn01.mmo-logres.com/mg/Logres_ver1001.swf"

  def searchItem(shopId: Int): Future[List[MarketPrice]] = {
    val result = Http(searchRequest(shopId) OK as.String)
    result map { data =>
      convertJsonResponse(data)
    }
  }

  def convertJsonResponse(data: String): List[MarketPrice] = {
    JSON.parseRaw(data) map { json =>
      json.asInstanceOf[JSONArray].list.flatMap { row: Any =>
        val map = row.asInstanceOf[JSONObject].obj.asInstanceOf[Map[String, String]]
        (for {
          shopId <- map.get("ItemID") flatMap safeToInt
          prefixLevel <- map.get("PrefixLevel") flatMap safeToInt
          saleStartDate <- map.get("SaleStartDate") flatMap safeToDate
          sellerCUID <- map.get("SellerCUID")
          sellerName <- map.get("SellerName")
          itemUID <- map.get("ItemUID")
          coreCount <- map.get("CoreCount") flatMap safeToInt
          saleCount <- map.get("SaleCount") flatMap safeToInt
          price <- map.get("Price") flatMap safeToInt
          endState <- map.get("EndState") flatMap safeToInt
        } yield MarketPrice(None, shopId, prefixLevel, saleStartDate, sellerCUID, sellerName,
            itemUID, coreCount, saleCount, price, endState)).toList
      }
    } getOrElse List[MarketPrice]()
  }

  def searchRequest(shopId: Int): Req = {
    url("http://%s/api/index.php" format host)
      .<<?(Map("id" -> "market", "command" -> "record", "itemId" -> shopId.toString, "level" -> "0", "time" -> time))
      .addHeader("Referer", referer)
  }

  private def time: String = {
    ((System.currentTimeMillis() / 1000).toDouble / 600.0).toString
  }

  private def safeToInt(s: String): Option[Int] = {
    try {
      Some(s.toInt)
    } catch {
      case _: java.lang.NumberFormatException => None
    }
  }

  private def safeToDate(s: String): Option[java.sql.Date] =
    safeToInt(s) map { i => new java.sql.Date(i.toLong * 1000L) }
}

package io.github.tomykaira.withlog.db

import scala.slick.driver.H2Driver.simple._

case class CoreAcquisition(id: Option[Int], key: Long, gameId: Long, name: String, from: String)

object CoreAcquisitions extends Table[CoreAcquisition]("core_acquisitions") {
  def id = column[Int]("id", O PrimaryKey, O AutoInc)
  def key = column[Long]("key", O DBType "Long UNIQUE")
  def gameId = column[Long]("game_id", O DBType "Long")
  def name = column[String]("name", O DBType "varchar(255)")
  def from = column[String]("from", O DBType "varchar(255)")

  def * = id.? ~ key ~ gameId ~ name ~ from <> (CoreAcquisition.apply _, CoreAcquisition.unapply _)

  def forInsert = key ~ gameId ~ name ~ from <>
    ({ t => CoreAcquisition.apply(None, t._1, t._2, t._3, t._4) },
      { (c: CoreAcquisition) => Some((c.key, c.gameId, c.name, c.from)) })
}
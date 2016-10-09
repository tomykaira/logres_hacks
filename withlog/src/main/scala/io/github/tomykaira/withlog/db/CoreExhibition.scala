package io.github.tomykaira.withlog.db

import scala.slick.driver.H2Driver.simple._

case class CoreExhibition(id: Option[Int], acquisitionId: Int, count: Int, price: Int, startedAt: Long, endedAt: Option[Long])

object CoreExhibitions extends Table[CoreExhibition]("core_exhibitions") {
  def id = column[Int]("id", O PrimaryKey, O AutoInc)
  def acquisitionId = column[Int]("acquisition_id")
  def count = column[Int]("count")
  def price = column[Int]("price")
  def startedAt = column[Long]("started_at")
  def endedAt = column[Option[Long]]("ended_at")

  def * = id.? ~ acquisitionId ~ count ~ price ~ startedAt ~ endedAt <> (CoreExhibition.apply _, CoreExhibition.unapply _)

  def forInsert = acquisitionId ~ count ~ price ~ startedAt <>
    ({ t => CoreExhibition.apply(None, t._1, t._2, t._3, t._4, None) },
      { (c: CoreExhibition) => Some((c.acquisitionId, c.count, c.price, c.startedAt)) })
}
package io.github.tomykaira.withlog.db

import scala.slick.driver.H2Driver.simple._

object Manager {
  val db = Database.forURL("jdbc:h2:file:withlog", driver = "org.h2.Driver")

  def initialize() {
    db.withSession { implicit session: Session =>
      Items.ddl.create
      CoreAcquisitions.ddl.create
      CoreExhibitions.ddl.create
    }
  }
}

package io.github.tomykaira.withlog.db

import scala.swing._
import scala.slick.driver.H2Driver.simple._
import javax.swing.border.EmptyBorder
import java.text.SimpleDateFormat
import java.util.Date
import io.github.tomykaira.withlog.{RowSortTable, TabPanel}
import scala.swing.event.ButtonClicked
import javax.swing.table.DefaultTableModel

class CoreMarketPanel extends TabPanel {
  titleField.text = "Core"

  private[this] val table = new RowSortTable() {
    private[this] val dateFormat = new SimpleDateFormat("MM/dd HH:mm:ss")

    def formatKeyAsDate(key: Long): String =
      dateFormat.format(new Date(key / 1000)) + " " + key.toString

    autoResizeMode = Table.AutoResizeMode.Off

    model.addColumn("GameID")
    model.addColumn("Name")
    model.addColumn("From")
    model.addColumn("Price")
    model.addColumn("Count")

    def refresh() {
      for (i <- 0 to model.getRowCount - 1) {
        model.removeRow(0)
      }

      Manager.db.withSession { implicit session: Session =>
        Query(CoreAcquisitions)
          .leftJoin(CoreExhibitions).on { (t1, t2) => t1.id === t2.acquisitionId }
          .map { case (t1, t2) => (t1.gameId, t1.name, t1.from, t2.price.?, t2.count.?) }
          .list foreach { row =>
          val data = Array(row._1, row._2, row._3, row._4.map(_.toString).getOrElse(""), row._5.map(_.toString).getOrElse(""))
          model.addRow(data.asInstanceOf[Array[AnyRef]])
        }
      }
    }
  }

  private[this] val refreshButton = new Button("Refresh") {
    reactions += {
      case _: ButtonClicked => table.refresh()
    }
  }

  private[this] val controlPanel = new GridPanel(1, 2) {
    contents ++= Seq(titleField, refreshButton)
  }

  table.refresh()

  add(controlPanel, BorderPanel.Position.North)
  add(new ScrollPane(table) {
    border = new EmptyBorder(0,0,0,0)
  }, BorderPanel.Position.Center)

}

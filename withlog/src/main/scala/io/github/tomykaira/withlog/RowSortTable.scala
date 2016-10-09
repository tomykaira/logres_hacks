package io.github.tomykaira.withlog

import scala.swing.Table

class RowSortTable extends Table {
  import javax.swing.table._

  class RowSortTableModel extends DefaultTableModel {
    private var columnClass = Array[Class[_]]()

    override def addColumn(name: Object) { addColumn(name, classOf[Object]) }
    def addColumn(name: Object, c: Class[_]) {
      columnClass :+= c
      super.addColumn(name)
    }

    override def getColumnClass(index: Int) = columnClass(index)
  }

  override def apply(row: Int, column: Int): Any = model.getValueAt(viewToModelRow(row), viewToModelColumn(column))
  def viewToModelRow(idx: Int) = peer.convertRowIndexToModel(idx)
  def modelToViewRow(idx: Int) = peer.convertRowIndexToView(idx)

  model = new RowSortTableModel
  override def model = super.model.asInstanceOf[RowSortTableModel]

  peer.setAutoCreateRowSorter(true)
}
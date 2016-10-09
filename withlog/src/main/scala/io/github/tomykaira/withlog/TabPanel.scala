package io.github.tomykaira.withlog

import scala.swing.{TextField, BorderPanel}
import scala.swing.event.ValueChanged
import io.github.tomykaira.constraintscala.Constraint

trait TabPanel extends BorderPanel {
  protected val titleField = new TextField("")
  val title: Constraint[String] = new Constraint[String]({ titleField.text })
  titleField.reactions += { case _: ValueChanged => title.invalidate() }
}

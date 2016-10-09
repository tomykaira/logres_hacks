package io.github.tomykaira.withlog

case class EquipCondition(jobs: List[Job], level: Int) {
  override def toString: String = (jobs mkString " ") + s" Lv. $level"
}

package io.github.tomykaira.withlog

case class Parameters(params: Map[String, Int])

object Parameters {
  private[this] val keyList = Seq(
    "HP", "物攻", "物防", "命中", "回避", "クリティカル", "魔攻", "魔防",
    "火攻", "水攻", "風攻", "土攻", "光攻", "闇攻",
    "火耐", "水耐", "風耐", "土耐", "光耐", "闇耐")

  def apply(list: Seq[Int]): Parameters = {
    Parameters(Map() ++ keyList.zip(list))
  }
}
package io.github.tomykaira.withlog.db

import akka.actor.Props

class ItemManager {
  def subscriber: Props = Props[ItemDbConstructor]
}

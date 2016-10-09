package io.github.tomykaira.withlog.db

import akka.actor.Props
import io.github.tomykaira.withlog.TabPanel

class CoreManager {
  def subscribers: Seq[Props] = Seq(Props[CoreAcquisitionLogger], Props[CoreExhibitionLogger])

  def newPanel: TabPanel = new CoreMarketPanel
}

package io.github.tomykaira.withlog

import javax.swing.JPopupMenu
import scala.swing.Component
import scala.swing.SequentialContainer.Wrapper

object PopupMenu {
  private[PopupMenu] trait JPopupMenuMixin { def popupMenuWrapper: PopupMenu }
}

class PopupMenu extends Component with Wrapper {

  override lazy val peer: JPopupMenu = new JPopupMenu with PopupMenu.JPopupMenuMixin with SuperMixin {
    def popupMenuWrapper = PopupMenu.this
  }

  def show(invoker: Component, x: Int, y: Int): Unit = peer.show(invoker.peer, x, y)
}

package io.github.tomykaira.withlog

import scala.swing._
import io.github.tomykaira.constraintscala.{StaticConstraint, Constraint}
import scala.swing.event._
import scala.collection.immutable.SortedMap
import scala.collection.mutable.ArrayBuffer
import scala.swing.ListView.Renderer
import javax.swing.border.EmptyBorder
import scala.swing.MenuItem
import scala.swing.event.ButtonClicked
import scala.swing.event.SelectionChanged
import javax.swing.JList

class LogPanel(buffer: StaticConstraint[ArrayBuffer[Packet]]) extends TabPanel {
  titleField.text = "Log"

  private[this] val selectorMap: Map[String, Message => Boolean] = SortedMap[String, Message => Boolean](
    "All" -> { case _ => true },
    "Talk" -> {
      case m: Message.Speech => List("Ar", "Pt", "Cl", "Gl").contains(m.typ)
      case _ => false
    },
    "Friends" -> {
      case m: Message.Speech => List("Pt", "Cl").contains(m.typ)
      case _ => false
    },
    "System" -> {
      case m: Message.Speech => List("Sys", "NPC").contains(m.typ)
      case _ => true
    },
    "Chaos" -> {
      case m: Message.Speech => List("Ar", "Pt", "Cl").contains(m.typ)
      case Message.ChaosStatus(_) | Message.ChaosLog(_) | Message.Banner(_) => true
      case _ => false
    }
  ).withDefaultValue { _ => true }

  private[this] val messageSelectorBox = new ComboBox(selectorMap.keys.toSeq) {
    listenTo(selection)
  }
  val messageSelector = new Constraint[Message => Boolean]({ selectorMap(messageSelectorBox.selection.item) })
  messageSelectorBox.reactions += { case _: SelectionChanged => messageSelector.invalidate() }

  private[this] val filterField = new TextField("")
  val filter: Constraint[String] = new Constraint[String]({ filterField.text })
  filterField.reactions += { case _: ValueChanged => filter.invalidate() }

  private[this] val noUpdateCheck = new CheckBox("Stop update") { selected = false }

  private[this] val sinceField = new TextField("0")
  private[this] val since = new Constraint[Long]({
    try { sinceField.text.toLong } catch { case _: NumberFormatException => 0L }
  })
  sinceField.reactions += { case _:ValueChanged => since.invalidate() }

  private[this] val sinceNowButton = new Button("Since now") {
    reactions += {
      case _: ButtonClicked => sinceField.text = buffer.get.lastOption map (_.key.toString) getOrElse "0"
    }
  }
  private[this] val clearSinceButton = new Button("Clear") {
    reactions += {
      case _: ButtonClicked => sinceField.text = "0"
    }
  }

  private[this] val controlPanel = new GridPanel(1, 7) {
    contents ++= Seq(titleField, messageSelectorBox, filterField, noUpdateCheck, sinceField, sinceNowButton, clearSinceButton)
  }


  private[this] val logLines = new ListView[Packet]() {
    focusable = true

    listenTo(mouse.clicks)

    reactions += {
      case e: MouseButtonEvent if e.triggersPopup =>
        val typedPeer = peer.asInstanceOf[JList[Packet]]
        val index = typedPeer.locationToIndex(e.point)
        selectIndices(index)
        if (listData.indices.contains(index)) {
          val packet = listData(index)
          makePopup(packet).show(this, e.point.x, e.point.y)
        }
    }

    renderer = new Renderer[Packet] {
      def componentFor(list: ListView[_], isSelected: Boolean, focused: Boolean, packet: Packet, index: Int): Component = {
        new TextArea() {
          text = packet.displayable
          opaque = true
          background = if (isSelected || focused) new Color(192, 192, 255) else java.awt.Color.white
          lineWrap = false
        }
      }
    }

    buffer onChange { _ => updateContent() }
    messageSelector onChange { _ => updateContent() }
    filter onChange { _ => updateContent() }
    since onChange { _ => updateContent() }

    private def updateContent() {
      if (noUpdateCheck.selected)
        return
      val query = filter.get
      listData = buffer.get filter {
        p => p.key >= since.get && messageSelector.get(p.message) && (query == "" || p.message.displayable.contains(query))
      }
      repaint()
      listData.indices.lastOption map peer.ensureIndexIsVisible
    }

    private def copyToClipboard(string: String) {
      val clipboard = java.awt.Toolkit.getDefaultToolkit.getSystemClipboard
      val sel = new java.awt.datatransfer.StringSelection(string)
      clipboard.setContents(sel, sel)
    }

    private def makePopup(packet: Packet): PopupMenu = {
      new PopupMenu {
        contents += new MenuItem(new Action("Copy key") {
          def apply() = copyToClipboard(packet.key.toString)
        })
        contents += new MenuItem(new Action("Copy raw data") {
          def apply() = copyToClipboard(packet.message.data map ("%2x" format _) mkString " ")
        })
        contents += new MenuItem(new Action("Copy displayed data") {
          def apply() = copyToClipboard(packet.message.displayable)
        })
      }
    }
  }

  add(controlPanel, BorderPanel.Position.North)
  add(new ScrollPane(logLines) {
    border = new EmptyBorder(0,0,0,0)
  }, BorderPanel.Position.Center)
}

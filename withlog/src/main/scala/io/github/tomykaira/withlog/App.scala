package io.github.tomykaira.withlog

import scala.swing._
import akka.actor.{Props, ActorSystem}
import java.net.InetSocketAddress
import io.github.tomykaira.constraintscala.StaticConstraint
import scala.collection.mutable.ArrayBuffer
import javax.swing.UIManager
import scala.collection.JavaConverters._
import io.github.tomykaira.withlog.db.{CoreManager, ItemManager}

object App extends SimpleSwingApplication {
  val messageLog = new MessageLog
  val itemManager = new ItemManager
  val coreManager = new CoreManager

  def top: Frame = new MainFrame() {
    title = "Withlog"

    val baseFont = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment.getAllFonts find { font =>
      font.getName == "Ume Gothic"
    } getOrElse font deriveFont(java.awt.Font.PLAIN, 14)

    UIManager.getDefaults.entrySet().asScala foreach { entry =>
      if (entry.getKey.toString.endsWith(".font"))
        UIManager.put(entry.getKey, baseFont)
    }

    val tabbedPane: TabbedPane = new TabbedPane
    contents = tabbedPane

    menuBar = new MenuBar {
      contents += new MenuItem(Action("Add tab") {
        addPane(messageLog.newPanel)
      })

      contents += new MenuItem(Action("Add core tab") {
        addPane(coreManager.newPanel)
      })

      contents += new MenuItem(Action("Remove current tab") {
        val i = tabbedPane.selection.index
        if (i >= 0) {
          tabbedPane.pages.remove(i)
        }
      })
    }

    def addPane(panel: TabPanel) {
      tabbedPane.pages += new TabbedPane.Page("new tab", panel) {
        panel.title.onChange { title = _ }
      }
    }

    addPane(messageLog.newPanel)
  }

  override def main(args: Array[String]) {
    if (args.length > 0 && args(0) == "init") {
      println("Initializing DB")
      db.Manager.initialize()
    }

    System.setProperty("awt.useSystemAAFontSettings", "on")
    System.setProperty("swing.aatext", "true")

    val system = ActorSystem("System")
    val server = new InetSocketAddress("localhost", 8282)

    def startSubscriber(subscriber: Props) {
      val listener = system.actorOf(subscriber)
      system.actorOf(Client.props(server, listener))
    }

    startSubscriber(messageLog.subscriber)
    startSubscriber(itemManager.subscriber)
    coreManager.subscribers.foreach(startSubscriber)

    super.main(args)
  }
}

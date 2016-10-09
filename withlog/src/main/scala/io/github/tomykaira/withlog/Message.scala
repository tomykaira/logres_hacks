package io.github.tomykaira.withlog

import akka.util.ByteString
import scala.collection.immutable.HashMap

sealed trait Message extends ByteStringHelper {
  val data: ByteString
  val displayable: String
}

object Message {
  private def bytes(data: Int*): ByteString = ByteString(data.toArray.map(_.asInstanceOf[Byte]))

  lazy private[this] val table: Map[ByteString, ByteString => Message] = HashMap(
    Speech.tag -> { Speech(_) },
    ChaosStatus.tag -> { ChaosStatus(_) },
    ItemDescription.tag -> { ItemDescription(_) },
    ItemDescription2.tag -> { ItemDescription2(_) },
    Banner.tag -> { Banner(_) },
    ChaosLog.tag -> { ChaosLog(_) },
    Item.tag -> { Item(_) }
  )

  object Speech {
    val tag: ByteString = bytes(0x95, 0x80, 0x5d, 0x14)
  }

  object ChaosStatus {
    val tag: ByteString = bytes(0x2d, 0x8f, 0xd6, 0x0d)
  }

  object ItemDescription {
    val tag: ByteString = bytes(0x5b, 0x53, 0xc6, 0xa8)
  }

  object ItemDescription2 {
    val tag: ByteString = bytes(0x09, 0xec, 0xae, 0x2f)
  }

  object Banner {
    val tag: ByteString = bytes(0x53, 0xaa, 0x76, 0x1f)
  }

  object ChaosLog {
    val tag: ByteString = bytes(0xf9, 0x9b, 0x81, 0x73)
  }

  object Item {
    val tag: ByteString = bytes(0xf0, 0xd3, 0x03, 0xf5)
  }

  def apply(data: ByteString): Message =
    try {
      table.get(data.take(4)).getOrElse(Unknown(_))(data)
    } catch {
      case e: Exception =>
        System.err.println("Decode error")
        System.err.println(data)
        e.printStackTrace()
        Broken(data)
    }

  case class Speech(data: ByteString) extends Message {
    lazy val displayable =
      "%-8s%s%s" format (typ, if (speaker.isEmpty) "" else speaker + ": ", message)

    val typ: String = data(4) match {
      case 0x00 => "Ar"
      case 0x01 => "Pt"
      case 0x02 => "Cl"
      case 0x03 => "Gl"
      case 0x04 => "Di"
      case 0x06 => "Sys"
      case 0x07 => "NPC"
      case unknown => unknown.toString
    }

    val message: String = readString(data.drop(16))._1

    val speaker: String = {
      val afterMessage = readString(data.drop(16))._2
      readString(afterMessage.drop(8))._1
    }
  }

  case class ChaosStatus(data: ByteString) extends Message {
    lazy val displayable =
      "Chaos   %-8s %s 人 %s pts" format (eventString, participants.toString, pts.toString)

    val event = readInt(data.drop(16))

    val participants = readInt(data.drop(20))

    val pts = readString(data.drop(24))._1.toInt

    lazy val eventString = event match {
      case 1 => "参戦"
      case 2 => "撤退"
      case 3 => "参戦中"
      case code => new StringBuilder().append("Code ").append(code).result()
    }
  }

  case class ChaosLog(data: ByteString) extends Message {
    lazy val displayable: String =
      "Chaos   %s" format message

    val message: String = readString(data.drop(24))._1
  }

  case class Banner(data: ByteString) extends Message {
    private[this] val flashMap = HashMap(
      "chaos/end.swf" -> "カオス終了",
      "chaos/detect.swf" -> "カオス発見",
      "rarechaos/detect.swf" -> "レアカオス発見",
      "chaos/finish.swf" -> "カオス討伐",
      "rarechaos/finish.swf" -> "レアカオス討伐"
    )

    lazy val displayable =
      "Banner  %s%s" format (event, if (note.isEmpty) "" else " " + note)

    val (event, note) = initialize(data)

    private def initialize(data: ByteString) = {
      val event = readString(data.drop(4))
      val sound = readString(event._2)
      val note = readString(sound._2)
      (flashMap.get(event._1).getOrElse(event._1), note._1)
    }
  }

  case class ItemDetail(id: Long, shopId: Int, count: Int, subCode: ByteString,
                        name: String, kana: String, icon: ByteString, rest: ByteString)

  case class Item(data: ByteString) extends Message {
    lazy val displayable = {
      val builder = new StringBuilder
      builder.append("Item    Char: %s Code: %s\n" format (charCode.mkString(" "), unknownCode.toString))
      builder.append(items map { detail =>
        "        %016x (%08x): %s [%d]\n        %s\n        %s" format (detail.id, detail.shopId, detail.name, detail.count,
          detail.subCode.mkString(" "), detail.rest.mkString(" "))
      } mkString "\n")
      builder.result()
    }

    val charCode = data.slice(4, 12)
    val unknownCode = readInt(data.drop(12))
    val items: List[ItemDetail] = readItemDetails(data.drop(20), readInt(data.drop(16)))

    private def readItemDetails(data: ByteString, count: Int): List[ItemDetail] = {
      if (count <= 0) {
        if (data.nonEmpty)
          throw new RuntimeException("data should be empty when count is 0, but has data " + data.toString)
        List()
      } else {
        val id = readLong(data)
        val shopId = readInt(data.drop(8))
        val number = readInt(data.drop(12))
        val subCode = data.slice(16, 20)
        val (name, rest1) = readString(data.drop(20))
        val (kana, rest2) = readString(rest1)
        val icon = rest2.slice(0, 16)
        val unknown = rest2.slice(16, 84)
        ItemDetail(id, shopId, number, subCode, name, kana, icon, unknown) :: readItemDetails(rest2.drop(84), count - 1)
      }
    }
  }

  case class ItemDescription(data: ByteString) extends Message {
    lazy val displayable: String =
      (Seq(
        s"IDesc   $name",
        s"$id $shopId $unknown $name($avatarName) $description",
        equipCondition,
        baseParams,
        extParams,
        "skills " + skills.mkString(", ")
      ) ++ (unknowns map { u => u map ("%02x" format _) mkString " " })).mkString("\n        ")

    val charCode = data.slice(4, 12)
    val id = readLong(data.drop(12))
    val shopId = readInt(data.drop(20))
    val unknown = readInt(data.drop(24))
    val (name: String,
         avatarName: String,
         description: String,
         equipCondition: EquipCondition,
         baseParams: Parameters,
         extParams: Parameters,
         skills: List[String],
         unknowns: Seq[ByteString]) = { () =>
      val (name, rest1) = readString(data.drop(28))
      val (avatarName, rest2) = readString(rest1)
      val (description, rest3) = readString(rest2)
      val jobCode = rest3(0x1b).toInt
      val jobs = Map(Novice -> 0x1, Fighter -> 0x2, Knight -> 0x4, Ranger -> 0x8, Priest -> 0x10, Magician -> 0x20)
        .foldLeft(List[Job]()) { (list, job) => if ((jobCode & job._2) != 0) job._1 :: list else list }
      val level = rest3(0x1f).toInt
      val baseParams = Parameters((0 to 19) map { i => readInt(rest3.drop(40 + i * 4)) })
      val extParams = Parameters((20 to 39) map { i => readInt(rest3.drop(40 + i * 4)) })
      val skillCount = readInt(rest3.drop(200))
      val (skills, rest4) = (1 to skillCount).foldLeft((List[String](), rest3.drop(204))) { (pair, _) =>
        val (skill, rest) = readString(pair._2)
        (skill :: pair._1, rest)
      }
      (name, avatarName, description, EquipCondition(jobs, level), baseParams, extParams, skills, Seq(rest3.take(0x1b), rest4))
    }.apply()
  }

  case class ItemDescription2(data: ByteString) extends Message {
    lazy val displayable: String =
      (Seq(
        s"IDesc2  $name",
        s"$id $shopId $unknown $name($avatarName) $description",
        equipCondition,
        baseParams,
        extParams,
        "skills " + skills.mkString(", ")
      ) ++ (unknowns map { u => u map ("%02x" format _) mkString " " })).mkString("\n        ")

    val charCode = data.slice(4, 12)
    val place = readInt(data.drop(12))
    val id = readLong(data.drop(16))
    val shopId = readInt(data.drop(24))
    val unknown = readInt(data.drop(28))
    val (name: String,
    avatarName: String,
    description: String,
    equipCondition: EquipCondition,
    baseParams: Parameters,
    extParams: Parameters,
    skills: List[String],
    unknowns: Seq[ByteString]) = { () =>
      val (name, rest1) = readString(data.drop(32))
      val (avatarName, rest2) = readString(rest1)
      val (description, rest3) = readString(rest2)
      val jobCode = rest3(0x1b).toInt
      val jobs = Map(Novice -> 0x1, Fighter -> 0x2, Knight -> 0x4, Ranger -> 0x8, Priest -> 0x10, Magician -> 0x20)
        .foldLeft(List[Job]()) { (list, job) => if ((jobCode & job._2) != 0) job._1 :: list else list }
      val level = rest3(0x1f).toInt
      val baseParams = Parameters((0 to 19) map { i => readInt(rest3.drop(40 + i * 4)) })
      val extParams = Parameters((20 to 39) map { i => readInt(rest3.drop(40 + i * 4)) })
      val skillCount = readInt(rest3.drop(200))
      val (skills, rest4) = (1 to skillCount).foldLeft((List[String](), rest3.drop(204))) { (pair, _) =>
        val (skill, rest) = readString(pair._2)
        (skill :: pair._1, rest)
      }
      (name, avatarName, description, EquipCondition(jobs, level), baseParams, extParams, skills, Seq(rest3.take(0x1b), rest4))
    }.apply()
  }

  case class Unknown(data: ByteString) extends Message{
    val displayable: String =
      "Unknown %s" format (data.take(4) map ("%02x" format _) mkString " ")
  }

  case class Broken(data: ByteString) extends Message{
    val displayable: String =
      "Broken %s" format (data.take(4) map ("%02x" format _) mkString " ")
  }
}

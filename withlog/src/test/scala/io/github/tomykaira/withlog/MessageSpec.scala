package io.github.tomykaira.withlog

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers
import akka.util.ByteString

class MessageSpec extends FunSpec with ShouldMatchers {
  private def bytes(data: String): ByteString =
    ByteString(data.split(Array(' ', '\n'))
      filter { _.length > 0 }
      map { Integer.parseInt(_, 16).asInstanceOf[Byte] })

  describe("Message") {
    val packet = bytes("""
95 80 5d 14 07 00 00 00 00 00 00 00 00 00 00 00
1e 00 00 00 e4 bd 95 e3 81 a7 e3 82 82 e9 a0 90
e3 81 91 e3 81 a6 e3 81 8f e3 82 8c e3 82 88 e3
80 82 a7 00 04 01 30 d7 e8 51 13 00 00 00 e5 80
89 e5 ba ab e7 95 aa 20 e3 83 a2 e3 83 83 e3 83
84 00 00 00 00 00 00 00 00 00 00 00 00
""")
    it("should decode type, message, and speaker") {
      val message = Message.Speech(packet)
      message.typ should equal ("NPC")
      message.message should equal ("何でも預けてくれよ。")
      message.speaker should equal ("倉庫番 モッツ")
    }
  }

  describe("ChaosStatus") {
    val packet = bytes("""
2d 8f d6 0d 00 00 00 00 03 00 02 01 bf 03 e2 51
02 00 00 00 07 00 00 00 04 00 00 00 33 35 31 39
00 00 00 00
""")
    it("should decode event, participants, and point") {
      val status = Message.ChaosStatus(packet)
      status.event should equal (2)
      status.participants should equal (7)
      status.pts should equal (3519)
    }
  }

  describe("ChaosLog") {
    val packet = bytes("""
f9 9b 81 73 e9 00 f9 00 58 4c eb 51 00 00 00 00
01 00 00 00 01 00 00 00 37 00 00 00 e8 8a b1 e9
9e a0 20 e3 81 8c 20 e3 83 aa e3 83 90 e3 83 bc
e3 82 b9 e3 82 a2 e3 82 bf e3 83 83 e3 82 af 32
20 32 38 38 38 32 20 70 74 20 e3 82 92 e7 8d b2
e5 be 97 24 00 04 01 30 4c 8f 51 06 00 00 00 e8
8a b1 e9 9e a0 24 00 04 01 30 4c 8f 51 01 00 00
00
""")
    it("should decode chaos log") {
      Message.ChaosLog(packet).message should equal ("花鞠 が リバースアタック2 28882 pt を獲得")
    }
  }

  describe("Banner") {
    val packet = bytes("""
53 aa 76 1f 14 00 00 00 72 61 72 65 63 68 61 6f
73 2f 66 69 6e 69 73 68 2e 73 77 66 1e 00 00 00
53 45 5f e3 83 8f e3 83 83 e3 83 94 e3 83 bc e3
82 b8 e3 83 b3 e3 82 b0 e3 83 ab e5 a4 a7 2f 00
00 00 e8 a8 8e e4 bc 90 e8 80 85 3a e3 81 82 e3
81 8c e3 81 a1 e3 82 83 e3 82 93 20 28 20 31 30
30 30 30 30 20 70 74 73 20 e7 8d b2 e5 be 97 20
29
""")
    it("should decode event type and note") {
      val banner = Message.Banner(packet)
      banner.event should equal ("レアカオス討伐")
      banner.note should equal ("討伐者:あがちゃん ( 100000 pts 獲得 )")
    }
  }

  describe("Item") {
    it("should decode 1 item with empty name") {
      val packet = bytes("""
f0 d3 03 f5 03 00 02 01 bf 03 e2 51 c8 00 00 00
01 00 00 00 09 00 03 01 50 53 f0 51 00 00 00 00
00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
00 00 00 00
""")
      val item = Message.Item(packet)
      item.charCode should equal (bytes("03 00 02 01 bf 03 e2 51"))
      item.unknownCode should equal (0xc8)
      item.items should have length 1
      val detail = item.items.head
      detail.id should equal (0x51f0535001030009L)
      detail.name should equal ("")
      detail.kana should equal ("")
    }
    it("should decode 1 item with name") {
      val packet = bytes("""
f0 d3 03 f5 03 00 02 01 bf 03 e2 51 64 00 00 00
01 00 00 00 03 01 02 01 f1 51 e6 51 01 00 0f 00
01 00 00 00 00 00 00 00 06 00 00 00 e9 87 8e e8
8a b1 09 00 00 00 e3 81 ae e3 81 b0 e3 81 aa dd
05 64 14 0c 00 00 00 00 00 00 00 2f 4e 00 00 00
00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
00 00 00 00 00 00 00 00 05 00 00 00 00 00 00 00
f1 51 e6 51 00 00 00 00 00 00 00 00 00 00 00 01
00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
00 00 00
""")
      val item = Message.Item(packet)
      item.charCode should equal (bytes("03 00 02 01 bf 03 e2 51"))
      item.unknownCode should equal (0x64)
      item.items should have length 1
      val detail = item.items.head
      detail.id should equal (0x51e651f101020103L)
      detail.shopId should equal (0xf0001)
      detail.count should equal (1)
      detail.name should equal ("野花")
      detail.kana should equal ("のばな")
      detail.icon should equal (bytes("dd 05 64 14 0c 00 00 00 00 00 00 00 2f 4e 00 00"))
    }
    it("should decode 2 items with name") {
      val packet = bytes("""
f0 d3 03 f5 03 00 02 01 bf 03 e2 51 64 00 00 00
02 00 00 00 0c 00 02 01 50 93 e7 51 01 00 15 00
03 00 00 00 00 00 00 00 12 00 00 00 e5 b9 b3 e5
87 a1 e3 81 aa e3 82 ad e3 83 8e e3 82 b3 18 00
00 00 e3 81 b8 e3 81 84 e3 81 bc e3 82 93 e3 81
aa e3 81 8d e3 81 ae e3 81 93 35 08 64 14 0c 00
00 00 00 00 00 00 35 4e 00 00 00 00 00 00 00 00
00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
00 00 00 05 00 00 00 00 00 00 00 84 23 e9 51 00
00 00 00 00 00 00 00 00 00 00 01 00 00 00 00 00
00 00 00 00 00 00 00 00 00 00 00 00 00 00 1a 00
02 01 8d 31 e2 51 01 00 09 00 08 00 00 00 00 00
00 00 0f 00 00 00 e3 82 ad e3 83 8e e3 83 9d e8
83 9e e5 ad 90 12 00 00 00 e3 81 8d e3 81 ae e3
81 bd e3 81 bb e3 81 86 e3 81 97 85 03 64 14 0c
00 00 00 00 00 00 00 29 4e 00 00 00 00 00 00 00
00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
00 00 00 00 05 00 00 00 00 00 00 00 82 23 e9 51
00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 00
00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
""")
      val item = Message.Item(packet)
      item.charCode should equal (bytes("03 00 02 01 bf 03 e2 51"))
      item.unknownCode should equal (0x64)
      item.items should have length 2
      item.items(0).name should equal ("平凡なキノコ")
      item.items(1).name should equal ("キノポ胞子")
    }
  }
}

package io.github.tomykaira.withlog.db

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

class MarketResearcherSpec extends FunSpec with ShouldMatchers {
  val mr = new MarketResearcher

  describe("decode json response") {
    it("should decode JSON string to list of MarketPrices") {
      val json = """[{"ItemID":"6553800","PrefixLevel":"0","SaleStartDate":"1374929724","SellerCUID":"0103001650B0E5D3","SellerName":"\u30bf\u30d0\u30b5\u30d1\u30d2\u30e5\u30fc\u30e0","ItemUID":"0000000000000000","CoreCount":"0","SaleCount":"1","Price":"210","EndState":"1"},{"ItemID":"6553800","PrefixLevel":"0","SaleStartDate":"1374924300","SellerCUID":"0105000551D66579","SellerName":"\u9752\u78c1\u6d6a","ItemUID":"0000000000000000","CoreCount":"0","SaleCount":"1","Price":"210","EndState":"1"},{"ItemID":"6553800","PrefixLevel":"0","SaleStartDate":"1374920425","SellerCUID":"0102001151F399DA","SellerName":"\u51ac\u99ac\u3000\u6709\u5e0c","ItemUID":"0000000000000000","CoreCount":"0","SaleCount":"1","Price":"263","EndState":"1"},{"ItemID":"6553800","PrefixLevel":"0","SaleStartDate":"1374912009","SellerCUID":"0102000750664D7C","SellerName":"\u84ee\u83dc","ItemUID":"0000000000000000","CoreCount":"0","SaleCount":"1","Price":"105","EndState":"1"},{"ItemID":"6553800","PrefixLevel":"0","SaleStartDate":"1374904108","SellerCUID":"0106000C51A47175","SellerName":"\u30bc\u30a6\u30b9s","ItemUID":"0000000000000000","CoreCount":"0","SaleCount":"2","Price":"260","EndState":"1"},{"ItemID":"6553800","PrefixLevel":"0","SaleStartDate":"1374901571","SellerCUID":"0104000151636491","SellerName":"-JUNE_mhp-","ItemUID":"0000000000000000","CoreCount":"0","SaleCount":"1","Price":"315","EndState":"1"},{"ItemID":"6553800","PrefixLevel":"0","SaleStartDate":"1374897929","SellerCUID":"0101000751F08C30","SellerName":"\u30b6\u30ad\u30ad2\u7b49\u5175","ItemUID":"0000000000000000","CoreCount":"0","SaleCount":"2","Price":"210","EndState":"1"},{"ItemID":"6553800","PrefixLevel":"0","SaleStartDate":"1374897421","SellerCUID":"010700144FCBF5DE","SellerName":"\u043ci\u0438\u0434\u0442\u043e","ItemUID":"0000000000000000","CoreCount":"0","SaleCount":"1","Price":"263","EndState":"1"},{"ItemID":"6553800","PrefixLevel":"0","SaleStartDate":"1374877861","SellerCUID":"0103000D4FF7F8EE","SellerName":"\u30de\u30ed\u30a4","ItemUID":"0000000000000000","CoreCount":"0","SaleCount":"1","Price":"315","EndState":"1"},{"ItemID":"6553800","PrefixLevel":"0","SaleStartDate":"1374876628","SellerCUID":"0101000751201C69","SellerName":"Shingen","ItemUID":"0000000000000000","CoreCount":"0","SaleCount":"1","Price":"294","EndState":"1"}]"""
      val result = mr.convertJsonResponse(json)
      result should have length 10
      result.map(_.sellerCUID).head should equal ("0103001650B0E5D3")
    }
  }
}

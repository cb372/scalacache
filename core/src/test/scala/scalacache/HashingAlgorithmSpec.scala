package scalacache

import org.scalatest._

class HashingAlgorithmSpec extends FunSpec with Matchers {

  describe("implementing singletons") {

    it("should return their messageDigest fields properly") {
      Seq(MD5, SHA1, SHA256, SHA512).foreach(_.messageDigest)
    }

  }

}

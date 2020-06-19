package scalacache

import org.scalatest._
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class HashingAlgorithmSpec extends AnyFunSpec with Matchers {

  describe("implementing singletons") {

    it("should return their messageDigest fields properly") {
      Seq(MD5, SHA1, SHA256, SHA512).foreach(_.messageDigest)
    }

  }

}

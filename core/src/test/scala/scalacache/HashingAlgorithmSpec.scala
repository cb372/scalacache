package scalacache

import java.security.MessageDigest

import org.scalatest._

class HashingAlgorithmSpec extends FunSpec with Matchers {

  def digestFor(algo: HashingAlgorithm): MessageDigest = java.security.MessageDigest.getInstance(algo.name)

  describe("implementing classes") {
    it("should contain names that are valid MessageDigest algorithm names") {
      digestFor(MD5)
      digestFor(SHA1)
      digestFor(SHA256)
      digestFor(SHA512)
    }
  }

}

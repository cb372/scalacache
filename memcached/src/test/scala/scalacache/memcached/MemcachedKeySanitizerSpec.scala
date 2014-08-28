package scalacache.memcached

import org.scalatest.{ FlatSpec, ShouldMatchers }

import scalacache._

/**
 *
 * Author: c-birchall
 * Date:   13/11/07
 */
class ReplaceAndTruncateSanitizerSpec extends FlatSpec with ShouldMatchers {
  behavior of "ReplaceAndTruncateSanitizer"

  val sanitizer = new ReplaceAndTruncateSanitizer(maxKeyLength = 10)

  it should "truncate keys from the left if they are longer than maxKeyLength" in {
    val longKey = "0123456789A"
    sanitizer.toValidMemcachedKey(longKey) should be("123456789A")
  }

  it should "replace invalid chars with underscores" in {
    val invalidKey = "abc \t\r\nダメ"
    sanitizer.toValidMemcachedKey(invalidKey) should be("abc______")
  }

  it should "allow symbols in key" in {
    val validKey = "~`!@$"
    sanitizer.toValidMemcachedKey(validKey) should be(validKey)
  }

}

class HashingMemcachedKeySanitizerSpec extends FlatSpec with ShouldMatchers {
  behavior of "HashingMemcachedKeySanitizer"

  val longString = "lolol&%'(%$)$ほげほげ野郎123**+" * 500

  def hexToBytes(s: String): Array[Byte] = s.sliding(2, 2).map(Integer.parseInt(_, 16).toByte).toArray

  it should "return a hexadecimal hashed representation of the index string" in {
    val hashedValues = for {
      algo <- Seq(MD5, SHA1, SHA256, SHA512)
      hashingSanitizer = HashingMemcachedKeySanitizer(algo)
      hashed = hashingSanitizer.toValidMemcachedKey(longString)
    } yield hashed
    hashedValues.foreach(hexToBytes) // should work
    hashedValues.forall(_.length < 250) should be(true)
  }
}
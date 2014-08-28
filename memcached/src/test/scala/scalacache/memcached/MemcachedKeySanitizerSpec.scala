package scalacache.memcached

import org.scalatest.{ FlatSpec, ShouldMatchers }

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

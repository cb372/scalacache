package cacheable.memcached

import org.scalatest.{ FlatSpec, ShouldMatchers }

/**
 *
 * Author: c-birchall
 * Date:   13/11/07
 */
class MemcachedKeySanitizerSpec extends FlatSpec with ShouldMatchers {
  behavior of "MemcachedKeySanitizer"

  val sanitizer = new MemcachedKeySanitizer(maxKeyLength = 10)

  it should "truncate keys from the left if they are longer than maxKeyLength" in {
    val longKey = "0123456789A"
    sanitizer.toValidMemcachedKey(longKey) should be("123456789A")
  }

  it should "replace invalid chars with underscores" in {
    val invalidKey = "a b c"
    sanitizer.toValidMemcachedKey(invalidKey) should be("a_b_c")
  }

  it should "allow symbols in key" in {
    val validKey = "~`!@$"
    sanitizer.toValidMemcachedKey(validKey) should be(validKey)
  }

}

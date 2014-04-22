package scalacache

import org.scalatest.{ ShouldMatchers, FlatSpec }

/**
 *
 * Author: c-birchall
 * Date:   2014/04/22
 */
class DefaultCacheKeyGeneratorTest extends FlatSpec with ShouldMatchers {

  behavior of "Default cache key generator"

  it should "Separate the parts using the configured separator" in {
    implicit val config = CacheConfig(keySeparator = "_")
    DefaultCacheKeyGenerator.toCacheKey("abc", 123) should be("abc_123")
  }

  it should "Prepend the key prefix if one is configured" in {
    implicit val config = CacheConfig(keyPrefix = Some("foo"))
    DefaultCacheKeyGenerator.toCacheKey("abc", 123) should be("foo:abc:123")
  }

}

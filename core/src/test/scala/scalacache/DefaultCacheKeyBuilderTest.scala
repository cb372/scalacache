package scalacache

import org.scalatest.{ ShouldMatchers, FlatSpec }

/**
 *
 * Author: c-birchall
 * Date:   2014/04/22
 */
class DefaultCacheKeyBuilderTest extends FlatSpec with ShouldMatchers {

  behavior of "Default cache key generator"

  it should "Use a single string as-is" in {
    DefaultCacheKeyBuilder.toCacheKey(Seq("abc"))(CacheConfig()) should be("abc")
  }

  it should "Separate the parts using the configured separator" in {
    DefaultCacheKeyBuilder.toCacheKey(Seq("abc", 123))(CacheConfig(keySeparator = "_")) should be("abc_123")
  }

  it should "Prepend the key prefix if one is configured" in {
    DefaultCacheKeyBuilder.toCacheKey(Seq("abc", 123))(CacheConfig(keyPrefix = Some("foo"))) should be("foo:abc:123")
  }

}

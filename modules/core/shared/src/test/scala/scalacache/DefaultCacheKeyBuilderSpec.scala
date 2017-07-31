package scalacache

import org.scalatest.{ Matchers, FlatSpec }

class DefaultCacheKeyBuilderSpec extends FlatSpec with Matchers {

  behavior of "Default cache key builder"

  it should "Use a single string as-is" in {
    DefaultCacheKeyBuilder.toCacheKey(Seq("abc"))(CacheConfig()) should be("abc")
  }

  it should "Separate the parts using the configured separator" in {
    DefaultCacheKeyBuilder.toCacheKey(Seq("abc", 123))(CacheConfig(keySeparator = "_")) should be("abc_123")
  }

  it should "Prepend the key prefix if one is configured" in {
    DefaultCacheKeyBuilder.toCacheKey(Seq("abc", 123))(CacheConfig(keyPrefix = Some("foo"))) should be("foo:abc:123")
  }

  it should "Prepend the key prefix to a single string if one is configured" in {
    DefaultCacheKeyBuilder.stringToCacheKey("abc")(CacheConfig(keyPrefix = Some("foo"))) should be("foo:abc")
  }

}
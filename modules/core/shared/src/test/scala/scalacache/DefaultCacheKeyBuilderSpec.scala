package scalacache

import org.scalatest.{Matchers, FlatSpec}

class DefaultCacheKeyBuilderSpec extends FlatSpec with Matchers {

  behavior of "Default cache key builder"

  it should "Use a single string as-is" in {
    DefaultCacheKeyBuilder().toCacheKey(Seq("abc")) should be("abc")
  }

  it should "Separate the parts using the configured separator" in {
    DefaultCacheKeyBuilder(separator = "_").toCacheKey(Seq("abc", 123)) should be("abc_123")
  }

  it should "Prepend the key prefix if one is configured" in {
    DefaultCacheKeyBuilder(keyPrefix = Some("foo")).toCacheKey(Seq("abc", 123)) should be("foo:abc:123")
  }

  it should "Prepend the key prefix to a single string if one is configured" in {
    DefaultCacheKeyBuilder(keyPrefix = Some("foo")).stringToCacheKey("abc") should be("foo:abc")
  }

}

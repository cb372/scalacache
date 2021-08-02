package scalacache

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class DefaultCacheKeyBuilderSpec extends AnyFlatSpec with Matchers {

  behavior of "Default cache key builder"

  it should "Prepend the key prefix to a single string if one is configured" in {
    DefaultCacheKeyBuilder(keyPrefix = Some("foo")).stringToCacheKey("abc") should be("foo:abc")
  }

}

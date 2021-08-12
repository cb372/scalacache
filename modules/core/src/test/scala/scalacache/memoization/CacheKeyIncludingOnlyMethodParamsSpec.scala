package scalacache.memoization

import org.scalatest.flatspec.AnyFlatSpec
import scalacache.memoization.MethodCallToStringConverter.onlyMethodParams

class CacheKeyIncludingOnlyMethodParamsSpec extends AnyFlatSpec with CacheKeySpecCommon {

  behavior of "cache key generation for method memoization (only including method params in cache key)"

  override implicit lazy val config: MemoizationConfig = MemoizationConfig(toStringConverter = onlyMethodParams)

  it should "include values of all arguments for all argument lists" in {
    checkCacheKey("(1, 2)(3, 4)") {
      multipleArgLists(1, "2")("3", 4)
    }
  }

}

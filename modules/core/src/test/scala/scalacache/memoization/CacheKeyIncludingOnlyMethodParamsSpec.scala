package scalacache.memoization

import org.scalatest._

import scalacache._
import scalacache.memoization.MethodCallToStringConverter.onlyMethodParams
import org.scalatest.flatspec.AnyFlatSpec

class CacheKeyIncludingOnlyMethodParamsSpec extends AnyFlatSpec with CacheKeySpecCommon {

  behavior of "cache key generation for method memoization (only including method params in cache key)"

  implicit val config: MemoizationConfig = MemoizationConfig(toStringConverter = onlyMethodParams)

  it should "include values of all arguments for all argument lists" in {
    checkCacheKey("(1, 2)(3, 4)") {
      multipleArgLists(1, "2")("3", 4)
    }
  }

}

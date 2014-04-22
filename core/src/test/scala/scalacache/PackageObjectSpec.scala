package scalacache

import org.scalatest.{ BeforeAndAfter, FlatSpec, ShouldMatchers }
import scala.concurrent.duration._
import scala.language.postfixOps

/**
 *
 * Author: c-birchall
 * Date:   2014/04/22
 */
class PackageObjectSpec extends FlatSpec with ShouldMatchers with BeforeAndAfter {

  val cache = new LoggingMockCache
  implicit val cacheConfig = CacheConfig(cache)

  before {
    cache.mmap.clear()
    cache.reset()
  }

  behavior of "get"

  it should "call get on the cache found in the CacheConfig" in {
    scalacache.get("foo")
    cache.getCalledWithArgs(0) should be("foo")
  }

  behavior of "put"

  it should "call put on the underlying cache" in {
    scalacache.put("foo", "bar", Some(1 second))
    cache.putCalledWithArgs(0) should be(("foo", "bar", Some(1 second)))
  }

  behavior of "remove"

  it should "call get on the cache found in the CacheConfig" in {
    scalacache.remove("baz")
    cache.removeCalledWithArgs(0) should be("baz")
  }

  behavior of "withCaching"

  it should "run the block and cache its result if the value is not found in the cache" in {
    var called = false
    val result = scalacache.withCaching("myKey") {
      called = true
      "result of block"
    }

    cache.getCalledWithArgs(0) should be("myKey")
    called should be(true)
    cache.putCalledWithArgs(0) should be("myKey", "result of block", None)
    result should be("result of block")
  }

  it should "not run the block if the value is found in the cache" in {
    cache.mmap.put("myKey", "value from cache")

    var called = false
    val result = scalacache.withCaching("myKey") {
      called = true
      "result of block"
    }

    cache.getCalledWithArgs(0) should be("myKey")
    called should be(false)
    cache.putCalledWithArgs.size should be(0)
    result should be("value from cache")
  }

}

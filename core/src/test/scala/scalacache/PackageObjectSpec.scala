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
  implicit val scalaCache = ScalaCache(cache)

  before {
    cache.mmap.clear()
    cache.reset()
  }

  behavior of "#get"

  it should "call get on the cache found in the ScalaCache" in {
    scalacache.get("foo")
    cache.getCalledWithArgs(0) should be("foo")
  }

  it should "use the CacheKeyBuilder to build the cache key" in {
    scalacache.get("foo", 123)
    cache.getCalledWithArgs(0) should be("foo:123")
  }

  behavior of "#put"

  it should "call put on the underlying cache" in {
    scalacache.put("foo")("bar", Some(1 second))
    cache.putCalledWithArgs(0) should be(("foo", "bar", Some(1 second)))
  }

  behavior of "#remove"

  it should "call get on the cache found in the ScalaCache" in {
    scalacache.remove("baz")
    cache.removeCalledWithArgs(0) should be("baz")
  }

  behavior of "#caching"

  it should "run the block and cache its result with no TTL if the value is not found in the cache" in {
    var called = false
    val result = scalacache.caching("myKey") {
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
    val result = scalacache.caching("myKey") {
      called = true
      "result of block"
    }

    cache.getCalledWithArgs(0) should be("myKey")
    called should be(false)
    cache.putCalledWithArgs.size should be(0)
    result should be("value from cache")
  }

  behavior of "#cachingWithTTL"

  it should "run the block and cache its result with the given TTL if the value is not found in the cache" in {
    var called = false
    val result = scalacache.cachingWithTTL("myKey")(10.seconds) {
      called = true
      "result of block"
    }

    cache.getCalledWithArgs(0) should be("myKey")
    called should be(true)
    cache.putCalledWithArgs(0) should be("myKey", "result of block", Some(10.seconds))
    result should be("result of block")
  }

}

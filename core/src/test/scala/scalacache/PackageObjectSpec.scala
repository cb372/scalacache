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

}

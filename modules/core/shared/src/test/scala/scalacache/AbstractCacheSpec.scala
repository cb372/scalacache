package scalacache

import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

import scala.concurrent.duration._
import scala.language.postfixOps

import scala.util.{Success, Try}
import cats.effect.SyncIO
import cats.implicits._

class AbstractCacheSpec extends FlatSpec with Matchers with BeforeAndAfter {

  val cache = new LoggingMockCache[SyncIO, String]

  before {
    cache.mmap.clear()
    cache.reset.unsafeRunSync()
  }

  behavior of "#get"

  it should "call doGet on the concrete cache" in {
    cache.get("foo").unsafeRunSync()
    cache.getCalledWithArgs(0) should be("foo")
  }

  it should "use the CacheKeyBuilder to build the cache key" in {
    cache.get("foo", 123).unsafeRunSync()
    cache.getCalledWithArgs(0) should be("foo:123")
  }

  it should "not call doGet on the concrete cache if cache reads are disabled" in {
    implicit val flags: Flags = Flags(readsEnabled = false)
    cache.get("foo").unsafeRunSync()
    cache.getCalledWithArgs should be(empty)
  }

  it should "conditionally call doGet on the concrete cache depending on the readsEnabled flag" in {
    def possiblyGetFromCache(key: String): Unit = {
      implicit def flags: Flags = Flags(readsEnabled = (key == "foo"))
      cache.get(key).unsafeRunSync()
    }
    possiblyGetFromCache("foo")
    possiblyGetFromCache("bar")
    cache.getCalledWithArgs.size should be(1)
    cache.getCalledWithArgs(0) should be("foo")
  }

  behavior of "#put"

  it should "call doPut on the concrete cache" in {
    cache.put("foo")("bar", Some(1 second)).unsafeRunSync()
    cache.putCalledWithArgs(0) should be(("foo", "bar", Some(1 second)))
  }

  it should "not call doPut on the concrete cache if cache writes are disabled" in {
    implicit val flags: Flags = Flags(writesEnabled = false)
    cache.put("foo")("bar", Some(1 second)).unsafeRunSync()
    cache.putCalledWithArgs should be(empty)
  }

  it should "call doPut with no TTL if the provided TTL is not finite" in {
    cache.put("foo")("bar", Some(Duration.Inf)).unsafeRunSync()
    cache.putCalledWithArgs(0) should be(("foo", "bar", None))
  }

  behavior of "#remove"

  it should "call doRemove on the concrete cache" in {
    cache.remove("baz").unsafeRunSync()
    cache.removeCalledWithArgs(0) should be("baz")
  }

  it should "concatenate key parts correctly" in {
    cache.remove("hey", "yeah").unsafeRunSync()
    cache.removeCalledWithArgs(0) should be("hey:yeah")
  }

  behavior of "#caching"

  it should "run the block and cache its result with no TTL if the value is not found in the cache" in {
    var called = false
    val result = cache
      .caching("myKey")(None) {
        called = true
        "result of block"
      }
      .unsafeRunSync()

    cache.getCalledWithArgs(0) should be("myKey")
    cache.putCalledWithArgs(0) should be("myKey", "result of block", None)
    called should be(true)
    result should be("result of block")
  }

  it should "run the block and cache its result with a TTL if the value is not found in the cache" in {
    var called = false
    val result = cache
      .caching("myKey")(Some(5 seconds)) {
        called = true
        "result of block"
      }
      .unsafeRunSync()

    cache.getCalledWithArgs(0) should be("myKey")
    cache.putCalledWithArgs(0) should be("myKey", "result of block", Some(5 seconds))
    called should be(true)
    result should be("result of block")
  }

  it should "not run the block if the value is found in the cache" in {
    cache.mmap.put("myKey", "value from cache")

    var called = false
    val result = cache
      .caching("myKey")(None) {
        called = true
        "result of block"
      }
      .unsafeRunSync()

    cache.getCalledWithArgs(0) should be("myKey")
    called should be(false)
    result should be("value from cache")
  }

  behavior of "#cachingF (Scala Try mode)"

  it should "run the block and cache its result with no TTL if the value is not found in the cache" in {

    var called = false
    val tResult = cache
      .cachingF("myKey")(None) {
        SyncIO {
          called = true
          "result of block"
        }
      }
      .unsafeRunSync()

    cache.getCalledWithArgs(0) should be("myKey")
    cache.putCalledWithArgs(0) should be("myKey", "result of block", None)
    called should be(true)
    tResult should be("result of block")
  }

  it should "not run the block if the value is found in the cache" in {

    cache.mmap.put("myKey", "value from cache")

    var called = false
    val tResult = cache
      .cachingF("myKey")(None) {
        SyncIO {
          called = true
          "result of block"
        }
      }
      .unsafeRunSync()

    cache.getCalledWithArgs(0) should be("myKey")
    called should be(false)
    tResult should be("value from cache")
  }

  behavior of "#caching (sync mode)"

  it should "run the block and cache its result if the value is not found in the cache" in {
    var called = false
    val result = cache
      .caching("myKey")(None) {
        called = true
        "result of block"
      }
      .unsafeRunSync()

    cache.getCalledWithArgs(0) should be("myKey")
    cache.putCalledWithArgs(0) should be("myKey", "result of block", None)
    called should be(true)
    result should be("result of block")
  }

  it should "not run the block if the value is found in the cache" in {
    cache.mmap.put("myKey", "value from cache")

    var called = false
    val result = cache
      .caching("myKey")(None) {
        called = true
        "result of block"
      }
      .unsafeRunSync()

    cache.getCalledWithArgs(0) should be("myKey")
    called should be(false)
    result should be("value from cache")
  }

  behavior of "#caching and flags"

  it should "run the block and cache its result if cache reads are disabled" in {
    cache.mmap.put("myKey", "value from cache")

    implicit val flags: Flags = Flags(readsEnabled = false)

    var called = false
    val result = cache
      .caching("myKey")(None) {
        called = true
        "result of block"
      }
      .unsafeRunSync()

    cache.getCalledWithArgs should be(empty)
    called should be(true)
    result should be("result of block")

    cache.putCalledWithArgs(0) should be("myKey", "result of block", None)
  }

  it should "run the block but not cache its result if cache writes are disabled" in {
    implicit val flags: Flags = Flags(writesEnabled = false)

    var called = false
    val result = cache
      .caching("myKey")(None) {
        called = true
        "result of block"
      }
      .unsafeRunSync()

    cache.getCalledWithArgs(0) should be("myKey")
    called should be(true)
    cache.putCalledWithArgs should be(empty)
    result should be("result of block")
  }

  behavior of "#cachingF and flags"

  it should "run the block and cache its result if cache reads are disabled" in {
    cache.mmap.put("myKey", "value from cache")

    implicit val flags: Flags = Flags(readsEnabled = false)

    var called = false
    val result = cache
      .cachingF("myKey")(None) {
        SyncIO { called = true } *>
          SyncIO("result of block")
      }
      .unsafeRunSync()

    cache.getCalledWithArgs should be(empty)
    called should be(true)
    result should be("result of block")

    cache.putCalledWithArgs(0) should be("myKey", "result of block", None)
  }

  it should "run the block but not cache its result if cache writes are disabled" in {
    implicit val flags: Flags = Flags(writesEnabled = false)

    var called = false
    val result = cache
      .cachingF("myKey")(None) {
        SyncIO { called = true } *>
          SyncIO("result of block")
      }
      .unsafeRunSync()

    cache.getCalledWithArgs(0) should be("myKey")
    called should be(true)
    cache.putCalledWithArgs should be(empty)
    result should be("result of block")
  }

}

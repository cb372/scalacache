package scalacache

import org.scalatest.concurrent.{ Eventually, IntegrationPatience, ScalaFutures }
import org.scalatest.{ BeforeAndAfter, FlatSpec, Matchers }

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps
import scalacache.serialization.{ Codec, InMemoryRepr }

class PackageObjectSpec extends FlatSpec with Matchers with BeforeAndAfter with ScalaFutures with Eventually with IntegrationPatience {

  val cache = new LoggingMockCache
  implicit val scalaCache = ScalaCache(cache)

  before {
    cache.mmap.clear()
    cache.reset()
  }

  behavior of "#get"

  it should "call get on the cache found in the ScalaCache" in {
    scalacache.get[String, InMemoryRepr]("foo")
    cache.getCalledWithArgs(0) should be("foo")
  }

  it should "use the CacheKeyBuilder to build the cache key" in {
    scalacache.get[String, InMemoryRepr]("foo", 123)
    cache.getCalledWithArgs(0) should be("foo:123")
  }

  it should "not call get on the cache found in the ScalaCache if cache reads are disabled" in {
    implicit val flags = Flags(readsEnabled = false)
    scalacache.get[String, InMemoryRepr]("foo")
    cache.getCalledWithArgs should be('empty)
  }

  it should "conditionally call get on the cache found in the ScalaCache depending on the readsEnabled flag" in {
    def possiblyGetFromCache(key: String): Unit = {
      implicit def flags = Flags(readsEnabled = (key == "foo"))
      scalacache.get[String, InMemoryRepr](key)
    }
    possiblyGetFromCache("foo")
    possiblyGetFromCache("bar")
    cache.getCalledWithArgs.size should be(1)
    cache.getCalledWithArgs(0) should be("foo")
  }

  behavior of "#put"

  it should "call put on the underlying cache" in {
    scalacache.put("foo")("bar", Some(1 second))
    cache.putCalledWithArgs(0) should be(("foo", "bar", Some(1 second)))
  }

  it should "not call put on the underlying cache if cache writes are disabled" in {
    implicit val flags = Flags(writesEnabled = false)
    scalacache.put("foo")("bar", Some(1 second))
    cache.putCalledWithArgs should be('empty)
  }

  it should "call put with no TTL if the provided TTL is not finite" in {
    scalacache.put("foo")("bar", Some(Duration.Inf))
    cache.putCalledWithArgs(0) should be(("foo", "bar", None))
  }

  behavior of "#remove"

  it should "call get on the cache found in the ScalaCache" in {
    scalacache.remove("baz")
    cache.removeCalledWithArgs(0) should be("baz")
  }

  it should "concatenate key parts correctly" in {
    scalacache.remove("hey", "yeah")
    cache.removeCalledWithArgs(0) should be("hey:yeah")
  }

  behavior of "typed.remove"

  it should "concatenate key parts correctly" in {
    scalacache.typed[String, InMemoryRepr].remove("oh", "wow")
    cache.removeCalledWithArgs(0) should be("oh:wow")
  }

  behavior of "#caching"

  it should "run the block and cache its result with no TTL if the value is not found in the cache" in {
    var called = false
    val fResult = scalacache.caching("myKey") {
      Future {
        called = true
        "result of block"
      }
    }

    whenReady(fResult) { result =>
      cache.getCalledWithArgs(0) should be("myKey")
      cache.putCalledWithArgs(0) should be("myKey", "result of block", None)
      called should be(true)
      result should be("result of block")
    }
  }

  it should "perform the cache write asynchronously if ScalaCache is thus configured" in {
    val cache = new LoggingMockCache {
      override def put[V](key: String, value: V, ttl: Option[Duration])(implicit codec: Codec[V, NoSerialization]): Future[Unit] = {
        Thread.sleep(2000L)
        super.put(key, value, ttl)
      }
    }
    implicit val scalaCache = ScalaCache(cache, CacheConfig(waitForWriteToComplete = false))

    var called = false
    val fResult = scalacache.caching("myKey") {
      Future {
        called = true
        "result of block"
      }
    }

    whenReady(fResult) { result =>
      cache.getCalledWithArgs(0) should be("myKey")
      cache.putCalledWithArgs shouldBe empty
      called should be(true)
      result should be("result of block")

      eventually {
        cache.putCalledWithArgs(0) should be("myKey", "result of block", None)
      }
    }
  }

  it should "not run the block if the value is found in the cache" in {
    cache.mmap.put("myKey", "value from cache")

    var called = false
    val fResult = scalacache.caching("myKey") {
      Future {
        called = true
        "result of block"
      }
    }

    whenReady(fResult) { result =>
      cache.getCalledWithArgs(0) should be("myKey")
      called should be(false)
      result should be("value from cache")
    }
  }

  behavior of "#cachingWithTTL"

  it should "run the block and cache its result asynchronously with the given TTL if the value is not found in the cache" in {
    var called = false
    val fResult = scalacache.cachingWithTTL("myKey")(10.seconds) {
      Future {
        called = true
        "result of block"
      }
    }

    whenReady(fResult) { result =>
      cache.getCalledWithArgs(0) should be("myKey")
      called should be(true)
      result should be("result of block")

      eventually {
        cache.putCalledWithArgs(0) should be("myKey", "result of block", Some(10.seconds))
      }
    }
  }

  behavior of "sync.caching"

  it should "run the block and cache its result with no TTL if the value is not found in the cache" in {
    var called = false
    val result = scalacache.sync.caching("myKey") {
      called = true
      "result of block"
    }

    cache.getCalledWithArgs(0) should be("myKey")
    called should be(true)
    result should be("result of block")

    eventually {
      cache.putCalledWithArgs(0) should be("myKey", "result of block", None)
    }
  }

  it should "not run the block if the value is found in the cache" in {
    cache.mmap.put("myKey", "value from cache")

    var called = false
    val result = scalacache.sync.caching("myKey") {
      called = true
      "result of block"
    }

    cache.getCalledWithArgs(0) should be("myKey")
    called should be(false)
    cache.putCalledWithArgs.size should be(0)
    result should be("value from cache")
  }

  it should "run the block and cache its result with no TTL if cache reads are disabled" in {
    cache.mmap.put("myKey", "value from cache")
    implicit val flags = Flags(readsEnabled = false)

    var called = false
    val result = scalacache.sync.caching("myKey") {
      called = true
      "result of block"
    }

    cache.getCalledWithArgs should be('empty)
    called should be(true)
    result should be("result of block")

    eventually {
      cache.putCalledWithArgs(0) should be("myKey", "result of block", None)
    }
  }

  it should "run the block but not cache its result if cache writes are disabled" in {
    implicit val flags = Flags(writesEnabled = false)

    var called = false
    val result = scalacache.sync.caching("myKey") {
      called = true
      "result of block"
    }

    cache.getCalledWithArgs(0) should be("myKey")
    called should be(true)
    cache.putCalledWithArgs should be('empty)
    result should be("result of block")
  }

  behavior of "sync.cachingWithTTL"

  it should "run the block and cache its result with the given TTL if the value is not found in the cache" in {
    var called = false
    val result = scalacache.sync.cachingWithTTL("myKey")(10.seconds) {
      called = true
      "result of block"
    }

    cache.getCalledWithArgs(0) should be("myKey")
    called should be(true)
    result should be("result of block")

    eventually {
      cache.putCalledWithArgs(0) should be("myKey", "result of block", Some(10.seconds))
    }
  }

  it should "run the block and cache its result with the given TTL if cache reads are disabled" in {
    cache.mmap.put("myKey", "value from cache")
    implicit val flags = Flags(readsEnabled = false)

    var called = false
    val result = scalacache.sync.cachingWithTTL("myKey")(10.seconds) {
      called = true
      "result of block"
    }

    cache.getCalledWithArgs should be('empty)
    called should be(true)
    result should be("result of block")

    eventually {
      cache.putCalledWithArgs(0) should be("myKey", "result of block", Some(10.seconds))
    }
  }

  it should "run the block but not cache its result if cache writes are disabled" in {
    implicit val flags = Flags(writesEnabled = false)

    var called = false
    val result = scalacache.sync.cachingWithTTL("myKey")(10.seconds) {
      called = true
      "result of block"
    }

    cache.getCalledWithArgs(0) should be("myKey")
    called should be(true)
    cache.putCalledWithArgs should be('empty)
    result should be("result of block")
  }

}

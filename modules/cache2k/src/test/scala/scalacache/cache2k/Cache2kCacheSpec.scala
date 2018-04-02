package scalacache.cache2k

import java.time.{Clock, Instant, ZoneOffset}

import org.cache2k.Cache2kBuilder
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

import scala.concurrent.duration._
import scalacache._

class Cache2kCacheSpec extends FlatSpec with Matchers with BeforeAndAfter with ScalaFutures {

  private def newCCache = Cache2kBuilder.of(classOf[String], classOf[Entry[String]]).build

  import scalacache.modes.sync._

  behavior of "get"

  it should "return the value stored in the underlying cache" in {
    val underlying = newCCache
    val entry = Entry("hello", expiresAt = None)
    underlying.put("key1", entry)
    Cache2kCache(underlying).get("key1") should be(Some("hello"))
  }

  it should "return None if the given key does not exist in the underlying cache" in {
    val underlying = newCCache
    Cache2kCache(underlying).get("non-existent key") should be(None)
  }

  it should "return None if the given key exists but the value has expired" in {
    val underlying = newCCache
    val expiredEntry =
      Entry("hello", expiresAt = Some(Instant.now.minusSeconds(1)))
    underlying.put("key1", expiredEntry)
    Cache2kCache(underlying).get("key1") should be(None)
  }

  behavior of "put"

  it should "store the given key-value pair in the underlying cache with no TTL" in {
    val underlying = newCCache
    Cache2kCache(underlying).put("key1")("hello", None)
    underlying.peek("key1") should be(Entry("hello", None))
  }

  behavior of "put with TTL"

  it should "store the given key-value pair in the underlying cache with the given TTL" in {
    val now = Instant.now()
    val clock = Clock.fixed(now, ZoneOffset.UTC)

    val underlying = newCCache
    new Cache2kCache(underlying)(implicitly[CacheConfig], clock).put("key1")("hello", Some(10.seconds))
    underlying.peek("key1") should be(Entry("hello", expiresAt = Some(now.plusSeconds(10))))
  }

  it should "support a TTL greater than Int.MaxValue millis" in {
    val now = Instant.parse("2015-10-01T00:00:00Z")
    val clock = Clock.fixed(now, ZoneOffset.UTC)

    val underlying = newCCache
    new Cache2kCache(underlying)(implicitly[CacheConfig], clock).put("key1")("hello", Some(30.days))
    underlying.peek("key1") should be(Entry("hello", expiresAt = Some(Instant.parse("2015-10-31T00:00:00Z"))))
  }

  behavior of "remove"

  it should "delete the given key and its value from the underlying cache" in {
    val underlying = newCCache
    val entry = Entry("hello", expiresAt = None)
    underlying.put("key1", entry)
    underlying.peek("key1") should be(entry)

    Cache2kCache(underlying).remove("key1")
    underlying.peek("key1") should be(null)
  }

}

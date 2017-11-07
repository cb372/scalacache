package scalacache.guava

import java.time.{Clock, Instant, ZoneOffset}

import scalacache._
import org.scalatest.{FlatSpec, Matchers}
import com.google.common.cache.CacheBuilder

import scala.concurrent.duration._
import org.scalatest.concurrent.ScalaFutures

class GuavaCacheSpec extends FlatSpec with Matchers with ScalaFutures {

  private def newGCache = CacheBuilder.newBuilder.build[String, Entry[String]]

  import scalacache.modes.sync._

  behavior of "get"

  it should "return the value stored in the underlying cache" in {
    val underlying = newGCache
    val entry = Entry("hello", expiresAt = None)
    underlying.put("key1", entry)
    GuavaCache(underlying).get("key1") should be(Some("hello"))
  }

  it should "return None if the given key does not exist in the underlying cache" in {
    val underlying = newGCache
    GuavaCache(underlying).get("non-existent key") should be(None)
  }

  it should "return None if the given key exists but the value has expired" in {
    val underlying = newGCache
    val expiredEntry =
      Entry("hello", expiresAt = Some(Instant.now.minusSeconds(1)))
    underlying.put("key1", expiredEntry)
    GuavaCache(underlying).get("non-existent key") should be(None)
  }

  behavior of "put"

  it should "store the given key-value pair in the underlying cache with no TTL" in {
    val underlying = newGCache
    GuavaCache(underlying).put("key1")("hello", None)
    underlying.getIfPresent("key1") should be(Entry("hello", None))
  }

  behavior of "put with TTL"

  it should "store the given key-value pair in the underlying cache with the given TTL" in {
    val now = Instant.now()
    val clock = Clock.fixed(now, ZoneOffset.UTC)

    val underlying = newGCache
    new GuavaCache(underlying)(implicitly[CacheConfig], clock).put("key1")("hello", Some(10.seconds))
    underlying.getIfPresent("key1") should be(Entry("hello", expiresAt = Some(Instant.from(now.plusSeconds(10)))))
  }

  it should "support a TTL greater than Int.MaxValue millis" in {
    val now = Instant.parse("2015-10-01T00:00:00Z")
    val clock = Clock.fixed(now, ZoneOffset.UTC)

    val underlying = newGCache
    new GuavaCache(underlying)(implicitly[CacheConfig], clock).put("key1")("hello", Some(30.days))
    underlying.getIfPresent("key1") should be(Entry("hello", expiresAt = Some(Instant.parse("2015-10-31T00:00:00Z"))))
  }

  behavior of "remove"

  it should "delete the given key and its value from the underlying cache" in {
    val underlying = newGCache
    val entry = Entry("hello", expiresAt = None)
    underlying.put("key1", entry)
    underlying.getIfPresent("key1") should be(entry)

    GuavaCache(underlying).remove("key1")
    underlying.getIfPresent("key1") should be(null)
  }

}

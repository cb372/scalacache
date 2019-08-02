package scalacache.mapdb

import java.time.{Clock, Instant, ZoneOffset}

import org.mapdb.HTreeMap
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}
import scalacache._

import scala.concurrent.duration._

class MapDBCacheSpec extends FlatSpec with Matchers with ScalaFutures {

  private def newCache = MapDBCache.db.hashMap("map").createOrOpen.asInstanceOf[HTreeMap[String, Entry[String]]]

  import scalacache.modes.sync._

  behavior of "get"

  it should "return the value stored in the underlying cache" in {
    val underlying = newCache
    val entry      = Entry("hello", expiresAt = None)
    underlying.put("key1", entry)
    MapDBCache(underlying).get("key1") should be(Some("hello"))
  }

  it should "return None if the given key does not exist in the underlying cache" in {
    val underlying = newCache
    MapDBCache(underlying).get("non-existent key") should be(None)
  }

  it should "return None if the given key exists but the value has expired" in {
    val underlying = newCache
    val expiredEntry =
      Entry("hello", expiresAt = Some(Instant.now.minusSeconds(1)))
    underlying.put("key1", expiredEntry)
    MapDBCache(underlying).get("non-existent key") should be(None)
  }

  behavior of "put"

  it should "store the given key-value pair in the underlying cache with no TTL" in {
    val underlying = newCache
    new MapDBCache(underlying).put("key1")("hello", None)
    underlying.get("key1") should be(Entry("hello", None))
  }

  behavior of "put with TTL"

  it should "store the given key-value pair in the underlying cache with the given TTL" in {
    val now   = Instant.now()
    val clock = Clock.fixed(now, ZoneOffset.UTC)

    val underlying = newCache
    new MapDBCache(underlying)(implicitly[CacheConfig], clock).put("key1")("hello", Some(10.seconds))
    underlying.get("key1") should be(Entry("hello", expiresAt = Some(Instant.from(now.plusSeconds(10)))))
  }

  it should "support a TTL greater than Int.MaxValue millis" in {
    val now   = Instant.parse("2015-10-01T00:00:00Z")
    val clock = Clock.fixed(now, ZoneOffset.UTC)

    val underlying = newCache
    new MapDBCache(underlying)(implicitly[CacheConfig], clock).put("key1")("hello", Some(30.days))
    underlying.get("key1") should be(Entry("hello", expiresAt = Some(Instant.parse("2015-10-31T00:00:00Z"))))
  }

  behavior of "remove"

  it should "delete the given key and its value from the underlying cache" in {
    val underlying = newCache
    val entry      = Entry("hello", expiresAt = None)
    underlying.put("key1", entry)
    underlying.get("key1") should be(entry)

    MapDBCache(underlying).remove("key1")
    underlying.get("key1") should be(null)
  }

}

package scalacache.cache2k

import java.time.Instant

import org.cache2k.Cache2kBuilder
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

import scala.concurrent.duration._
import scalacache._

class Cache2kCacheSpec extends FlatSpec with Matchers with BeforeAndAfter with ScalaFutures {

  private def newCCache =
    new Cache2kBuilder[String, String]() {}
      .expireAfterWrite(1, DAYS)
      .build

  import scalacache.modes.sync._

  behavior of "get"

  it should "return the value stored in the underlying cache" in {
    val underlying = newCCache
    underlying.put("key1", "hello")
    Cache2kCache(underlying).get("key1") should be(Some("hello"))
  }

  it should "return None if the given key does not exist in the underlying cache" in {
    val underlying = newCCache
    Cache2kCache(underlying).get("non-existent key") should be(None)
  }

  it should "return None if the given key has expired" in {
    val underlying = newCCache
    underlying.put("key1", "hello")
    underlying.expireAt("key1", Instant.now.minusSeconds(1).toEpochMilli)
    Cache2kCache(underlying).get("key1") should be(None)
  }

  behavior of "put"

  it should "store the given key-value pair in the underlying cache with no TTL" in {
    val underlying = newCCache
    Cache2kCache(underlying).put("key1")("hello", None)
    underlying.peek("key1") should be("hello")
  }

  behavior of "put with TTL"

  it should "store the given key-value pair in the underlying cache with the given TTL" in {
    val underlying   = newCCache
    val cache2kCache = new Cache2kCache(underlying)(implicitly[CacheConfig])
    cache2kCache.put("key1")("hello", Some(1.nanosecond))
    Thread.sleep(100)
    underlying.peek("key1") should be(null)
    cache2kCache.put("key2")("hello", Some(1.day))
    underlying.peek("key2") should be("hello")
  }

  behavior of "remove"

  it should "delete the given key and its value from the underlying cache" in {
    val underlying = newCCache
    underlying.put("key1", "hello")
    underlying.peek("key1") should be("hello")

    Cache2kCache(underlying).remove("key1")
    underlying.peek("key1") should be(null)
  }

}

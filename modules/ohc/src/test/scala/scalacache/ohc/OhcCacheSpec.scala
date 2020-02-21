package scalacache.ohc

import java.time.Instant

import org.caffinitas.ohc.{OHCache, OHCacheBuilder}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.BeforeAndAfter

import scala.concurrent.duration._
import scalacache._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class OhcCacheSpec extends AnyFlatSpec with Matchers with BeforeAndAfter with ScalaFutures {

  private def newOHCache: OHCache[String, String] =
    OHCacheBuilder
      .newBuilder()
      .keySerializer(OhcCache.stringSerializer)
      .valueSerializer(OhcCache.stringSerializer)
      .timeouts(true)
      .build()

  import scalacache.modes.sync._

  behavior of "get"

  it should "return the value stored in the underlying cache" in {
    val underlying = newOHCache
    underlying.put("key1", "hello")
    OhcCache(underlying).get("key1") should be(Some("hello"))
    underlying.close()
  }

  it should "return None if the given key does not exist in the underlying cache" in {
    val underlying = newOHCache
    OhcCache(underlying).get("non-existent key") should be(None)
    underlying.close()
  }

  it should "return None if the given key has expired" in {
    val underlying = newOHCache
    underlying.put("key1", "hello", Instant.now.minusSeconds(1).toEpochMilli)
    OhcCache(underlying).get("key1") should be(None)
    underlying.close()
  }

  behavior of "put"

  it should "store the given key-value pair in the underlying cache with no TTL" in {
    val underlying = newOHCache
    OhcCache(underlying).put("key1")("hello", None)
    underlying.get("key1") should be("hello")
    underlying.close()
  }

  behavior of "put with TTL"

  it should "store the given key-value pair in the underlying cache with the given TTL" in {
    val underlying = newOHCache
    val ohcCache   = new OhcCache(underlying)(implicitly[CacheConfig])
    ohcCache.put("key1")("hello", Some(1.nanosecond))
    Thread.sleep(100)
    underlying.get("key1") should be(null)
    ohcCache.put("key2")("hello", Some(1.day))
    underlying.get("key2") should be("hello")
    underlying.close()
  }

  behavior of "remove"

  it should "delete the given key and its value from the underlying cache" in {
    val underlying = newOHCache
    underlying.put("key1", "hello")
    underlying.get("key1") should be("hello")

    OhcCache(underlying).remove("key1")
    underlying.get("key1") should be(null)
    underlying.close()
  }

}

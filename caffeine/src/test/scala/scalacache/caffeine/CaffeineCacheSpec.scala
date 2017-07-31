package scalacache.caffeine

import scalacache.Entry
import org.scalatest.{BeforeAndAfter, Matchers, FlatSpec}
import com.github.benmanes.caffeine.cache.Caffeine
import org.joda.time.{DateTimeUtils, DateTime}
import scala.concurrent.duration._
import org.scalatest.concurrent.ScalaFutures

class CaffeineCacheSpec
    extends FlatSpec
    with Matchers
    with BeforeAndAfter
    with ScalaFutures {

  def newCCache = Caffeine.newBuilder.build[String, Object]

  behavior of "get"

  it should "return the value stored in the underlying cache" in {
    val underlying = newCCache
    val entry = Entry("hello", expiresAt = None)
    underlying.put("key1", entry)
    whenReady(CaffeineCache(underlying).get[String]("key1")) { result =>
      result should be(Some("hello"))
    }
  }

  it should "return None if the given key does not exist in the underlying cache" in {
    val underlying = newCCache
    whenReady(CaffeineCache(underlying).get[String]("non-existent key")) {
      result =>
        result should be(None)
    }
  }

  it should "return None if the given key exists but the value has expired" in {
    val underlying = newCCache
    val expiredEntry =
      Entry("hello", expiresAt = Some(DateTime.now.minusSeconds(1)))
    underlying.put("key1", expiredEntry)
    whenReady(CaffeineCache(underlying).get[String]("non-existent key")) {
      result =>
        result should be(None)
    }
  }

  behavior of "put"

  it should "store the given key-value pair in the underlying cache with no TTL" in {
    val underlying = newCCache
    CaffeineCache(underlying).put("key1", "hello", None)
    underlying.getIfPresent("key1") should be(Entry("hello", None))
  }

  behavior of "put with TTL"

  it should "store the given key-value pair in the underlying cache with the given TTL" in {
    val now = DateTime.now
    DateTimeUtils.setCurrentMillisFixed(now.getMillis)

    val underlying = newCCache
    CaffeineCache(underlying).put("key1", "hello", Some(10.seconds))
    underlying.getIfPresent("key1") should be(
      Entry("hello", expiresAt = Some(now.plusSeconds(10))))
  }

  it should "support a TTL greater than Int.MaxValue millis" in {
    val now = new DateTime("2015-10-01T00:00:00Z")
    DateTimeUtils.setCurrentMillisFixed(now.getMillis)

    val underlying = newCCache
    CaffeineCache(underlying).put("key1", "hello", Some(30.days))
    underlying.getIfPresent("key1") should be(
      Entry("hello", expiresAt = Some(new DateTime("2015-10-31T00:00:00Z"))))
  }

  behavior of "remove"

  it should "delete the given key and its value from the underlying cache" in {
    val underlying = newCCache
    val entry = Entry("hello", expiresAt = None)
    underlying.put("key1", entry)
    underlying.getIfPresent("key1") should be(entry)

    CaffeineCache(underlying).remove("key1")
    underlying.getIfPresent("key1") should be(null)
  }

  after {
    DateTimeUtils.setCurrentMillisSystem()
  }

}

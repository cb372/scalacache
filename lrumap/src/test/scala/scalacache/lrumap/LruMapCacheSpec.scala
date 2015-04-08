package scalacache.lrumap

import scala.concurrent.duration._

import com.twitter.util.LruMap
import org.joda.time.{DateTime, DateTimeUtils}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

class LruMapCacheSpec extends FlatSpec with Matchers with BeforeAndAfter with ScalaFutures {
  def newCache = new LruMap[String, Object](10)

  behavior of "get"

  it should "return the value stored in the underlying cache" in {
    val underlying = newCache
    val entry = LruMapCache.Entry("hello", expiresAt = None)
    underlying.put("key1", entry)
    whenReady(LruMapCache(underlying).get("key1")) { result =>
      result should be(Some("hello"))
    }
  }

  it should "return None if the given key does not exist in the underlying cache" in {
    val underlying = newCache
    whenReady(LruMapCache(underlying).get("non-existent key")) { result =>
      result should be(None)
    }
  }

  it should "return None if the given key exists but the value has expired" in {
    val underlying = newCache
    val expiredEntry = LruMapCache.Entry("hello", expiresAt = Some(DateTime.now.minusSeconds(1)))
    underlying.put("key1", expiredEntry)
    whenReady(LruMapCache(underlying).get("non-existent key")) { result =>
      result should be(None)
    }
  }

  behavior of "put"

  it should "store the given key-value pair in the underlying cache with no TTL" in {
    val underlying = newCache
    LruMapCache(underlying).put("key1", "hello", None)
    underlying.get("key1").get should be(LruMapCache.Entry("hello", None))
  }

  behavior of "put with TTL"

  it should "store the given key-value pair in the underlying cache with the given TTL" in {
    val now = DateTime.now
    DateTimeUtils.setCurrentMillisFixed(now.getMillis)

    val underlying = newCache
    LruMapCache(underlying).put("key1", "hello", Some(10.seconds))
    underlying.get("key1").get should be(LruMapCache.Entry("hello", expiresAt = Some(now.plusSeconds(10))))
  }

  behavior of "remove"

  it should "delete the given key and its value from the underlying cache" in {
    val underlying = newCache
    val entry = LruMapCache.Entry("hello", expiresAt = None)
    underlying.put("key1", entry)
    underlying.get("key1").get should be(entry)

    LruMapCache(underlying).remove("key1")
    underlying.get("key1") should be(None)
  }

  after {
    DateTimeUtils.setCurrentMillisSystem()
  }

}

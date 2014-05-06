package scalacache.guava

import org.scalatest.{ BeforeAndAfter, ShouldMatchers, FlatSpec }
import com.google.common.cache.CacheBuilder
import org.joda.time.{ DateTimeUtils, DateTime }
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import org.scalatest.concurrent.ScalaFutures

/**
 *
 * Author: c-birchall
 * Date:   13/11/07
 */
class GuavaCacheSpec extends FlatSpec with ShouldMatchers with BeforeAndAfter with ScalaFutures {

  def newGCache = CacheBuilder.newBuilder.build[String, Object]

  behavior of "get"

  it should "return the value stored in the underlying cache" in {
    val underlying = newGCache
    val entry = Entry("hello", expiresAt = None)
    underlying.put("key1", entry)
    whenReady(GuavaCache(underlying).get("key1")) { result =>
      result should be(Some("hello"))
    }
  }

  it should "return None if the given key does not exist in the underlying cache" in {
    val underlying = newGCache
    whenReady(GuavaCache(underlying).get("non-existent key")) { result =>
      result should be(None)
    }
  }

  it should "return None if the given key exists but the value has expired" in {
    val underlying = newGCache
    val expiredEntry = Entry("hello", expiresAt = Some(DateTime.now.minusSeconds(1)))
    underlying.put("key1", expiredEntry)
    whenReady(GuavaCache(underlying).get("non-existent key")) { result =>
      result should be(None)
    }
  }

  behavior of "put"

  it should "store the given key-value pair in the underlying cache with no TTL" in {
    val underlying = newGCache
    GuavaCache(underlying).put("key1", "hello", None)
    underlying.getIfPresent("key1") should be(Entry("hello", None))
  }

  behavior of "put with TTL"

  it should "store the given key-value pair in the underlying cache with the given TTL" in {
    val now = DateTime.now
    DateTimeUtils.setCurrentMillisFixed(now.getMillis)

    val underlying = newGCache
    GuavaCache(underlying).put("key1", "hello", Some(10.seconds))
    underlying.getIfPresent("key1") should be(Entry("hello", expiresAt = Some(now.plusSeconds(10))))
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

  after {
    DateTimeUtils.setCurrentMillisSystem()
  }

}

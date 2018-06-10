package scalacache.guava

import java.time.{Clock, Instant, ZoneOffset}

import cats.effect.IO
import com.google.common.cache.CacheBuilder
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}
import scalacache._

import scala.concurrent.duration._

class GuavaCacheSpec extends FlatSpec with Matchers with ScalaFutures {

  import scalacache.CatsEffect.implicits._
  import scalacache.serialization.binary._

  private def newGCache = CacheBuilder.newBuilder.build[String, Entry]
  private def newCacheInstance = GuavaCache[IO](newGCache)

  behavior of "get"

  it should "return the value stored in the underlying cache" in {
    val cache = newCacheInstance
    val entry = Entry("hello", expiresAt = None)
    cache.underlying.put("key1", entry)
    newCacheInstance.get("key1").unsafeRunSync() should be(Some("hello"))
  }

  it should "return None if the given key does not exist in the underlying cache" in {
    newCacheInstance.get("non-existent key").unsafeRunSync() should be(None)
  }

  it should "return None if the given key exists but the value has expired" in {
    val cache = newCacheInstance
    val expiredEntry = Entry("hello", expiresAt = Some(Instant.now.minusSeconds(1)))
    cache.underlying.put("key1", expiredEntry)
    cache.get("non-existent key").unsafeRunSync() should be(None)
  }

  behavior of "put"

  it should "store the given key-value pair in the underlying cache with no TTL" in {
    val cache = newCacheInstance
    cache.put("key1")("hello", None).unsafeRunSync()
    cache.underlying.getIfPresent("key1") should be(Entry("hello", None))
  }

  behavior of "put with TTL"

  it should "store the given key-value pair in the underlying cache with the given TTL" in {
    val now = Instant.now()
    implicit val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)

    val cache = newCacheInstance
    cache.put("key1")("hello", Some(10.seconds)).unsafeRunSync()
    cache.underlying.getIfPresent("key1") should be(Entry("hello", expiresAt = Some(Instant.from(now.plusSeconds(10)))))
  }

  it should "support a TTL greater than Int.MaxValue millis" in {
    val now = Instant.parse("2015-10-01T00:00:00Z")
    implicit val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)

    val cache = newCacheInstance
    cache.put("key1")("hello", Some(30.days)).unsafeRunSync()
    cache.underlying.getIfPresent("key1") should be(
      Entry("hello", expiresAt = Some(Instant.parse("2015-10-31T00:00:00Z"))))
  }

  behavior of "remove"

  it should "delete the given key and its value from the underlying cache" in {
    val cache = newCacheInstance
    val entry = Entry("hello", expiresAt = None)
    cache.underlying.put("key1", entry)
    cache.underlying.getIfPresent("key1") should be(entry)

    cache.remove("key1").unsafeRunSync()
    cache.underlying.getIfPresent("key1") should be(null)
  }

}

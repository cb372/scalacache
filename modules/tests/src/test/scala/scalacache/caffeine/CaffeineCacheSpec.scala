package scalacache.caffeine

import java.time.{Clock, Instant, ZoneOffset}

import cats.effect.IO
import com.github.benmanes.caffeine.cache.Caffeine
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}
import scalacache._

import scala.concurrent.duration._

class CaffeineCacheSpec extends FlatSpec with Matchers with BeforeAndAfter with ScalaFutures {

  import scalacache.CatsEffect.implicits._
  import scalacache.serialization.binary._

  private def newCCache = Caffeine.newBuilder.build[String, Entry]

  behavior of "get"

  it should "return the value stored in the underlying cache" in {
    val underlying = newCCache
    val entry = Entry("hello", expiresAt = None)
    underlying.put("key1", entry)
    CaffeineCache[IO](underlying).get("key1").unsafeRunSync() should be(Some("hello"))
  }

  it should "return None if the given key does not exist in the underlying cache" in {
    val underlying = newCCache
    CaffeineCache[IO](underlying).get("non-existent key").unsafeRunSync() should be(None)
  }

  it should "return None if the given key exists but the value has expired" in {
    val underlying = newCCache
    val expiredEntry =
      Entry("hello", expiresAt = Some(Instant.now.minusSeconds(1)))
    underlying.put("key1", expiredEntry)
    CaffeineCache[IO](underlying).get("key1").unsafeRunSync() should be(None)
  }

  behavior of "put"

  it should "store the given key-value pair in the underlying cache with no TTL" in {
    val underlying = newCCache
    CaffeineCache[IO](underlying).put("key1")("hello", None).unsafeRunSync()
    underlying.getIfPresent("key1") should be(Entry("hello", None))
  }

  behavior of "put with TTL"

  it should "store the given key-value pair in the underlying cache with the given TTL" in {
    val now = Instant.now()
    implicit val clock = Clock.fixed(now, ZoneOffset.UTC)

    val underlying = newCCache
    new CaffeineCache[IO](underlying).put("key1")("hello", Some(10.seconds)).unsafeRunSync()
    underlying.getIfPresent("key1") should be(Entry("hello", expiresAt = Some(now.plusSeconds(10))))
  }

  it should "support a TTL greater than Int.MaxValue millis" in {
    val now = Instant.parse("2015-10-01T00:00:00Z")
    implicit val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)

    val underlying = newCCache
    new CaffeineCache[IO](underlying).put("key1")("hello", Some(30.days)).unsafeRunSync()
    underlying.getIfPresent("key1") should be(Entry("hello", expiresAt = Some(Instant.parse("2015-10-31T00:00:00Z"))))
  }

  behavior of "remove"

  it should "delete the given key and its value from the underlying cache" in {
    val underlying = newCCache
    val entry = Entry("hello", expiresAt = None)
    underlying.put("key1", entry)
    underlying.getIfPresent("key1") should be(entry)

    CaffeineCache[IO](underlying).remove("key1").unsafeRunSync()
    underlying.getIfPresent("key1") should be(null)
  }

}

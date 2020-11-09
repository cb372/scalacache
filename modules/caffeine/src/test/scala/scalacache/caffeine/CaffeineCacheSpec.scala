package scalacache.caffeine

import java.time.{Instant, ZoneOffset}

import scalacache._
import org.scalatest.BeforeAndAfter
import com.github.benmanes.caffeine.cache.Caffeine

import scala.concurrent.duration._
import org.scalatest.concurrent.ScalaFutures
import cats.effect.SyncIO
import cats.effect.Clock
import java.util.concurrent.TimeUnit
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CaffeineCacheSpec extends AnyFlatSpec with Matchers with BeforeAndAfter with ScalaFutures {

  private def newCCache = Caffeine.newBuilder.build[String, Entry[String]]

  val defaultClock = Clock.create[SyncIO]
  def fixedClock(now: Instant): Clock[SyncIO] = new Clock[SyncIO] {
    def realTime(unit: TimeUnit): SyncIO[Long] = SyncIO.pure {
      unit.convert(now.toEpochMilli(), TimeUnit.MILLISECONDS)
    }

    def monotonic(unit: concurrent.duration.TimeUnit): SyncIO[Long] = realTime(unit)

  }

  def newIOCache[V](
      underlying: com.github.benmanes.caffeine.cache.Cache[String, Entry[V]],
      clock: Clock[SyncIO] = defaultClock
  ) = {
    implicit val clockImplicit = clock
    CaffeineCache[SyncIO, V](underlying)
  }

  behavior of "get"

  it should "return the value stored in the underlying cache" in {
    val underlying = newCCache
    val entry      = Entry("hello", expiresAt = None)
    underlying.put("key1", entry)
    newIOCache(underlying).get("key1").unsafeRunSync() should be(Some("hello"))
  }

  it should "return None if the given key does not exist in the underlying cache" in {
    val underlying = newCCache
    newIOCache(underlying).get("non-existent key").unsafeRunSync() should be(None)
  }

  it should "return None if the given key exists but the value has expired" in {
    val underlying = newCCache
    val expiredEntry =
      Entry("hello", expiresAt = Some(Instant.now.minusSeconds(1)))
    underlying.put("key1", expiredEntry)
    newIOCache(underlying).get("key1").unsafeRunSync() should be(None)
  }

  behavior of "put"

  it should "store the given key-value pair in the underlying cache with no TTL" in {
    val underlying = newCCache
    newIOCache(underlying).put("key1")("hello", None).unsafeRunSync()

    underlying.getIfPresent("key1") should be(Entry("hello", None))
  }

  behavior of "put with TTL"

  it should "store the given key-value pair in the underlying cache with the given TTL" in {
    val now   = Instant.parse("2020-05-31T12:00:00Z")
    val clock = fixedClock(now)

    val underlying = newCCache
    newIOCache(underlying, clock).put("key1")("hello", Some(10.seconds)).unsafeRunSync()

    underlying.getIfPresent("key1") should be(Entry("hello", expiresAt = Some(now.plusSeconds(10))))
  }

  it should "support a TTL greater than Int.MaxValue millis" in {
    val now   = Instant.parse("2015-10-01T00:00:00Z")
    val clock = fixedClock(now)

    val underlying = newCCache
    newIOCache(underlying, clock).put("key1")("hello", Some(30.days)).unsafeRunSync()

    underlying.getIfPresent("key1") should be(
      Entry("hello", expiresAt = Some(Instant.parse("2015-10-31T00:00:00Z")))
    )
  }

  behavior of "remove"

  it should "delete the given key and its value from the underlying cache" in {
    val underlying = newCCache
    val entry      = Entry("hello", expiresAt = None)
    underlying.put("key1", entry)
    underlying.getIfPresent("key1") should be(entry)

    newIOCache(underlying).remove("key1").unsafeRunSync()
    underlying.getIfPresent("key1") should be(null)
  }

}

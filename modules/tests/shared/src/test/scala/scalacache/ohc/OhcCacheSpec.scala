package scalacache.ohc

import java.nio.charset.StandardCharsets
import java.time.Instant

import cats.effect.IO
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

import scala.concurrent.duration._
import scala.language.implicitConversions

class OhcCacheSpec extends FlatSpec with Matchers with BeforeAndAfter with ScalaFutures {

  import scalacache.CatsEffect.implicits._
  import scalacache.serialization.binary._

  private def newCacheInstance: OhcCache[IO] = OhcCache[IO]()

  // Ugly but convenient. Do not abuse of that.
  private[this] implicit def getBytes(s: String): Array[Byte] = s.getBytes(StandardCharsets.UTF_8)

  behavior of "get"

  it should "return the value stored in the underlying cache" in {
    val cache = newCacheInstance
    cache.underlying.put("key1", "hello")
    cache.get("key1").unsafeRunSync() should be(Some("hello"))
    cache.underlying.close()
  }

  it should "return None if the given key does not exist in the underlying cache" in {
    val cache = newCacheInstance
    cache.get("non-existent key").unsafeRunSync() should be(None)
    cache.underlying.close()
  }

  it should "return None if the given key has expired" in {
    val cache = newCacheInstance
    cache.underlying.put("key1", "hello", Instant.now.minusSeconds(1).toEpochMilli)
    cache.get("key1").unsafeRunSync() should be(None)
    cache.underlying.close()
  }

  behavior of "put"

  it should "store the given key-value pair in the underlying cache with no TTL" in {
    val cache = newCacheInstance
    cache.put("key1")("hello", None).unsafeRunSync()
    cache.underlying.get("key1") should be("hello")
    cache.underlying.close()
  }

  behavior of "put with TTL"

  it should "store the given key-value pair in the underlying cache with the given TTL" in {
    val cache = newCacheInstance
    cache.put("key1")("hello", Some(1.nanosecond)).unsafeRunSync()
    Thread.sleep(100)
    cache.underlying.get("key1") should be(null)
    cache.put("key2")("hello", Some(1.day)).unsafeRunSync()
    cache.underlying.get("key2") should be("hello")
    cache.underlying.close()
  }

  behavior of "remove"

  it should "delete the given key and its value from the underlying cache" in {
    val cache = newCacheInstance
    cache.underlying.put("key1", "hello")
    cache.underlying.get("key1") should be("hello")

    cache.remove("key1")
    cache.get("key1").unsafeRunSync() should be(null)
    cache.close()
  }

}

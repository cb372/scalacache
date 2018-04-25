package scalacache.ohc

import java.nio.ByteBuffer
import java.time.Instant

import com.google.common.base.Charsets
import org.caffinitas.ohc.{CacheSerializer, OHCache, OHCacheBuilder}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

import scala.concurrent.duration._
import scalacache._

class OhcCacheSpec extends FlatSpec with Matchers with BeforeAndAfter with ScalaFutures {

  private val stringSerializer = new CacheSerializer[String]() {
    def serialize(s: String, buf: ByteBuffer): Unit = {
      val bytes = s.getBytes(Charsets.UTF_8)
      buf.put(((bytes.length >>> 8) & 0xFF).toByte)
      buf.put(((bytes.length >>> 0) & 0xFF).toByte)
      buf.put(bytes)
    }

    def deserialize(buf: ByteBuffer): String = {
      val length = ((buf.get & 0xff) << 8) + ((buf.get & 0xff) << 0)
      val bytes = new Array[Byte](length)
      buf.get(bytes)
      new String(bytes, Charsets.UTF_8)
    }

    def serializedSize(s: String): Int =
      s.getBytes(Charsets.UTF_8).length + 2
  }

  private def newOHCache: OHCache[String, String] =
    OHCacheBuilder
      .newBuilder()
      .keySerializer(stringSerializer)
      .valueSerializer(stringSerializer)
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
    val ohcCache = new OhcCache(underlying)(implicitly[CacheConfig])
    ohcCache.put("key1")("hello", Some(1.nanosecond))
    Thread.sleep(100)
    underlying.get("key1") should be(null)
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

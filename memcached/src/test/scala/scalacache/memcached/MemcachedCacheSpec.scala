package scalacache.memcached

import org.scalatest.{ BeforeAndAfter, ShouldMatchers, FlatSpec }
import net.spy.memcached._
import scala.concurrent.duration._
import org.scalatest.concurrent.{ ScalaFutures, Eventually }
import org.scalatest.time.{ Span, Seconds }

import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global

class MemcachedCacheSpec
    extends FlatSpec with ShouldMatchers with Eventually with BeforeAndAfter with ScalaFutures {

  val client = new MemcachedClient(AddrUtil.getAddresses("localhost:11211"))

  def memcachedIsRunning = {
    try {
      client.get("foo")
      true
    } catch { case _: Exception => false }
  }

  if (!memcachedIsRunning) {
    alert("Skipping tests because Memcached does not appear to be running on localhost.")
  } else {

    before {
      client.flush()
    }

    behavior of "get"

    it should "return the value stored in Memcached" in {
      client.set("key1", 0, 123)
      whenReady(MemcachedCache(client).get("key1")) { _ should be(Some(123)) }
    }

    it should "return None if the given key does not exist in the underlying cache" in {
      whenReady(MemcachedCache(client).get("non-existent-key")) { _ should be(None) }
    }

    behavior of "put"

    it should "store the given key-value pair in the underlying cache" in {
      whenReady(MemcachedCache(client).put("key2", 123, None)) { _ =>
        client.get("key2") should be(123)
      }
    }

    behavior of "put with TTL"

    it should "store the given key-value pair in the underlying cache" in {
      whenReady(MemcachedCache(client).put("key3", 123, Some(1 second))) { _ =>
        client.get("key3") should be(123)

        // Should expire after 1 second
        eventually(timeout(Span(2, Seconds))) {
          client.get("key3") should be(null)
        }
      }
    }

    behavior of "remove"

    it should "delete the given key and its value from the underlying cache" in {
      client.set("key1", 0, 123)
      client.get("key1") should be(123)

      whenReady(MemcachedCache(client).remove("key1")) { _ =>
        client.get("key1") should be(null)
      }
    }

  }

}

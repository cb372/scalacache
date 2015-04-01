package scalacache.memcached

import org.scalatest.{ BeforeAndAfter, Matchers, FlatSpec }
import net.spy.memcached._
import scala.concurrent.duration._
import org.scalatest.concurrent.{ ScalaFutures, Eventually, IntegrationPatience }
import org.scalatest.time.{ Span, Seconds }

import scala.language.postfixOps

class MemcachedCacheSpec
    extends FlatSpec with Matchers with Eventually with BeforeAndAfter with ScalaFutures with IntegrationPatience {

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
      whenReady(MemcachedCache(client).put("key3", 123, Some(3 seconds))) { _ =>
        client.get("key3") should be(123)

        // Should expire after 3 seconds
        eventually(timeout(Span(4, Seconds))) {
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

    behavior of "caching instances of scala.util.List"

    it should "work around SI-9237 i.e. not throw a ClassNotFoundException" in {
      val cache = MemcachedCache(client)
      val listOfStuff = List(Stuff(123, "foo"), Stuff(456, "bar"))
      whenReady(cache.put("list-of-stuff", listOfStuff, ttl = None)) { _ =>
        whenReady(cache.get[List[Stuff]]("list-of-stuff")) { result =>
          result should be(Some(listOfStuff))
        }
      }
    }
  }

}

case class Stuff(id: Int, name: String)

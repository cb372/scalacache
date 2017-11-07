package scalacache.ehcache

import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}
import net.sf.ehcache.{CacheManager, Element, Cache => Ehcache}

import scala.concurrent.duration._
import language.postfixOps
import org.scalatest.time.{Seconds, Span}
import org.scalatest.concurrent.{Eventually, ScalaFutures}

import scalacache._

class EhcacheCacheSpec extends FlatSpec with Matchers with Eventually with BeforeAndAfter with ScalaFutures {

  private val underlying = {
    val cacheManager = new CacheManager
    val cache = new Ehcache("test", 1000, false, false, 0, 0)
    cacheManager.addCache(cache)
    cache
  }

  import scalacache.modes.sync._

  before {
    underlying.removeAll()
  }

  behavior of "get"

  it should "return the value stored in Ehcache" in {
    underlying.put(new Element("key1", 123))
    EhcacheCache[Int](underlying).get("key1") should be(Some(123))
  }

  it should "return None if the given key does not exist in the underlying cache" in {
    EhcacheCache[Int](underlying).get("non-existent-key") should be(None)
  }

  behavior of "put"

  it should "store the given key-value pair in the underlying cache" in {
    EhcacheCache[Int](underlying).put("key1")(123, None)
    underlying.get("key1").getObjectValue should be(123)
  }

  behavior of "put with TTL"

  it should "store the given key-value pair in the underlying cache" in {
    EhcacheCache[Int](underlying).put("key1")(123, Some(1 second))
    underlying.get("key1").getObjectValue should be(123)

    // Should expire after 1 second
    eventually(timeout(Span(2, Seconds))) {
      underlying.get("key1") should be(null)
    }
  }

  behavior of "remove"

  it should "delete the given key and its value from the underlying cache" in {
    underlying.put(new Element("key1", 123))
    underlying.get("key1").getObjectValue should be(123)

    EhcacheCache[Int](underlying).remove("key1")
    underlying.get("key1") should be(null)
  }

}

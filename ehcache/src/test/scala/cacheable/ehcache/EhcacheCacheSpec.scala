package cacheable.ehcache

import org.scalatest.{BeforeAndAfter, FlatSpec, ShouldMatchers}
import net.sf.ehcache.{Cache => Ehcache, CacheManager, Element}
import scala.concurrent.duration._
import language.postfixOps
import org.scalatest.time.{Seconds, Span}
import org.scalatest.concurrent.Eventually

/**
 * Author: chris
 * Created: 11/16/13
 */
class EhcacheCacheSpec extends FlatSpec with ShouldMatchers with Eventually with BeforeAndAfter {

  val underlying = {
    val cacheManager = new CacheManager
    val cache = new Ehcache("test", 1000, false, false, 0, 0)
    cacheManager.addCache(cache)
    cache
  }

  before {
    underlying.removeAll()
  }

  behavior of "get"

  it should "return the value stored in Ehcache" in {
    underlying.put(new Element("key1", 123))
    EhcacheCache(underlying).get("key1") should be(Some(123))
  }

  it should "return None if the given key does not exist in the underlying cache" in {
    EhcacheCache(underlying).get("non-existent-key") should be(None)
  }

  behavior of "put"

  it should "store the given key-value pair in the underlying cache" in {
    EhcacheCache(underlying).put("key1", 123, None)
    underlying.get("key1").getObjectValue should be(123)
  }

  behavior of "put with TTL"

  it should "store the given key-value pair in the underlying cache" in {
    EhcacheCache(underlying).put("key1", 123, Some(1 second))
    underlying.get("key1").getObjectValue should be(123)

    // Should expire after 1 second
    eventually(timeout(Span(2, Seconds))) {
      underlying.get("key1") should be(null)
    }
  }

}

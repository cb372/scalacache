package scalacache.ehcache

import cats.effect.IO
import net.sf.ehcache.{CacheManager, Element, Cache => Ehcache}
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

import scala.concurrent.duration._
import scala.language.postfixOps

class EhcacheCacheSpec extends FlatSpec with Matchers with Eventually with BeforeAndAfter with ScalaFutures {

  import scalacache.CatsEffect.implicits._
  import scalacache.serialization.binary._

  private val underlying = {
    val cacheManager = new CacheManager
    val cache = new Ehcache("test", 1000, false, false, 0, 0)
    cacheManager.addCache(cache)
    cache
  }

  private def newCacheInstance: EhcacheCache[IO] = EhcacheCache[IO](underlying)

  before {
    underlying.removeAll()
  }

  behavior of "get"

  it should "return the value stored in Ehcache" in {
    underlying.put(new Element("key1", 123))
    newCacheInstance.get("key1").unsafeRunSync() should be(Some(123))
  }

  it should "return None if the given key does not exist in the underlying cache" in {
    newCacheInstance.get("non-existent-key").unsafeRunSync() should be(None)
  }

  behavior of "put"

  it should "store the given key-value pair in the underlying cache" in {
    newCacheInstance.put("key1")(123, None).unsafeRunSync()
    underlying.get("key1").getObjectValue should be(123)
  }

  behavior of "put with TTL"

  it should "store the given key-value pair in the underlying cache" in {
    newCacheInstance.put("key1")(123, Some(1 second)).unsafeRunSync()
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

    newCacheInstance.remove("key1").unsafeRunSync()
    underlying.get("key1") should be(null)
  }

}

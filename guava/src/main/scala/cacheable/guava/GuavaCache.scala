package cacheable.guava

import cacheable.{ LoggingSupport, Cache }
import com.google.common.cache.{ Cache => GCache, CacheBuilder => GCacheBuilder }
import scala.concurrent.duration.Duration
import org.joda.time.DateTime
import com.typesafe.scalalogging.slf4j.StrictLogging

/**
 * Author: chris
 * Created: 2/19/13
 */

/*
Note: Would be nice to use Any here, but that doesn't conform to GCache's type bounds,
because Any does not extend java.lang.Object.
 */
class GuavaCache(underlying: GCache[String, Object])
    extends Cache
    with LoggingSupport
    with StrictLogging {

  /**
   * Get the value corresponding to the given key from the cache
   * @param key cache key
   * @tparam V the type of the corresponding value
   * @return the value, if there is one
   */
  def get[V](key: String) = {
    val entry = Option(underlying.getIfPresent(key).asInstanceOf[Entry[V]])
    /*
     Note: we could delete the entry from the cache if it has expired,
     but that would lead to nasty race conditions in case of concurrent access.
     We might end up deleting an entry that another thread has just inserted.
     */
    val result = entry.flatMap { e =>
      if (e.isExpired) None
      else Some(e.value)
    }
    logCacheHitOrMiss(key, result)
    result
  }

  /**
   * Insert the given key-value pair into the cache, with an optional Time To Live.
   * @param key cache key
   * @param value corresponding value
   * @param ttl Time To Live
   * @tparam V the type of the corresponding value
   */
  def put[V](key: String, value: V, ttl: Option[Duration]) {
    val entry = Entry(value, ttl.map(toExpiryTime))
    underlying.put(key, entry.asInstanceOf[Object])
    logCachePut(key, ttl)
  }

  /**
   * Remove the given key and its associated value from the cache, if it exists.
   * If the key is not in the cache, do nothing.
   * @param key cache key
   */
  def remove(key: String): Unit = underlying.invalidate(key)

  private def toExpiryTime(ttl: Duration): DateTime = DateTime.now.plusMillis(ttl.toMillis.toInt)

}

object GuavaCache {

  /**
   * Create a new Guava cache
   */
  def apply(): GuavaCache = apply(GCacheBuilder.newBuilder().build[String, Object]())

  /**
   * Create a new cache utilizing the given underlying Guava cache.
   * @param underlying a Guava cache
   */
  def apply(underlying: GCache[String, Object]): GuavaCache = new GuavaCache(underlying)

}

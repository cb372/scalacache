package scalacache.ehcache

import scalacache.{ LoggingSupport, Cache }
import scala.concurrent.duration.Duration
import net.sf.ehcache.{ Cache => Ehcache, Element }
import com.typesafe.scalalogging.slf4j.StrictLogging

/**
 * Author: chris
 * Created: 11/16/13
 */
class EhcacheCache(underlying: Ehcache)
    extends Cache
    with LoggingSupport
    with StrictLogging {

  /**
   * Get the value corresponding to the given key from the cache
   * @param key cache key
   * @tparam V the type of the corresponding value
   * @return the value, if there is one
   */
  def get[V](key: String): Option[V] = {
    val result = for {
      e <- Option(underlying.get(key))
      v <- Option(e.getObjectValue.asInstanceOf[V])
    } yield v
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
  def put[V](key: String, value: V, ttl: Option[Duration]): Unit = {
    val element = new Element(key, value)
    ttl.foreach(t => element.setTimeToLive(t.toSeconds.toInt))
    underlying.put(element)
    logCachePut(key, ttl)
  }

  /**
   * Remove the given key and its associated value from the cache, if it exists.
   * If the key is not in the cache, do nothing.
   * @param key cache key
   */
  def remove(key: String): Unit = underlying.remove(key)

}

object EhcacheCache {

  /**
   * Create a new cache utilizing the given underlying Ehcache cache.
   * @param underlying an Ehcache cache
   */
  def apply(underlying: Ehcache): EhcacheCache = new EhcacheCache(underlying)

}

package scalacache.ehcache

import scalacache.{ LoggingSupport, Cache }
import scala.concurrent.duration.Duration
import net.sf.ehcache.{ Cache => Ehcache, Element }
import com.typesafe.scalalogging.slf4j.StrictLogging
import scala.concurrent.{ ExecutionContext, Future }

/**
 * Thin wrapper around Ehcache.
 * Since Ehcache is in-memory and non-blocking,
 * all operations are performed synchronously, i.e. ExecutionContext is ignored.
 *
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
  override def get[V](key: String)(implicit execContext: ExecutionContext) = {
    val result = for {
      e <- Option(underlying.get(key))
      v <- Option(e.getObjectValue.asInstanceOf[V])
    } yield v
    logCacheHitOrMiss(key, result)
    Future.successful(result)
  }

  /**
   * Insert the given key-value pair into the cache, with an optional Time To Live.
   * @param key cache key
   * @param value corresponding value
   * @param ttl Time To Live
   * @tparam V the type of the corresponding value
   */
  override def put[V](key: String, value: V, ttl: Option[Duration])(implicit execContext: ExecutionContext) = {
    val element = new Element(key, value)
    ttl.foreach(t => element.setTimeToLive(t.toSeconds.toInt))
    underlying.put(element)
    logCachePut(key, ttl)
    Future.successful(())
  }

  /**
   * Remove the given key and its associated value from the cache, if it exists.
   * If the key is not in the cache, do nothing.
   * @param key cache key
   */
  override def remove(key: String)(implicit execContext: ExecutionContext) = Future.successful(underlying.remove(key))

}

object EhcacheCache {

  /**
   * Create a new cache utilizing the given underlying Ehcache cache.
   * @param underlying an Ehcache cache
   */
  def apply(underlying: Ehcache): EhcacheCache = new EhcacheCache(underlying)

}

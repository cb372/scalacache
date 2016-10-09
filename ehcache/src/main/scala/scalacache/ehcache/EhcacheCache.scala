package scalacache.ehcache

import org.slf4j.LoggerFactory

import scalacache.serialization.{ Codec, InMemoryRepr }
import scalacache.{ Cache, LoggingSupport }
import scala.concurrent.duration.Duration
import net.sf.ehcache.{ Element, Cache => Ehcache }

import scala.concurrent.Future

/**
 * Thin wrapper around Ehcache.
 * Since Ehcache is in-memory and non-blocking,
 * all operations are performed synchronously, i.e. ExecutionContext is not needed.
 */
class EhcacheCache(underlying: Ehcache)
    extends Cache[InMemoryRepr]
    with LoggingSupport {

  override protected final val logger = LoggerFactory.getLogger(getClass.getName)

  override def get[V](key: String)(implicit codec: Codec[V, InMemoryRepr]) = {
    val result = {
      val elem = underlying.get(key)
      if (elem == null) None
      else Option(elem.getObjectValue.asInstanceOf[V])
    }
    if (logger.isDebugEnabled)
      logCacheHitOrMiss(key, result)
    result
  }

  override def put[V](key: String, value: V, ttl: Option[Duration])(implicit codec: Codec[V, InMemoryRepr]) = {
    val element = new Element(key, value)
    ttl.foreach(t => element.setTimeToLive(t.toSeconds.toInt))
    underlying.put(element)
    logCachePut(key, ttl)
  }

  override def remove(key: String) = underlying.remove(key)

  override def removeAll() = underlying.removeAll()

  override def close(): Unit = {
    // Nothing to do
  }

}

object EhcacheCache {

  /**
   * Create a new cache utilizing the given underlying Ehcache cache.
   *
   * @param underlying an Ehcache cache
   */
  def apply(underlying: Ehcache): EhcacheCache = new EhcacheCache(underlying)

}

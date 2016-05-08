package scalacache.lrumap

import com.twitter.util.LruMap
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

import scalacache.serialization.{ Codec, InMemoryRepr }
import scalacache.{ Cache, LoggingSupport }

import scala.concurrent.Future
import scala.concurrent.duration.Duration

/**
 * This wrapper around LruMap from twitter utils.
 *
 * LruMap doesn't support a expired key and I use simple wrapper for implement this.
 *
 */
class LruMapCache(underlying: LruMap[String, Object])
    extends Cache[InMemoryRepr]
    with LoggingSupport {

  override protected final val logger = LoggerFactory.getLogger(getClass.getName)

  /**
   * Get the value corresponding to the given key from the cache
   *
   * @param key cache key
   * @tparam V the type of the corresponding value
   * @return the value, if there is one
   */
  override def get[V](key: String)(implicit codec: Codec[V, InMemoryRepr]): Future[Option[V]] = {
    val entry = underlying.get(key).map(_.asInstanceOf[LruMapCache.Entry[V]])

    val result = entry.flatMap { e =>
      if (e.isExpired) {
        // remove expired entry
        underlying.remove(key)
        None
      } else Some(e.value)
    }
    logCacheHitOrMiss(key, result)
    Future.successful(result)
  }

  /**
   * Insert the given key-value pair into the cache, with an optional Time To Live.
   *
   * @param key cache key
   * @param value corresponding value
   * @param ttl Time To Live
   * @tparam V the type of the corresponding value
   */
  override def put[V](key: String, value: V, ttl: Option[Duration])(implicit codec: Codec[V, InMemoryRepr]): Future[Unit] = {
    val entry = LruMapCache.Entry(value, ttl.map(toExpiryTime))
    underlying.put(key, entry.asInstanceOf[Object])
    logCachePut(key, ttl)
    Future.successful(())
  }

  /**
   * Remove the given key and its associated value from the cache, if it exists.
   * If the key is not in the cache, do nothing.
   *
   * @param key cache key
   */
  override def remove(key: String): Future[Unit] =
    Future.successful(underlying.remove(key))

  override def removeAll() = Future.successful(underlying.clear())

  override def close(): Unit = {
    // Nothing to do
  }

  private def toExpiryTime(ttl: Duration): DateTime = DateTime.now.plusMillis(ttl.toMillis.toInt)
}

object LruMapCache {

  def apply(maxSize: Int): LruMapCache = apply(new LruMap[String, Object](maxSize))

  def apply(underlying: LruMap[String, Object]): LruMapCache = new LruMapCache(underlying)

  /**
   * A cache entry with an optional expiry time
   */
  case class Entry[+A](value: A, expiresAt: Option[DateTime]) {

    /**
     * Has the entry expired yet?
     */
    def isExpired: Boolean = expiresAt.exists(_.isBeforeNow)
  }
}

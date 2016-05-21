package scalacache.caffeine

import com.github.benmanes.caffeine.cache.{ Caffeine, Cache => CCache }
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import scalacache.serialization.{ Codec, InMemoryRepr }
import scalacache.{ Cache, Entry, LoggingSupport }

import scala.concurrent.duration.Duration
import scala.concurrent.Future

/*
 * Thin wrapper around Caffeine.
 * Since Caffeine is in-memory and non-blocking,
 * all operations are performed synchronously, i.e. ExecutionContext is not needed.
 *
 * Note: Would be nice to use Any here, but that doesn't conform to CCache's type bounds,
 * because Any does not extend java.lang.Object.
 */
class CaffeineCache(underlying: CCache[String, Object])
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
  override def get[V](key: String)(implicit codec: Codec[V, InMemoryRepr]) = {
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
  override def put[V](key: String, value: V, ttl: Option[Duration] = None)(implicit codec: Codec[V, InMemoryRepr]) = {
    val entry = Entry(value, ttl.map(toExpiryTime))
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
  override def remove(key: String) = Future.successful(underlying.invalidate(key))

  override def removeAll() = Future.successful(underlying.invalidateAll())

  override def close(): Unit = {
    // Nothing to do
  }

  private def toExpiryTime(ttl: Duration): DateTime = DateTime.now.plus(ttl.toMillis)

}

object CaffeineCache {

  /**
   * Create a new Caffeine cache
   */
  def apply(): CaffeineCache = apply(Caffeine.newBuilder().build[String, Object]())

  /**
   * Create a new cache utilizing the given underlying Caffeine cache.
   *
   * @param underlying a Caffeine cache
   */
  def apply(underlying: CCache[String, Object]): CaffeineCache = new CaffeineCache(underlying)

}

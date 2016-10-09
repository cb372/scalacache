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

  override def get[V](key: String)(implicit codec: Codec[V, InMemoryRepr]) = {
    /*
    Note: we could delete the entry from the cache if it has expired,
    but that would lead to nasty race conditions in case of concurrent access.
    We might end up deleting an entry that another thread has just inserted.
    */
    val baseValue = underlying.getIfPresent(key)
    val result = {
      if (baseValue != null) {
        val entry = baseValue.asInstanceOf[Entry[V]]
        if (entry.isExpired) None else Some(entry.value)
      } else None
    }
    if (logger.isDebugEnabled)
      logCacheHitOrMiss(key, result)
    result
  }

  override def put[V](key: String, value: V, ttl: Option[Duration] = None)(implicit codec: Codec[V, InMemoryRepr]) = {
    val entry = Entry(value, ttl.map(toExpiryTime))
    underlying.put(key, entry.asInstanceOf[Object])
    logCachePut(key, ttl)
  }

  override def remove(key: String) = underlying.invalidate(key)

  override def removeAll() = underlying.invalidateAll()

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

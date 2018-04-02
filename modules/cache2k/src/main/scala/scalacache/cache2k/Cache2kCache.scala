package scalacache.cache2k

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant}

import org.cache2k.{Cache2kBuilder, Cache => CCache}
import org.slf4j.LoggerFactory

import scala.concurrent.duration.Duration
import scala.language.higherKinds
import scalacache.{AbstractCache, CacheConfig, Entry, Mode}

/*
 * Thin wrapper around cache2k.
 *
 * This cache implementation is synchronous.
 */
class Cache2kCache[V](underlying: CCache[String, Entry[V]])(implicit val config: CacheConfig,
                                                            clock: Clock = Clock.systemUTC())
    extends AbstractCache[V] {

  override protected final val logger =
    LoggerFactory.getLogger(getClass.getName)

  def doGet[F[_]](key: String)(implicit mode: Mode[F]): F[Option[V]] = {
    mode.M.delay {
      val baseValue = underlying.peek(key)
      val result = {
        if (baseValue != null) {
          val entry = baseValue.asInstanceOf[Entry[V]]
          if (entry.isExpired) None else Some(entry.value)
        } else None
      }
      logCacheHitOrMiss(key, result)
      result
    }
  }

  def doPut[F[_]](key: String, value: V, ttl: Option[Duration])(implicit mode: Mode[F]): F[Any] = {
    mode.M.delay {
      val entry = Entry(value, ttl.map(toExpiryTime))
      underlying.put(key, entry)
      logCachePut(key, ttl)
    }
  }

  override def doRemove[F[_]](key: String)(implicit mode: Mode[F]): F[Any] =
    mode.M.delay(underlying.remove(key))

  override def doRemoveAll[F[_]]()(implicit mode: Mode[F]): F[Any] =
    mode.M.delay(underlying.clear())

  override def close[F[_]]()(implicit mode: Mode[F]): F[Any] = {
    // Nothing to do
    mode.M.pure(())
  }

  private def toExpiryTime(ttl: Duration): Instant =
    Instant.now(clock).plus(ttl.toMillis, ChronoUnit.MILLIS)

}

object Cache2kCache {

  /**
    * Create a new cache2k cache
    */
  def apply[V](implicit config: CacheConfig): Cache2kCache[V] =
    apply(Cache2kBuilder.of(classOf[String], classOf[Entry[V]]).build)

  /**
    * Create a new cache utilizing the given underlying cache2k cache.
    *
    * @param underlying a cache2k cache
    */
  def apply[V](underlying: CCache[String, Entry[V]])(implicit config: CacheConfig): Cache2kCache[V] =
    new Cache2kCache(underlying)

}

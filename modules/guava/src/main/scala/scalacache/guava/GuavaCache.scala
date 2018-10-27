package scalacache.guava

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant}

import scalacache.{AbstractCache, CacheConfig, Entry, Mode}
import scalacache.logging.Logger

import com.google.common.cache.{Cache => GCache, CacheBuilder => GCacheBuilder}

import scala.concurrent.duration.Duration
import scala.language.higherKinds

/*
 * Thin wrapper around Google Guava.
 */
class GuavaCache[V](val underlying: GCache[String, Entry[V]])(implicit val config: CacheConfig,
                                                              clock: Clock = Clock.systemUTC())
    extends AbstractCache[V] {

  override protected final val logger =
    Logger.getLogger(getClass.getName)

  def doGet[F[_]](key: String)(implicit mode: Mode[F]): F[Option[V]] = {
    mode.M.delay {
      val entry = underlying.getIfPresent(key)
      val result = {
        if (entry == null || entry.isExpired)
          None
        else
          Some(entry.value)
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
    mode.M.delay(underlying.invalidate(key))

  override def doRemoveAll[F[_]]()(implicit mode: Mode[F]): F[Any] =
    mode.M.delay(underlying.invalidateAll())

  override def close[F[_]]()(implicit mode: Mode[F]): F[Any] = {
    // Nothing to do
    mode.M.pure(())
  }

  private def toExpiryTime(ttl: Duration): Instant =
    Instant.now(clock).plus(ttl.toMillis, ChronoUnit.MILLIS)

}

object GuavaCache {

  /**
    * Create a new Guava cache
    */
  def apply[V](implicit config: CacheConfig): GuavaCache[V] =
    apply(GCacheBuilder.newBuilder().build[String, Entry[V]]())

  /**
    * Create a new cache utilizing the given underlying Guava cache.
    *
    * @param underlying a Guava cache
    */
  def apply[V](underlying: GCache[String, Entry[V]])(implicit config: CacheConfig): GuavaCache[V] =
    new GuavaCache(underlying)

}

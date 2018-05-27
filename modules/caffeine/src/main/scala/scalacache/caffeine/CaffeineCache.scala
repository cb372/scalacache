package scalacache.caffeine

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant}

import com.github.benmanes.caffeine.cache.{Caffeine, Cache => CCache}
import org.slf4j.LoggerFactory
import scalacache.serialization.Codec
import scalacache.{AbstractCache, CacheConfig, Entry, Mode}

import scala.concurrent.duration.Duration
import scala.language.higherKinds

/*
 * Thin wrapper around Caffeine.
 *
 * This cache implementation is synchronous.
 */
class CaffeineCache[F[_]](underlying: CCache[String, Entry[Any]])(implicit val config: CacheConfig,
                                                                  mode: Mode[F],
                                                                  clock: Clock = Clock.systemUTC())
    extends AbstractCache[F] {

  override protected final val logger =
    LoggerFactory.getLogger(getClass.getName)

  def doGet[V: Codec](key: String): F[Option[V]] = {
    mode.M.delay {
      val baseValue = underlying.getIfPresent(key)
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

  def doPut[V: Codec](key: String, value: V, ttl: Option[Duration]): F[Any] = {
    mode.M.delay {
      val entry = Entry(value, ttl.map(toExpiryTime))
      underlying.put(key, entry)
      logCachePut(key, ttl)
    }
  }

  override def doRemove(key: String): F[Any] =
    mode.M.delay(underlying.invalidate(key))

  override def doRemoveAll(): F[Any] =
    mode.M.delay(underlying.invalidateAll())

  override def close(): F[Any] = {
    // Nothing to do
    mode.M.pure(())
  }

  private def toExpiryTime(ttl: Duration): Instant =
    Instant.now(clock).plus(ttl.toMillis, ChronoUnit.MILLIS)

}

object CaffeineCache {

  /**
    * Create a new Caffeine cache
    */
  def apply[F[_]: Mode](implicit config: CacheConfig): CaffeineCache[F] =
    apply(Caffeine.newBuilder().build[String, Entry[Any]]())

  /**
    * Create a new cache utilizing the given underlying Caffeine cache.
    *
    * @param underlying a Caffeine cache
    */
  def apply[F[_]: Mode](underlying: CCache[String, Entry[Any]])(implicit config: CacheConfig): CaffeineCache[F] =
    new CaffeineCache(underlying)

}

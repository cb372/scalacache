package scalacache.caffeine

import java.time.temporal.ChronoUnit
import java.time.{Instant}

import com.github.benmanes.caffeine.cache.{Caffeine, Cache => CCache}
import cats.effect.Clock
import scalacache.logging.Logger
import scalacache.{AbstractCache, CacheConfig, Entry}
import scala.concurrent.duration.Duration
import scala.language.higherKinds
import cats.effect.Sync
import java.util.concurrent.TimeUnit
import cats.implicits._
import cats.MonadError

/*
 * Thin wrapper around Caffeine.
 *
 * This cache implementation is synchronous.
 */
class CaffeineCache[F[_]: Sync, V](val underlying: CCache[String, Entry[V]])(implicit
    val config: CacheConfig,
    clock: Clock[F]
) extends AbstractCache[F, V] {
  protected val F: Sync[F] = Sync[F]

  override protected final val logger = Logger.getLogger(getClass.getName)

  def doGet(key: String): F[Option[V]] = {
    F.delay {
      Option(underlying.getIfPresent(key))
    }.flatMap(_.filterA(Entry.isNotExpired[F, V]))
      .map(_.map(_.value))
      .flatTap { result =>
        logCacheHitOrMiss(key, result)
      }
  }

  def doPut(key: String, value: V, ttl: Option[Duration]): F[Unit] =
    ttl.traverse(toExpiryTime).flatMap { expiry =>
      F.delay {
        val entry = Entry(value, expiry)
        underlying.put(key, entry)
      } *> logCachePut(key, ttl)
    }

  override def doRemove(key: String): F[Unit] =
    F.delay(underlying.invalidate(key))

  override def doRemoveAll: F[Unit] =
    F.delay(underlying.invalidateAll())

  override def close: F[Unit] = {
    // Nothing to do
    F.unit
  }

  private def toExpiryTime(ttl: Duration): F[Instant] =
    clock.monotonic.map(m => Instant.ofEpochMilli(m.toMillis).plusMillis(ttl.toMillis))

}

object CaffeineCache {

  /** Create a new Caffeine cache.
    */
  def apply[F[_]: Sync: Clock, V](implicit config: CacheConfig): F[CaffeineCache[F, V]] =
    Sync[F].delay(Caffeine.newBuilder().build[String, Entry[V]]()).map(apply(_))

  /** Create a new cache utilizing the given underlying Caffeine cache.
    *
    * @param underlying
    *   a Caffeine cache
    */
  def apply[F[_]: Sync: Clock, V](
      underlying: CCache[String, Entry[V]]
  )(implicit config: CacheConfig): CaffeineCache[F, V] =
    new CaffeineCache(underlying)

}

package scalacache.caffeine

import cats.effect.Sync
import cats.implicits._
import com.github.benmanes.caffeine.cache.{Caffeine, Cache => CCache}
import scalacache.logging.Logger
import scalacache.{AbstractCache, Entry}

import java.time.Instant
import scala.concurrent.duration.Duration
import scala.language.higherKinds

/*
 * Thin wrapper around Caffeine.
 *
 * This cache implementation is synchronous.
 */
class CaffeineCache[F[_]: Sync, K, V](val underlying: CCache[K, Entry[V]]) extends AbstractCache[F, K, V] {
  protected val F: Sync[F] = Sync[F]

  override protected final val logger = Logger.getLogger(getClass.getName)

  def doGet(key: K): F[Option[V]] = {
    F.delay {
      Option(underlying.getIfPresent(key))
    }.flatMap(_.filterA(Entry.isBeforeExpiration[F, V]))
      .map(_.map(_.value))
      .flatTap { result =>
        logCacheHitOrMiss(key, result)
      }
  }

  def doPut(key: K, value: V, ttl: Option[Duration]): F[Unit] =
    ttl.traverse(toExpiryTime).flatMap { expiry =>
      F.delay {
        val entry = Entry(value, expiry)
        underlying.put(key, entry)
      } *> logCachePut(key, ttl)
    }

  override def doRemove(key: K): F[Unit] =
    F.delay(underlying.invalidate(key))

  override def doRemoveAll: F[Unit] =
    F.delay(underlying.invalidateAll())

  override def close: F[Unit] = {
    // Nothing to do
    F.unit
  }

  private def toExpiryTime(ttl: Duration): F[Instant] =
    Sync[F].monotonic.map(m => Instant.ofEpochMilli(m.toMillis).plusMillis(ttl.toMillis))

}

object CaffeineCache {

  /** Create a new Caffeine cache.
    */
  def apply[F[_]: Sync, K <: AnyRef, V]: F[CaffeineCache[F, K, V]] =
    Sync[F].delay(Caffeine.newBuilder.build[K, Entry[V]]()).map(apply(_))

  /** Create a new cache utilizing the given underlying Caffeine cache.
    *
    * @param underlying
    *   a Caffeine cache
    */
  def apply[F[_]: Sync, K, V](
      underlying: CCache[K, Entry[V]]
  ): CaffeineCache[F, K, V] =
    new CaffeineCache(underlying)

}

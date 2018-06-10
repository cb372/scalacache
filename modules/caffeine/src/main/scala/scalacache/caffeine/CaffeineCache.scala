package scalacache.caffeine

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant}

import com.github.benmanes.caffeine.cache.{Caffeine, Cache => CCache}
import org.slf4j.LoggerFactory
import scalacache.serialization.Codec
import scalacache.serialization.Codec.DecodingResult
import scalacache._

import scala.concurrent.duration.Duration
import scala.language.higherKinds

/**
  * Thin wrapper around Caffeine.
  *
  * This cache implementation is synchronous.
  */
class CaffeineCache[F[_]](override val underlying: CCache[String, Entry])(
    implicit val config: CacheConfig,
    F: Async[F],
    clock: Clock = Clock.systemUTC()
) extends AbstractCache[F] {

  override type Underlying = CCache[String, Entry]

  override protected final val logger = LoggerFactory.getLogger(getClass.getName)

  def doGet[V](key: String)(implicit codec: Codec[V]): F[Option[V]] = {
    F.delay {
      val baseValue = underlying.getIfPresent(key)
      val result =
        if (baseValue == null || baseValue.isExpired) None else DecodingResult.toOption(codec.decode(baseValue.value))
      logCacheHitOrMiss(key, result)
      result
    }
  }

  def doPut[V](key: String, value: V, ttl: Option[Duration])(implicit codec: Codec[V]): F[Unit] = {
    @inline def toExpiryTime(ttl: Duration): Instant = Instant.now(clock).plus(ttl.toMillis, ChronoUnit.MILLIS)

    F.delay {
      val entry = Entry(codec.encode(value), ttl.map(toExpiryTime))
      underlying.put(key, entry)
      logCachePut(key, ttl)
    }
  }

  override def doRemove(key: String): F[Any] = F.delay(underlying.invalidate(key))
  override def doRemoveAll(): F[Any] = F.delay(underlying.invalidateAll())
  override def close(): F[Any] = F.pure(()) // Nothing to do

}

object CaffeineCache {

  /**
    * Create a new Caffeine cache
    */
  def apply[F[_]: Async](implicit config: CacheConfig): CaffeineCache[F] =
    apply(Caffeine.newBuilder().build[String, Entry]())

  /**
    * Create a new cache utilizing the given underlying Caffeine cache.
    *
    * @param underlying a Caffeine cache
    */
  def apply[F[_]: Async](underlying: CCache[String, Entry])(implicit config: CacheConfig): CaffeineCache[F] =
    new CaffeineCache(underlying)

}

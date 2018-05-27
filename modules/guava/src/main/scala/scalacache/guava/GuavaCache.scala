package scalacache.guava

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant}

import org.slf4j.LoggerFactory
import scalacache.{AbstractCache, CacheConfig, Entry, Mode}
import com.google.common.cache.{Cache => GCache, CacheBuilder => GCacheBuilder}
import scalacache.serialization.Codec
import scalacache.serialization.Codec.DecodingResult

import scala.concurrent.duration.Duration
import scala.language.higherKinds

/**
  * Thin wrapper around Google Guava.
  */
final class GuavaCache[F[_]](underlying: GCache[String, Entry])(
    implicit val config: CacheConfig,
    mode: Mode[F],
    clock: Clock = Clock.systemUTC()
) extends AbstractCache[F] {

  override protected final val logger = LoggerFactory.getLogger(getClass.getName)

  def doGet[V](key: String)(implicit codec: Codec[V]): F[Option[V]] =
    mode.M.delay {
      val baseValue = underlying.getIfPresent(key)
      val result =
        if (baseValue == null || baseValue.isExpired) None else DecodingResult.toOption(codec.decode(baseValue.value))
      logCacheHitOrMiss(key, result)
      result
    }

  def doPut[V](key: String, value: V, ttl: Option[Duration])(implicit codec: Codec[V]): F[Unit] = {
    @inline def toExpiryTime(ttl: Duration): Instant = Instant.now(clock).plus(ttl.toMillis, ChronoUnit.MILLIS)

    mode.M.delay {
      val entry = Entry(codec.encode(value), ttl.map(toExpiryTime))
      underlying.put(key, entry)
      logCachePut(key, ttl)
    }
  }

  override def doRemove(key: String): F[Any] = mode.M.delay(underlying.invalidate(key))
  override def doRemoveAll(): F[Any] = mode.M.delay(underlying.invalidateAll())
  override def close(): F[Any] = mode.M.pure(()) // Nothing to do

}

object GuavaCache {

  /**
    * Create a new Guava cache
    */
  def apply[F[_]: Mode](implicit config: CacheConfig): GuavaCache[F] =
    apply(GCacheBuilder.newBuilder().build[String, Entry]())

  /**
    * Create a new cache utilizing the given underlying Guava cache.
    *
    * @param underlying a Guava cache
    */
  def apply[F[_]: Mode](underlying: GCache[String, Entry])(implicit config: CacheConfig): GuavaCache[F] =
    new GuavaCache(underlying)

}

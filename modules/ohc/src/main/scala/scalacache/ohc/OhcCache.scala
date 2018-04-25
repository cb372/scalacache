package scalacache.ohc

import org.caffinitas.ohc.{CacheSerializer, OHCache, OHCacheBuilder}
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.language.higherKinds
import scalacache.{AbstractCache, CacheConfig, Mode}

/*
 * Thin wrapper around OHC.
 *
 * This cache implementation is synchronous.
 */
class OhcCache[V](underlying: OHCache[String, V])(implicit val config: CacheConfig) extends AbstractCache[V] {

  override protected final val logger =
    LoggerFactory.getLogger(getClass.getName)

  def doGet[F[_]](key: String)(implicit mode: Mode[F]): F[Option[V]] = {
    mode.M.delay {
      val result = Option(underlying.get(key))
      logCacheHitOrMiss(key, result)
      result
    }
  }

  def doPut[F[_]](key: String, value: V, ttl: Option[Duration])(implicit mode: Mode[F]): F[Any] = {
    mode.M.delay {
      ttl.fold(underlying.put(key, value))(x => underlying.put(key, value, toExpiryTime(x)))
      logCachePut(key, ttl)
    }
  }

  override def doRemove[F[_]](key: String)(implicit mode: Mode[F]): F[Any] =
    mode.M.delay(underlying.remove(key))

  override def doRemoveAll[F[_]]()(implicit mode: Mode[F]): F[Any] =
    mode.M.delay(underlying.clear())

  override def close[F[_]]()(implicit mode: Mode[F]): F[Any] =
    mode.M.pure(underlying.close())

  private def toExpiryTime(ttl: Duration): Long =
    System.currentTimeMillis + ttl.toMillis

}

object OhcCache {

  /**
    * Create a new OHC cache
    */
  def apply[V](
      keySerializer: CacheSerializer[String],
      valueSerializer: CacheSerializer[V]
  )(implicit config: CacheConfig): OhcCache[V] =
    new OhcCache(
      OHCacheBuilder
        .newBuilder()
        .keySerializer(keySerializer)
        .valueSerializer(valueSerializer)
        .timeouts(true)
        .build()
    )

  /**
    * Create a new cache utilizing the given underlying OHC cache.
    *
    * @param underlying a OHC cache
    */
  def apply[V](underlying: OHCache[String, V])(implicit config: CacheConfig): OhcCache[V] =
    new OhcCache(underlying)

}

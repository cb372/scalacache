package scalacache.cache2k

import org.cache2k.{Cache => CCache}
import scalacache.logging.Logger

import scala.concurrent.duration._
import scala.language.higherKinds
import scalacache.{AbstractCache, CacheConfig, Mode}

/*
 * Thin wrapper around cache2k.
 *
 * This cache implementation is synchronous.
 */
class Cache2kCache[V](val underlying: CCache[String, V])(implicit val config: CacheConfig) extends AbstractCache[V] {

  override protected final val logger = Logger.getLogger(getClass.getName)

  def doGet[F[_]](key: String)(implicit mode: Mode[F]): F[Option[V]] = {
    mode.M.delay {
      val result = Option(underlying.peek(key))
      logCacheHitOrMiss(key, result)
      result
    }
  }

  def doPut[F[_]](key: String, value: V, ttl: Option[Duration])(implicit mode: Mode[F]): F[Any] = {
    mode.M.delay {
      underlying.put(key, value)
      ttl.foreach(x => underlying.expireAt(key, toExpiryTime(x)))
      logCachePut(key, ttl)
    }
  }

  override def doRemove[F[_]](key: String)(implicit mode: Mode[F]): F[Any] =
    mode.M.delay(underlying.remove(key))

  override def doRemoveAll[F[_]]()(implicit mode: Mode[F]): F[Any] =
    mode.M.delay(underlying.clear())

  override def close[F[_]]()(implicit mode: Mode[F]): F[Any] =
    mode.M.delay(underlying.close())

  private def toExpiryTime(ttl: Duration): Long =
    System.currentTimeMillis + ttl.toMillis

}

object Cache2kCache {

  /**
    * Create a new cache utilizing the given underlying cache2k cache.
    *
    * @param underlying a cache2k cache configured with a ExpiryPolicy or Cache2kBuilder.expireAfterWrite(long, TimeUnit)
    */
  def apply[V](underlying: CCache[String, V])(implicit config: CacheConfig): Cache2kCache[V] =
    new Cache2kCache(underlying)

}

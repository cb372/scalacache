package scalacache.cache2k

import org.cache2k.{Cache => CCache}
import org.slf4j.LoggerFactory
import scalacache.serialization.Codec

import scala.concurrent.duration._
import scala.language.higherKinds
import scalacache.{AbstractCache, CacheConfig, Mode}

/*
 * Thin wrapper around cache2k.
 *
 * This cache implementation is synchronous.
 */
class Cache2kCache[F[_]](underlying: CCache[String, Any])(implicit val config: CacheConfig, mode: Mode[F])
    extends AbstractCache[F] {

  override protected final val logger =
    LoggerFactory.getLogger(getClass.getName)

  def doGet[V: Codec](key: String): F[Option[V]] = {
    mode.M.delay {
      val result = Option(underlying.peek(key))
      logCacheHitOrMiss(key, result)
      result.asInstanceOf[Option[V]]
    }
  }

  def doPut[V: Codec](key: String, value: V, ttl: Option[Duration]): F[Any] = {
    mode.M.delay {
      underlying.put(key, value)
      ttl.foreach(x => underlying.expireAt(key, toExpiryTime(x)))
      logCachePut(key, ttl)
    }
  }

  override def doRemove(key: String): F[Any] =
    mode.M.delay(underlying.remove(key))

  override def doRemoveAll(): F[Any] =
    mode.M.delay(underlying.clear())

  override def close(): F[Any] =
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
  def apply[F[_]: Mode](underlying: CCache[String, Any])(implicit config: CacheConfig): Cache2kCache[F] =
    new Cache2kCache(underlying)

}

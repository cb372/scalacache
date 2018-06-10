package scalacache.cache2k

import org.cache2k.{Cache => CCache}
import org.slf4j.LoggerFactory
import scalacache.serialization.Codec
import scalacache.serialization.Codec.DecodingResult
import scalacache.{AbstractCache, Async, CacheConfig}

import scala.concurrent.duration._
import scala.language.higherKinds

/**
  * Thin wrapper around cache2k.
  *
  * This cache implementation is synchronous.
  */
final class Cache2kCache[F[_]](override val underlying: CCache[String, Array[Byte]])(
    implicit val config: CacheConfig,
    F: Async[F]
) extends AbstractCache[F] {

  override type Underlying = CCache[String, Array[Byte]]

  override protected final val logger = LoggerFactory.getLogger(getClass.getName)

  def doGet[V](key: String)(implicit codec: Codec[V]): F[Option[V]] = {
    F.delay {
      val result = Option(underlying.peek(key))
      logCacheHitOrMiss(key, result)
      result.flatMap(r => DecodingResult.toOption(codec.decode(r))) // TODO Jules: Can we do better than `.toOption` as error management ?
    }
  }

  def doPut[V](key: String, value: V, ttl: Option[Duration])(implicit codec: Codec[V]): F[Unit] = {
    @inline def toExpiryTime(ttl: Duration): Long = System.currentTimeMillis + ttl.toMillis

    F.delay {
      underlying.put(key, codec.encode(value))
      ttl.foreach(x => underlying.expireAt(key, toExpiryTime(x)))
      logCachePut(key, ttl)
    }
  }

  override def doRemove(key: String): F[Any] = F.delay(underlying.remove(key))
  override def doRemoveAll(): F[Any] = F.delay(underlying.clear())
  override def close(): F[Any] = F.delay(underlying.close())

}

object Cache2kCache {

  /**
    * Create a new cache utilizing the given underlying cache2k cache.
    *
    * @param underlying a cache2k cache configured with a ExpiryPolicy or Cache2kBuilder.expireAfterWrite(long, TimeUnit)
    */
  def apply[F[_]: Async](underlying: CCache[String, Array[Byte]])(implicit config: CacheConfig): Cache2kCache[F] =
    new Cache2kCache(underlying)

}

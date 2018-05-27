package scalacache.ohc

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

import org.caffinitas.ohc.{CacheSerializer, OHCache, OHCacheBuilder}
import org.slf4j.LoggerFactory
import scalacache.serialization.Codec

import scala.concurrent.duration._
import scala.language.higherKinds
import scalacache.{AbstractCache, CacheConfig, Mode}

/*
 * Thin wrapper around OHC.
 *
 * This cache implementation is synchronous.
 */
class OhcCache[F[_]](underlying: OHCache[String, Any])(implicit val config: CacheConfig, mode: Mode[F])
    extends AbstractCache[F] {

  override protected final val logger =
    LoggerFactory.getLogger(getClass.getName)

  def doGet[V: Codec](key: String): F[Option[V]] = {
    mode.M.delay {
      val result = Option(underlying.get(key))
      logCacheHitOrMiss(key, result)
      result.asInstanceOf[Option[V]]
    }
  }

  def doPut[V: Codec](key: String, value: V, ttl: Option[Duration]): F[Unit] = {
    mode.M.delay {
      ttl.fold(underlying.put(key, value))(x => underlying.put(key, value, toExpiryTime(x)))
      logCachePut(key, ttl)
    }
  }

  override def doRemove(key: String): F[Any] =
    mode.M.delay(underlying.remove(key))

  override def doRemoveAll(): F[Any] =
    mode.M.delay(underlying.clear())

  override def close(): F[Any] =
    mode.M.pure(underlying.close())

  private def toExpiryTime(ttl: Duration): Long =
    System.currentTimeMillis + ttl.toMillis

}

object OhcCache {

  val stringSerializer: CacheSerializer[String] = new CacheSerializer[String]() {

    def serialize(s: String, buf: ByteBuffer): Unit = {
      val bytes = s.getBytes(StandardCharsets.UTF_8)
      buf.putInt(bytes.length)
      buf.put(bytes)
    }

    def deserialize(buf: ByteBuffer): String = {
      val bytes = new Array[Byte](buf.getInt)
      buf.get(bytes)
      new String(bytes, StandardCharsets.UTF_8)
    }

    def serializedSize(s: String): Int =
      s.getBytes(StandardCharsets.UTF_8).length + 4

  }

  /**
    * Create a new OHC cache
    */
  def apply[F[_]: Mode](implicit config: CacheConfig): OhcCache[F] =
    new OhcCache(
      OHCacheBuilder
        .newBuilder()
        .keySerializer(stringSerializer)
        .timeouts(true)
        .build()
    )

  /**
    * Create a new cache utilizing the given underlying OHC cache.
    *
    * @param underlying a OHC cache configured with OHCacheBuilder.timeouts(true)
    */
  def apply[F[_]: Mode](underlying: OHCache[String, Any])(implicit config: CacheConfig): OhcCache[F] =
    new OhcCache(underlying)

}

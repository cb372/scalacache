package scalacache.ohc

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

import org.caffinitas.ohc.{CacheSerializer, OHCache, OHCacheBuilder}
import org.slf4j.LoggerFactory
import scalacache.serialization.Codec
import scalacache.serialization.Codec.DecodingResult
import scalacache.{AbstractCache, Async, CacheConfig}

import scala.concurrent.duration._
import scala.language.higherKinds

/*
 * Thin wrapper around OHC.
 *
 * This cache implementation is synchronous.
 */
class OhcCache[F[_]](override final val underlying: OHCache[String, Array[Byte]])(
    implicit val config: CacheConfig,
    F: Async[F]
) extends AbstractCache[F] {

  override type Underlying = OHCache[String, Array[Byte]]

  override protected final val logger = LoggerFactory.getLogger(getClass.getName)

  final def doGet[V](key: String)(implicit codec: Codec[V]): F[Option[V]] =
    F.delay {
      val result = Option(underlying.get(key))
      logCacheHitOrMiss(key, result)
      result.flatMap(r => DecodingResult.toOption(codec.decode(r)))
    }

  final def doPut[V](key: String, value: V, ttl: Option[Duration])(implicit codec: Codec[V]): F[Unit] = {
    @inline def toExpiryTime(ttl: Duration): Long = System.currentTimeMillis + ttl.toMillis

    F.delay {
      ttl.fold(underlying.put(key, codec.encode(value)))(x => underlying.put(key, codec.encode(value), toExpiryTime(x)))
      logCachePut(key, ttl)
    }
  }

  override final def doRemove(key: String): F[Unit] = F.delay(underlying.remove(key))
  override final def doRemoveAll(): F[Unit] = F.delay(underlying.clear())
  override final def close(): F[Unit] = F.pure(underlying.close())

}

object OhcCache {

  final val bytesSerializer: CacheSerializer[Array[Byte]] = new CacheSerializer[Array[Byte]]() {
    override final def serialize(bytes: Array[Byte], buf: ByteBuffer): Unit = {
      buf.putInt(bytes.length)
      buf.put(bytes)
    }

    override final def deserialize(buf: ByteBuffer): Array[Byte] = {
      val bytes = new Array[Byte](buf.getInt)
      buf.get(bytes)
      bytes
    }

    // TODO Jules: `+ 4 ?
    override final def serializedSize(value: Array[Byte]): Int = value.length

  }

  final val stringSerializer: CacheSerializer[String] = new CacheSerializer[String]() {

    final def serialize(s: String, buf: ByteBuffer): Unit =
      bytesSerializer.serialize(s.getBytes(StandardCharsets.UTF_8), buf)

    final def deserialize(buf: ByteBuffer): String =
      new String(bytesSerializer.deserialize(buf), StandardCharsets.UTF_8)

    final def serializedSize(s: String): Int =
      bytesSerializer.serializedSize(s.getBytes(StandardCharsets.UTF_8)) + 4

  }

  /**
    * Create a new OHC cache
    */
  final def apply[F[_]: Async]()(implicit config: CacheConfig): OhcCache[F] =
    new OhcCache(
      OHCacheBuilder
        .newBuilder()
        .keySerializer(stringSerializer)
        .valueSerializer(bytesSerializer)
        .timeouts(true)
        .build()
    )

  /**
    * Create a new cache utilizing the given underlying OHC cache.
    *
    * @param underlying a OHC cache configured with OHCacheBuilder.timeouts(true)
    */
  final def apply[F[_]: Async](underlying: OHCache[String, Array[Byte]])(implicit config: CacheConfig): OhcCache[F] =
    new OhcCache(underlying)

}

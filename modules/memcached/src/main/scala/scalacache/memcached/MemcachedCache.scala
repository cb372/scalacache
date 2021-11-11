package scalacache.memcached

import cats.effect.Async
import net.spy.memcached.internal.{GetCompletionListener, GetFuture, OperationCompletionListener, OperationFuture}
import net.spy.memcached.ops.StatusCode
import net.spy.memcached.{AddrUtil, BinaryConnectionFactory, MemcachedClient}
import scalacache.AbstractCache
import scalacache.logging.Logger
import scalacache.serialization.binary.BinaryCodec

import scala.concurrent.duration.Duration
import scala.language.higherKinds
import scala.util.Success
import scala.util.control.NonFatal

class MemcachedException(message: String) extends Exception(message)

/** Wrapper around spymemcached
  */
class MemcachedCache[F[_]: Async, V](
    val client: MemcachedClient,
    val keySanitizer: MemcachedKeySanitizer = ReplaceAndTruncateSanitizer()
)(implicit val codec: BinaryCodec[V])
    extends AbstractCache[F, String, V]
    with MemcachedTTLConverter {

  protected def F: Async[F] = Async[F]

  override protected final val logger =
    Logger.getLogger[F](getClass.getName)

  override protected def doGet(key: String): F[Option[V]] = {
    F.async_ { cb =>
      val f = client.asyncGet(keySanitizer.toValidMemcachedKey(key))
      f.addListener(new GetCompletionListener {
        def onComplete(g: GetFuture[_]): Unit = {
          if (g.getStatus.isSuccess) {
            try {
              val bytes = g.get()
              val value = codec.decode(bytes.asInstanceOf[Array[Byte]]).right.map(Some(_))
              cb(value)
            } catch {
              case NonFatal(e) => cb(Left(e))
            }
          } else {
            g.getStatus.getStatusCode match {
              case StatusCode.ERR_NOT_FOUND => cb(Right(None))
              case _                        => cb(Left(new MemcachedException(g.getStatus.getMessage)))
            }

          }
        }
      })
    }
  }

  override protected def doPut(key: String, value: V, ttl: Option[Duration]): F[Unit] = {
    F.async_ { cb =>
      val valueToSend = codec.encode(value)
      val f           = client.set(keySanitizer.toValidMemcachedKey(key), toMemcachedExpiry(ttl), valueToSend)
      f.addListener(new OperationCompletionListener {
        def onComplete(g: OperationFuture[_]): Unit = {
          if (g.getStatus.isSuccess) {
            logCachePut(key, ttl)
            cb(Right(()))
          } else {
            cb(Left(new MemcachedException(g.getStatus.getMessage)))
          }
          Success(())
        }
      })
    }
  }

  override protected def doRemove(key: String): F[Unit] = {
    F.async_ { cb =>
      val f = client.delete(key)
      f.addListener(new OperationCompletionListener {
        def onComplete(g: OperationFuture[_]): Unit = {
          if (g.getStatus.isSuccess)
            cb(Right(()))
          else
            cb(Left(new MemcachedException(g.getStatus.getMessage)))
        }
      })
    }
  }

  override protected def doRemoveAll: F[Unit] = {
    F.async_ { cb =>
      val f = client.flush()
      f.addListener(new OperationCompletionListener {
        def onComplete(g: OperationFuture[_]): Unit = {
          if (g.getStatus.isSuccess)
            cb(Right(()))
          else
            cb(Left(new MemcachedException(g.getStatus.getMessage)))
        }
      })
    }
  }

  override def close: F[Unit] = F.delay(client.shutdown())

}

object MemcachedCache {

  /** Create a Memcached client connecting to localhost:11211 and use it for caching
    */
  def apply[F[_]: Async, V](implicit codec: BinaryCodec[V]): MemcachedCache[F, V] =
    apply("localhost:11211")

  /** Create a Memcached client connecting to the given host(s) and use it for caching
    *
    * @param addressString
    *   Address string, with addresses separated by spaces, e.g. "host1:11211 host2:22322"
    */
  def apply[F[_]: Async, V](
      addressString: String
  )(implicit codec: BinaryCodec[V]): MemcachedCache[F, V] =
    apply(new MemcachedClient(new BinaryConnectionFactory(), AddrUtil.getAddresses(addressString)))

  /** Create a cache that uses the given Memcached client
    *
    * @param client
    *   Memcached client
    */
  def apply[F[_]: Async, V](
      client: MemcachedClient
  )(implicit codec: BinaryCodec[V]): MemcachedCache[F, V] =
    new MemcachedCache[F, V](client)

}

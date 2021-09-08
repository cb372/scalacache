package scalacache

import scala.concurrent.duration.Duration

import scala.language.higherKinds
import cats.Monad
import cats.implicits._
import cats.MonadError
import cats.effect.Sync
import cats.Applicative

/** An abstract implementation of [[Cache]] that takes care of some things that are common across all concrete
  * implementations.
  *
  * If you are writing a cache implementation, you probably want to extend this trait rather than extending [[Cache]]
  * directly.
  *
  * @tparam K
  *   The type of keys stored in the cache.
  * @tparam V
  *   The type of values stored in the cache.
  */
trait AbstractCache[F[_], K, V] extends Cache[F, K, V] with LoggingSupport[F, K] {

  protected implicit def F: Sync[F]
  // GET

  protected def doGet(key: K): F[Option[V]]

  private def checkFlagsAndGet(key: K)(implicit flags: Flags): F[Option[V]] = {
    if (flags.readsEnabled) {
      doGet(key)
    } else
      logger
        .ifDebugEnabled {
          logger.debug(s"Skipping cache GET because cache reads are disabled. Key: $key")
        }
        .as(None)
  }

  final override def get(key: K)(implicit flags: Flags): F[Option[V]] = {
    checkFlagsAndGet(key)
  }

  // PUT

  protected def doPut(key: K, value: V, ttl: Option[Duration]): F[Unit]

  private def checkFlagsAndPut(key: K, value: V, ttl: Option[Duration])(implicit
      flags: Flags
  ): F[Unit] = {
    if (flags.writesEnabled) {
      doPut(key, value, ttl)
    } else
      logger.ifDebugEnabled {
        logger.debug(s"Skipping cache PUT because cache writes are disabled. Key: $key")
      }.void
  }

  final override def put(
      key: K
  )(value: V, ttl: Option[Duration])(implicit flags: Flags): F[Unit] = {
    val finiteTtl = ttl.filter(_.isFinite) // discard Duration.Inf, Duration.Undefined
    checkFlagsAndPut(key, value, finiteTtl)
  }

  // REMOVE

  protected def doRemove(key: K): F[Unit]

  final override def remove(key: K): F[Unit] =
    doRemove(key)

  // REMOVE ALL

  protected def doRemoveAll: F[Unit]

  final override def removeAll: F[Unit] =
    doRemoveAll

  // CACHING

  final override def caching(
      key: K
  )(ttl: Option[Duration] = None)(f: => V)(implicit flags: Flags): F[V] = cachingF(key)(ttl)(Sync[F].delay(f))

  override def cachingF(
      key: K
  )(ttl: Option[Duration] = None)(f: F[V])(implicit flags: Flags): F[V] = {
    checkFlagsAndGet(key)
      .handleErrorWith { e =>
        logger
          .ifWarnEnabled(logger.warn(s"Failed to read from cache. Key = $key", e))
          .as(None)
      }
      .flatMap {
        case Some(valueFromCache) => F.pure(valueFromCache)
        case None =>
          f.flatTap { calculatedValue =>
            checkFlagsAndPut(key, calculatedValue, ttl)
              .handleError { e =>
                logger.ifWarnEnabled {
                  logger.warn(s"Failed to write to cache. Key = $key", e)
                }
              }
          }
      }
  }
}

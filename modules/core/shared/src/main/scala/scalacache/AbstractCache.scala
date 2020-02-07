package scalacache

import scala.concurrent.duration.Duration

import scala.language.higherKinds
import cats.Monad
import cats.implicits._
import cats.MonadError
import cats.Defer

/**
  * An abstract implementation of [[CacheAlg]] that takes care of
  * some things that are common across all concrete implementations.
  *
  * If you are writing a cache implementation, you probably want to
  * extend this trait rather than extending [[CacheAlg]] directly.
  *
  * @tparam V The value of types stored in the cache.
  */
trait AbstractCache[F[_], V] extends Cache[F, V] with LoggingSupport {

  protected implicit def F: MonadError[F, Throwable]
  protected implicit def Defer: Defer[F]
  // GET

  protected def doGet(key: String): F[Option[V]]

  private def checkFlagsAndGet(key: String)(implicit flags: Flags): F[Option[V]] = {
    if (flags.readsEnabled) {
      doGet(key)
    } else {
      if (logger.isDebugEnabled) {
        logger.debug(s"Skipping cache GET because cache reads are disabled. Key: $key")
      }
      F.pure(None)
    }
  }

  final override def get(keyParts: Any*)(implicit flags: Flags): F[Option[V]] = {
    val key = toKey(keyParts: _*)
    checkFlagsAndGet(key)
  }

  // PUT

  protected def doPut(key: String, value: V, ttl: Option[Duration]): F[Any]

  private def checkFlagsAndPut(key: String, value: V, ttl: Option[Duration])(
      implicit
      flags: Flags
  ): F[Any] = {
    if (flags.writesEnabled) {
      doPut(key, value, ttl)
    } else {
      if (logger.isDebugEnabled) {
        logger.debug(s"Skipping cache PUT because cache writes are disabled. Key: $key")
      }
      F.pure(())
    }
  }

  final override def put(
      keyParts: Any*
  )(value: V, ttl: Option[Duration])(implicit flags: Flags): F[Any] = {
    val key       = toKey(keyParts: _*)
    val finiteTtl = ttl.filter(_.isFinite) // discard Duration.Inf, Duration.Undefined
    checkFlagsAndPut(key, value, finiteTtl)
  }

  // REMOVE

  protected def doRemove(key: String): F[Any]

  final override def remove(keyParts: Any*): F[Any] =
    doRemove(toKey(keyParts: _*))

  // REMOVE ALL

  protected def doRemoveAll: F[Any]

  final override def removeAll: F[Any] =
    doRemoveAll

  // CACHING

  final override def caching(
      keyParts: Any*
  )(ttl: Option[Duration] = None)(f: => V)(implicit flags: Flags): F[V] = {
    val key = toKey(keyParts: _*)
    _caching(key, ttl, f)
  }

  override def cachingF(
      keyParts: Any*
  )(ttl: Option[Duration] = None)(f: F[V])(implicit flags: Flags): F[V] = {
    val key = toKey(keyParts: _*)
    _cachingF(key, ttl, f)
  }

  // MEMOIZE

  override def cachingForMemoize(
      baseKey: String
  )(ttl: Option[Duration] = None)(f: => V)(implicit flags: Flags): F[V] = {
    val key = config.cacheKeyBuilder.stringToCacheKey(baseKey)
    _caching(key, ttl, f)
  }

  override def cachingForMemoizeF(
      baseKey: String
  )(ttl: Option[Duration])(f: F[V])(implicit flags: Flags): F[V] = {
    val key = config.cacheKeyBuilder.stringToCacheKey(baseKey)
    _cachingF(key, ttl, f)
  }

  private def _caching(key: String, ttl: Option[Duration], f: => V)(
      implicit
      flags: Flags
  ): F[V] = _cachingF(key, ttl, Defer.defer(F.pure(f)))

  private def _cachingF(key: String, ttl: Option[Duration], f: => F[V])(
      implicit
      flags: Flags
  ): F[V] = {
    checkFlagsAndGet(key)
      .handleError { e =>
        if (logger.isWarnEnabled) {
          logger.warn(s"Failed to read from cache. Key = $key", e)
        }
        None
      }
      .flatMap {
        case Some(valueFromCache) => F.pure(valueFromCache)
        case None =>
          f.flatTap { calculatedValue =>
            checkFlagsAndPut(key, calculatedValue, ttl)
              .handleError { e =>
                if (logger.isWarnEnabled) {
                  logger.warn(s"Failed to write to cache. Key = $key", e)
                }
              }
          }
      }
  }

  private def toKey(keyParts: Any*): String =
    config.cacheKeyBuilder.toCacheKey(keyParts)

}

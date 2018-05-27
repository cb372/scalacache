package scalacache

import scalacache.serialization.Codec

import scala.concurrent.duration.Duration
import scala.language.higherKinds

/**
  * An abstract implementation of [[CacheAlg]] that takes care of
  * some things that are common across all concrete implementations.
  *
  * If you are writing a cache implementation, you probably want to
  * extend this trait rather than extending [[CacheAlg]] directly.
  *
  * @tparam F The type of container in which the result will be wrapped. This is decided by the mode.
  */
abstract class AbstractCache[F[_]](implicit mode: Mode[F]) extends Cache[F] with LoggingSupport {

  // GET

  protected def doGet[V](key: String)(implicit codec: Codec[V]): F[Option[V]]

  final override def get[V: Codec](keyParts: Any*)(implicit flags: Flags): F[Option[V]] = {
    val key = toKey(keyParts: _*)
    checkFlagsAndGet(key)
  }

  // PUT

  protected def doPut[V](key: String, value: V, ttl: Option[Duration])(implicit codec: Codec[V]): F[Unit]

  final override def put[V: Codec](keyParts: Any*)(value: V, ttl: Option[Duration])(implicit flags: Flags): F[Unit] = {
    val key = toKey(keyParts: _*)
    val finiteTtl = ttl.filter(_.isFinite()) // discard Duration.Inf, Duration.Undefined
    checkFlagsAndPut(key, value, finiteTtl)
  }

  // REMOVE

  protected def doRemove(key: String): F[Any]

  final override def remove(keyParts: Any*): F[Any] = doRemove(toKey(keyParts: _*))

  // REMOVE ALL

  protected def doRemoveAll(): F[Any]

  final override def removeAll(): F[Any] = doRemoveAll()

  // CACHING

  final override def caching[V: Codec](keyParts: Any*)(ttl: Option[Duration] = None)(f: => V)(
      implicit flags: Flags): F[V] = {
    val key = toKey(keyParts: _*)
    _caching(key, ttl, f)
  }

  final override def cachingF[V: Codec](keyParts: Any*)(ttl: Option[Duration] = None)(f: => F[V])(
      implicit flags: Flags): F[V] = {
    val key = toKey(keyParts: _*)
    _cachingF(key, ttl, f)
  }

  // MEMOIZE

  final override def cachingForMemoize[V: Codec](baseKey: String)(ttl: Option[Duration] = None)(f: => V)(
      implicit flags: Flags): F[V] = {
    val key = config.cacheKeyBuilder.stringToCacheKey(baseKey)
    _caching(key, ttl, f)
  }

  final override def cachingForMemoizeF[V: Codec](baseKey: String)(ttl: Option[Duration])(f: => F[V])(
      implicit flags: Flags): F[V] = {
    val key = config.cacheKeyBuilder.stringToCacheKey(baseKey)
    _cachingF(key, ttl, f)
  }

  private final def _caching[V: Codec](key: String, ttl: Option[Duration], f: => V)(implicit flags: Flags): F[V] = {
    import mode._

    M.flatMap {
      M.handleNonFatal(checkFlagsAndGet(key)) { e =>
        if (logger.isWarnEnabled) {
          logger.warn(s"Failed to read from cache. Key = $key", e)
        }
        None
      }
    } {
      case Some(valueFromCache) =>
        M.pure(valueFromCache)
      case None =>
        val calculatedValue = f
        M.map {
          M.handleNonFatal {
            checkFlagsAndPut(key, calculatedValue, ttl)
          } { e =>
            if (logger.isWarnEnabled) {
              logger.warn(s"Failed to write to cache. Key = $key", e)
            }
          }
        }(_ => calculatedValue)
    }
  }

  private final def _cachingF[V: Codec](key: String, ttl: Option[Duration], f: => F[V])(implicit flags: Flags): F[V] = {
    import mode._

    M.flatMap {
      M.handleNonFatal(checkFlagsAndGet(key)) { e =>
        if (logger.isWarnEnabled) {
          logger.warn(s"Failed to read from cache. Key = $key", e)
        }
        None
      }
    } {
      case Some(valueFromCache) =>
        M.pure(valueFromCache)
      case None =>
        M.flatMap(f) { calculatedValue =>
          M.map {
            M.handleNonFatal {
              checkFlagsAndPut(key, calculatedValue, ttl)
            } { e =>
              if (logger.isWarnEnabled) {
                logger.warn(s"Failed to write to cache. Key = $key", e)
              }
            }
          }(_ => calculatedValue)
        }
    }
  }

  private final def toKey(keyParts: Any*): String = config.cacheKeyBuilder.toCacheKey(keyParts)

  private final def checkFlagsAndGet[V: Codec](key: String)(implicit flags: Flags): F[Option[V]] =
    if (flags.readsEnabled) doGet(key)
    else {
      if (logger.isDebugEnabled) {
        logger.debug(s"Skipping cache GET because cache reads are disabled. Key: $key")
      }
      mode.M.pure(None)
    }

  private def checkFlagsAndPut[V](
      key: String,
      value: V,
      ttl: Option[Duration]
  )(implicit flags: Flags, codec: Codec[V]): F[Unit] =
    if (flags.writesEnabled) doPut(key, value, ttl)
    else {
      if (logger.isDebugEnabled) {
        logger.debug(s"Skipping cache PUT because cache writes are disabled. Key: $key")
      }
      mode.M.pure(())
    }

}

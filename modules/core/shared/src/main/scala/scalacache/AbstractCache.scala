package scalacache

import scala.concurrent.duration.Duration

import scala.language.higherKinds

/**
  * An abstract implementation of [[CacheAlg]] that takes care of
  * some things that are common across all concrete implementations.
  *
  * If you are writing a cache implementation, you probably want to
  * extend this trait rather than extending [[CacheAlg]] directly.
  *
  * @tparam V The value of types stored in the cache.
  */
trait AbstractCache[V] extends Cache[V] with LoggingSupport {

  // GET

  protected def doGet[F[_]](key: String)(implicit mode: Mode[F]): F[Option[V]]

  private def checkFlagsAndGet[F[_]](key: String)(implicit mode: Mode[F], flags: Flags): F[Option[V]] = {
    if (flags.readsEnabled) {
      doGet(key)
    } else {
      if (logger.isDebugEnabled) {
        logger.debug(s"Skipping cache GET because cache reads are disabled. Key: $key")
      }
      mode.M.pure(None)
    }
  }

  final override def get[F[_]](keyParts: Any*)(implicit mode: Mode[F], flags: Flags): F[Option[V]] = {
    val key = toKey(keyParts: _*)
    checkFlagsAndGet(key)
  }

  // PUT

  protected def doPut[F[_]](key: String, value: V, ttl: Option[Duration])(implicit mode: Mode[F]): F[Any]

  private def checkFlagsAndPut[F[_]](key: String, value: V, ttl: Option[Duration])(implicit mode: Mode[F],
                                                                                   flags: Flags): F[Any] = {
    if (flags.writesEnabled) {
      doPut(key, value, ttl)
    } else {
      if (logger.isDebugEnabled) {
        logger.debug(s"Skipping cache PUT because cache writes are disabled. Key: $key")
      }
      mode.M.pure(())
    }
  }

  final override def put[F[_]](keyParts: Any*)(value: V, ttl: Option[Duration])(implicit mode: Mode[F],
                                                                                flags: Flags): F[Any] = {
    val key = toKey(keyParts: _*)
    val finiteTtl = ttl.filter(_.isFinite()) // discard Duration.Inf, Duration.Undefined
    checkFlagsAndPut(key, value, finiteTtl)
  }

  // REMOVE

  protected def doRemove[F[_]](key: String)(implicit mode: Mode[F]): F[Any]

  final override def remove[F[_]](keyParts: Any*)(implicit mode: Mode[F]): F[Any] =
    doRemove(toKey(keyParts: _*))

  // REMOVE ALL

  protected def doRemoveAll[F[_]]()(implicit mode: Mode[F]): F[Any]

  final override def removeAll[F[_]]()(implicit mode: Mode[F]): F[Any] =
    doRemoveAll()

  // CACHING

  final override def caching[F[_]](keyParts: Any*)(ttl: Option[Duration] = None)(f: => V)(implicit mode: Mode[F],
                                                                                          flags: Flags): F[V] = {
    val key = toKey(keyParts: _*)
    _caching(key, ttl, f)
  }

  override def cachingF[F[_]](keyParts: Any*)(ttl: Option[Duration] = None)(f: => F[V])(implicit mode: Mode[F],
                                                                                        flags: Flags): F[V] = {
    val key = toKey(keyParts: _*)
    _cachingF(key, ttl, f)
  }

  // MEMOIZE

  override def cachingForMemoize[F[_]](baseKey: String)(ttl: Option[Duration] = None)(f: => V)(implicit mode: Mode[F],
                                                                                               flags: Flags): F[V] = {
    val key = config.cacheKeyBuilder.stringToCacheKey(baseKey)
    _caching(key, ttl, f)
  }

  override def cachingForMemoizeF[F[_]](baseKey: String)(ttl: Option[Duration])(f: => F[V])(implicit mode: Mode[F],
                                                                                            flags: Flags): F[V] = {
    val key = config.cacheKeyBuilder.stringToCacheKey(baseKey)
    _cachingF(key, ttl, f)
  }

  private def _caching[F[_]](key: String, ttl: Option[Duration], f: => V)(implicit mode: Mode[F],
                                                                          flags: Flags): F[V] = {
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

  private def _cachingF[F[_]](key: String, ttl: Option[Duration], f: => F[V])(implicit mode: Mode[F],
                                                                              flags: Flags): F[V] = {
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

  private def toKey(keyParts: Any*): String =
    config.cacheKeyBuilder.toCacheKey(keyParts)

}

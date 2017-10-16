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
  * See the comment on [[CacheAlg]] for more information on the type params.
  *
  * @tparam V The value of types stored in the cache.
  * @tparam S A type class describing what operations a container `F` must support in order to be used with this cache.
  */
trait AbstractCache[V, S[F[_]] <: Sync[F]] extends CacheAlg[V, S] {

  protected def config: CacheConfig

  // GET

  protected def doGet[F[_]](key: String)(implicit mode: Mode[F, S], flags: Flags): F[Option[V]]

  final override def get[F[_]](keyParts: Any*)(implicit mode: Mode[F, S], flags: Flags): F[Option[V]] =
    doGet(toKey(keyParts: _*))

  // PUT

  protected def doPut[F[_]](key: String, value: V, ttl: Option[Duration])(implicit mode: Mode[F, S],
                                                                          flags: Flags): F[Any]

  final override def put[F[_]](keyParts: Any*)(value: V, ttl: Option[Duration])(implicit mode: Mode[F, S],
                                                                                flags: Flags): F[Any] =
    doPut(toKey(keyParts: _*), value, ttl)

  // REMOVE

  protected def doRemove[F[_]](key: String)(implicit mode: Mode[F, S]): F[Any]

  final override def remove[F[_]](keyParts: Any*)(implicit mode: Mode[F, S]): F[Any] =
    doRemove(toKey(keyParts: _*))

  // REMOVE ALL

  protected def doRemoveAll[F[_]]()(implicit mode: Mode[F, S]): F[Any]

  final override def removeAll[F[_]]()(implicit mode: Mode[F, S]): F[Any] =
    doRemoveAll()

  // CACHING

  final override def caching[F[_]](keyParts: Any*)(ttl: Option[Duration] = None)(f: => V)(implicit mode: Mode[F, S],
                                                                                          flags: Flags): F[V] = {
    val key = toKey(keyParts)

    import mode._
    M.flatMap(doGet(key)) {
      case Some(valueFromCache) =>
        M.pure(valueFromCache)
      case None =>
        val calculatedValue = f
        M.map(doPut(key, calculatedValue, ttl))(_ => calculatedValue)
    }
  }

  override def cachingF[F[_]](keyParts: Any*)(ttl: Option[Duration] = None)(f: => F[V])(implicit mode: Mode[F, S],
                                                                                        flags: Flags): F[V] = {
    val key = toKey(keyParts)

    import mode._
    M.flatMap(doGet(key)) {
      case Some(valueFromCache) =>
        M.pure(valueFromCache)
      case None =>
        M.flatMap(f) { calculatedValue =>
          M.map(doPut(key, calculatedValue, ttl))(_ => calculatedValue)
        }
    }
  }

  // MEMOIZE

  override private[scalacache] def cachingForMemoize[F[_]](baseKey: String)(ttl: Option[Duration] = None)(
      f: => V)(implicit mode: Mode[F, S], flags: Flags): F[V] = {
    import mode._
    val key = config.cacheKeyBuilder.stringToCacheKey(baseKey)
    M.flatMap(doGet(key)) {
      case Some(valueFromCache) =>
        M.pure(valueFromCache)
      case None =>
        val calculatedValue = f
        M.map(doPut(key, calculatedValue, ttl))(_ => calculatedValue)
    }
  }

  override private[scalacache] def cachingForMemoizeF[F[_]](baseKey: String)(ttl: Option[Duration])(
      f: => F[V])(implicit mode: Mode[F, S], flags: Flags): F[V] = {
    import mode._
    val key = config.cacheKeyBuilder.stringToCacheKey(baseKey)
    M.flatMap(doGet(key)) {
      case Some(valueFromCache) =>
        M.pure(valueFromCache)
      case None =>
        M.flatMap(f) { calculatedValue =>
          M.map(doPut(key, calculatedValue, ttl))(_ => calculatedValue)
        }
    }
  }

  private def toKey(keyParts: Any*): String =
    config.cacheKeyBuilder.toCacheKey(keyParts)

}

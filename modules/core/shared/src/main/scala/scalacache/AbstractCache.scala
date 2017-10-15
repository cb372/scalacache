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
  * @tparam S A type class describing what operations a container `E` must support in order to be used with this cache.
  */
trait AbstractCache[V, S[E[_]] <: Sync[E]] extends CacheAlg[V, S] {

  protected def config: CacheConfig

  // GET

  protected def doGet[E[_], F[_]](key: String)(implicit mode: Mode[E, F, S], flags: Flags): E[Option[V]]

  override def get[E[_], F[_]](keyParts: Any*)(implicit mode: Mode[E, F, S], flags: Flags): F[Option[V]] =
    mode.transform(doGet(toKey(keyParts: _*)))

  // PUT

  protected def doPut[E[_], F[_]](key: String, value: V, ttl: Option[Duration])(implicit mode: Mode[E, F, S],
                                                                                flags: Flags): E[Any]

  override def put[E[_], F[_]](keyParts: Any*)(value: V, ttl: Option[Duration])(implicit mode: Mode[E, F, S],
                                                                                flags: Flags): F[Any] =
    mode.transform(doPut(toKey(keyParts: _*), value, ttl))

  // REMOVE

  protected def doRemove[E[_], F[_]](key: String)(implicit mode: Mode[E, F, S]): E[Any]

  override def remove[E[_], F[_]](keyParts: Any*)(implicit mode: Mode[E, F, S]): F[Any] =
    mode.transform(doRemove(toKey(keyParts: _*)))

  // REMOVE ALL

  protected def doRemoveAll[E[_], F[_]]()(implicit mode: Mode[E, F, S]): E[Any]

  override def removeAll[E[_], F[_]]()(implicit mode: Mode[E, F, S]): F[Any] =
    mode.transform(doRemoveAll())

  // CACHING

  override def caching[E[_], F[_]](keyParts: Any*)(ttl: Option[Duration] = None)(f: => V)(implicit mode: Mode[E, F, S],
                                                                                          flags: Flags): F[V] = {
    val key = toKey(keyParts)

    import mode._
    M.flatMap(mode.transform(doGet(key))) {
      case Some(valueFromCache) =>
        M.pure(valueFromCache)
      case None =>
        val calculatedValue = f
        M.map(mode.transform(doPut(key, calculatedValue, ttl)))(_ => calculatedValue)
    }
  }

  override def cachingE[E[_], F[_]](keyParts: Any*)(ttl: Option[Duration] = None)(
      f: => E[V])(implicit mode: Mode[E, F, S], flags: Flags): F[V] = {
    val key = toKey(keyParts)

    import mode._
    M.flatMap(transform(doGet(key))) {
      case Some(valueFromCache) =>
        M.pure(valueFromCache)
      case None =>
        M.flatMap(transform(f)) { calculatedValue =>
          M.map(transform(doPut(key, calculatedValue, ttl)))(_ => calculatedValue)
        }
    }
  }

  // MEMOIZE

  override private[scalacache] def cachingForMemoize[E[_], F[_]](baseKey: String)(ttl: Option[Duration] = None)(
      f: => V)(implicit mode: Mode[E, F, S], flags: Flags): F[V] = {
    import mode._
    val key = config.cacheKeyBuilder.stringToCacheKey(baseKey)
    M.flatMap(transform(doGet(key))) {
      case Some(valueFromCache) =>
        M.pure(valueFromCache)
      case None =>
        val calculatedValue = f
        M.map(transform(doPut(key, calculatedValue, ttl)))(_ => calculatedValue)
    }
  }

  override private[scalacache] def cachingForMemoizeE[E[_], F[_]](baseKey: String)(ttl: Option[Duration])(
      f: => E[V])(implicit mode: Mode[E, F, S], flags: Flags): F[V] = {
    import mode._
    val key = config.cacheKeyBuilder.stringToCacheKey(baseKey)
    M.flatMap(transform(doGet(key))) {
      case Some(valueFromCache) =>
        M.pure(valueFromCache)
      case None =>
        M.flatMap(transform(f)) { calculatedValue =>
          M.map(transform(doPut(key, calculatedValue, ttl)))(_ => calculatedValue)
        }
    }
  }

  private def toKey(keyParts: Any*): String =
    config.cacheKeyBuilder.toCacheKey(keyParts)

}

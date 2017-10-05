package scalacache

import scala.concurrent.duration.Duration

import scala.language.higherKinds

trait AbstractCache[V, S[F[_]] <: Sync[F]] extends CacheAlg[V, S] {

  protected def config: CacheConfig

  // GET

  protected def doGet[F[_], G[_]](key: String)(implicit mode: Mode[F, G, S], flags: Flags): F[Option[V]]

  override def get[F[_], G[_]](keyParts: Any*)(implicit mode: Mode[F, G, S], flags: Flags): G[Option[V]] =
    mode.transform(doGet(toKey(keyParts: _*)))

  // PUT

  protected def doPut[F[_], G[_]](key: String, value: V, ttl: Option[Duration])(implicit mode: Mode[F, G, S],
                                                                                flags: Flags): F[Any]

  override def put[F[_], G[_]](keyParts: Any*)(value: V, ttl: Option[Duration])(implicit mode: Mode[F, G, S],
                                                                                flags: Flags): G[Any] =
    mode.transform(doPut(toKey(keyParts: _*), value, ttl))

  // REMOVE

  protected def doRemove[F[_], G[_]](key: String)(implicit mode: Mode[F, G, S]): F[Any]

  override def remove[F[_], G[_]](keyParts: Any*)(implicit mode: Mode[F, G, S]): G[Any] =
    mode.transform(doRemove(toKey(keyParts: _*)))

  // REMOVE ALL

  protected def doRemoveAll[F[_], G[_]]()(implicit mode: Mode[F, G, S]): F[Any]

  override def removeAll[F[_], G[_]]()(implicit mode: Mode[F, G, S]): G[Any] =
    mode.transform(doRemoveAll())

  // CACHING

  override def caching[F[_], G[_]](keyParts: Any*)(ttl: Option[Duration] = None)(f: => V)(implicit mode: Mode[F, G, S],
                                                                                          flags: Flags): G[V] = {
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

  override def cachingF[F[_], G[_]](keyParts: Any*)(ttl: Option[Duration] = None)(
      f: => F[V])(implicit mode: Mode[F, G, S], flags: Flags): G[V] = {
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

  override private[scalacache] def cachingForMemoize[F[_], G[_]](baseKey: String)(ttl: Option[Duration] = None)(
      f: => V)(implicit mode: Mode[F, G, S], flags: Flags): G[V] = {
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

  override private[scalacache] def cachingForMemoizeF[F[_], G[_]](baseKey: String)(ttl: Option[Duration])(
      f: => F[V])(implicit mode: Mode[F, G, S], flags: Flags): G[V] = {
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

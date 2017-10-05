package scalacache

import scala.concurrent.duration.Duration

import scala.language.higherKinds

trait AbstractCache[V, S[F[_]] <: Sync[F]] extends CacheAlg[V, S] {

  protected def config: CacheConfig

  protected def getWithKey[F[_], G[_]](key: String)(implicit mode: Mode[F, G, S], flags: Flags): G[Option[V]]

  protected def putWithKey[F[_], G[_]](key: String, value: V, ttl: Option[Duration])(implicit mode: Mode[F, G, S],
                                                                                     flags: Flags): G[Unit]

  override def get[F[_], G[_]](keyParts: Any*)(implicit mode: Mode[F, G, S], flags: Flags): G[Option[V]] =
    getWithKey(toKey(keyParts: _*))

  override def put[F[_], G[_]](keyParts: Any*)(value: V, ttl: Option[Duration])(implicit mode: Mode[F, G, S],
                                                                                flags: Flags): G[Unit] =
    putWithKey(toKey(keyParts: _*), value, ttl)

  override def caching[F[_], G[_]](keyParts: Any*)(ttl: Option[Duration] = None)(f: => V)(implicit mode: Mode[F, G, S],
                                                                                          flags: Flags): G[V] = {
    val key = toKey(keyParts)

    import mode._
    M.flatMap(getWithKey(key)) {
      case Some(valueFromCache) =>
        M.pure(valueFromCache)
      case None =>
        val calculatedValue = f
        M.map(putWithKey(key, calculatedValue, ttl))(_ => calculatedValue)
    }
  }

  override def cachingF[F[_], G[_]](keyParts: Any*)(ttl: Option[Duration] = None)(
      f: => F[V])(implicit mode: Mode[F, G, S], flags: Flags): G[V] = {
    val key = toKey(keyParts)

    import mode._
    M.flatMap(getWithKey(key)) {
      case Some(valueFromCache) =>
        M.pure(valueFromCache)
      case None =>
        M.flatMap(transform(f)) { calculatedValue =>
          M.map(putWithKey(key, calculatedValue, ttl))(_ => calculatedValue)
        }
    }
  }

  override private[scalacache] def cachingForMemoize[F[_], G[_]](baseKey: String)(ttl: Option[Duration] = None)(
      f: => V)(implicit mode: Mode[F, G, S], flags: Flags): G[V] = {
    import mode._
    val key = config.cacheKeyBuilder.stringToCacheKey(baseKey)
    M.flatMap(getWithKey(key)) {
      case Some(valueFromCache) =>
        M.pure(valueFromCache)
      case None =>
        val calculatedValue = f
        M.map(putWithKey(key, calculatedValue, ttl))(_ => calculatedValue)
    }
  }

  override private[scalacache] def cachingForMemoizeF[F[_], G[_]](baseKey: String)(ttl: Option[Duration])(
      f: => F[V])(implicit mode: Mode[F, G, S], flags: Flags): G[V] = {
    import mode._
    val key = config.cacheKeyBuilder.stringToCacheKey(baseKey)
    M.flatMap(getWithKey(key)) {
      case Some(valueFromCache) =>
        M.pure(valueFromCache)
      case None =>
        M.flatMap(transform(f)) { calculatedValue =>
          M.map(putWithKey(key, calculatedValue, ttl))(_ => calculatedValue)
        }
    }
  }

  private def toKey(keyParts: Any*): String =
    config.cacheKeyBuilder.toCacheKey(keyParts)

}

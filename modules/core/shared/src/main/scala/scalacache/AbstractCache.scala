package scalacache

import scala.concurrent.duration.Duration

import scala.language.higherKinds

trait AbstractCache[V, M[F[_]] <: Sync[F]] extends CacheAlg[V, M] {

  protected def config: CacheConfig

  protected def getWithKey[F[_]](key: String)(implicit mode: Mode[F, M], flags: Flags): F[Option[V]]

  protected def putWithKey[F[_]](key: String, value: V, ttl: Option[Duration])(implicit mode: Mode[F, M],
                                                                               flags: Flags): F[Unit]

  override def get[F[_]](keyParts: Any*)(implicit mode: Mode[F, M], flags: Flags): F[Option[V]] =
    getWithKey(toKey(keyParts: _*))

  override def put[F[_]](keyParts: Any*)(value: V, ttl: Option[Duration])(implicit mode: Mode[F, M],
                                                                          flags: Flags): F[Unit] =
    putWithKey(toKey(keyParts: _*), value, ttl)

  override def caching[F[_]](keyParts: Any*)(ttl: Option[Duration] = None)(f: => V)(implicit mode: Mode[F, M],
                                                                                    flags: Flags): F[V] = {
    import mode._
    M.flatMap(get(keyParts: _*)) {
      case Some(valueFromCache) =>
        M.pure(valueFromCache)
      case None =>
        val calculatedValue = f
        M.map(put(keyParts: _*)(calculatedValue, ttl))(_ => calculatedValue)
    }
  }

  override def cachingF[F[_]](keyParts: Any*)(ttl: Option[Duration] = None)(f: => F[V])(implicit mode: Mode[F, M],
                                                                                        flags: Flags): F[V] = {
    import mode._
    M.flatMap(get(keyParts: _*)) {
      case Some(valueFromCache) =>
        M.pure(valueFromCache)
      case None =>
        M.flatMap(f) { calculatedValue =>
          M.map(put(keyParts: _*)(calculatedValue, ttl))(_ => calculatedValue)
        }
    }
  }

  override private[scalacache] def cachingForMemoize[F[_]](baseKey: String)(ttl: Option[Duration] = None)(
      f: => V)(implicit mode: Mode[F, M], flags: Flags): F[V] = {
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

  override private[scalacache] def cachingForMemoizeF[F[_]](baseKey: String)(ttl: Option[Duration])(
      f: => F[V])(implicit mode: Mode[F, M], flags: Flags): F[V] = {
    import mode._
    val key = config.cacheKeyBuilder.stringToCacheKey(baseKey)
    M.flatMap(getWithKey(key)) {
      case Some(valueFromCache) =>
        M.pure(valueFromCache)
      case None =>
        M.flatMap(f) { calculatedValue =>
          M.map(putWithKey(key, calculatedValue, ttl))(_ => calculatedValue)
        }
    }
  }

  private def toKey(keyParts: Any*): String =
    config.cacheKeyBuilder.toCacheKey(keyParts)

}

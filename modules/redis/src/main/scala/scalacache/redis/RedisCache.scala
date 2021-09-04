package scalacache.redis

import redis.clients.jedis._

import scala.language.higherKinds
import scalacache.CacheConfig
import scalacache.serialization.Codec
import cats.effect.{MonadCancel, MonadCancelThrow, Resource, Sync}

/** Thin wrapper around Jedis
  */
class RedisCache[F[_]: Sync: MonadCancelThrow, V](val jedisPool: JedisPool)(implicit
    val config: CacheConfig,
    val codec: Codec[V]
) extends RedisCacheBase[F, V] {

  protected def F: Sync[F]                             = Sync[F]
  protected def MonadCancelThrowF: MonadCancelThrow[F] = MonadCancel[F, Throwable]
  type JClient = Jedis

  protected val doRemoveAll: F[Unit] = withJedis { jedis =>
    F.delay(jedis.flushDB())
  }
}

object RedisCache {

  /** Create a Redis client connecting to the given host and use it for caching
    */
  def apply[F[_]: Sync: MonadCancelThrow, V](
      host: String,
      port: Int
  )(implicit config: CacheConfig, codec: Codec[V]): RedisCache[F, V] =
    apply(new JedisPool(host, port))

  /** Create a cache that uses the given Jedis client pool
    * @param jedisPool
    *   a Jedis pool
    */
  def apply[F[_]: Sync: MonadCancelThrow, V](
      jedisPool: JedisPool
  )(implicit config: CacheConfig, codec: Codec[V]): RedisCache[F, V] =
    new RedisCache[F, V](jedisPool)

}

package scalacache.redis

import cats.effect.{MonadCancel, MonadCancelThrow, Sync}
import redis.clients.jedis._
import scalacache.serialization.binary.{BinaryCodec, BinaryEncoder}

import scala.language.higherKinds

/**
  * Thin wrapper around Jedis
  */
class RedisCache[F[_]: Sync: MonadCancelThrow, K, V](val jedisPool: JedisPool)(
    implicit val keyEncoder: BinaryEncoder[K],
    val codec: BinaryCodec[V]
) extends RedisCacheBase[F, K, V] {

  protected def F: Sync[F]                             = Sync[F]
  protected def MonadCancelThrowF: MonadCancelThrow[F] = MonadCancel[F, Throwable]
  type JClient = Jedis

  protected val doRemoveAll: F[Unit] = withJedis { jedis =>
    F.delay(jedis.flushDB())
  }
}

object RedisCache {

  /**
    * Create a Redis client connecting to the given host and use it for caching
    */
  def apply[F[_]: Sync: MonadCancelThrow, K, V](
      host: String,
      port: Int
  )(implicit keyEncoder: BinaryEncoder[K], codec: BinaryCodec[V]): RedisCache[F, K, V] =
    apply(new JedisPool(host, port))

  /**
    * Create a cache that uses the given Jedis client pool
    * @param jedisPool a Jedis pool
    */
  def apply[F[_]: Sync: MonadCancelThrow, K, V](
      jedisPool: JedisPool
  )(implicit keyEncoder: BinaryEncoder[K], codec: BinaryCodec[V]): RedisCache[F, K, V] =
    new RedisCache[F, K, V](jedisPool)

}

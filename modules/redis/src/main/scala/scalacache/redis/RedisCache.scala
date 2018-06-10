package scalacache.redis

import redis.clients.jedis._
import scalacache.{Async, CacheConfig}

import scala.language.higherKinds

/**
  * Thin wrapper around Jedis
  */
class RedisCache[F[_]](val jedisPool: JedisPool)(implicit val config: CacheConfig, F: Async[F])
    extends RedisCacheBase[F] {

  override type JClient = Jedis
  override type Underlying = JedisPool

  override final val underlying: Underlying = jedisPool

  override protected final def doRemoveAll(): F[Any] = F.delay {
    val jedis = jedisPool.getResource
    try {
      jedis.flushDB()
    } finally {
      jedis.close()
    }
  }

}

object RedisCache {

  /**
    * Create a Redis client connecting to the given host and use it for caching
    */
  def apply[F[_]: Async](host: String, port: Int)(implicit config: CacheConfig): RedisCache[F] =
    apply(new JedisPool(host, port))

  /**
    * Create a cache that uses the given Jedis client pool
    * @param jedisPool a Jedis pool
    */
  def apply[F[_]: Async](jedisPool: JedisPool)(implicit config: CacheConfig): RedisCache[F] =
    new RedisCache[F](jedisPool)

}

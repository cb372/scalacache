package scalacache.redis

import redis.clients.jedis._

import scala.language.higherKinds
import scalacache.{CacheConfig, Mode}

/**
  * Thin wrapper around Jedis
  */
class RedisCache[F[_]](val jedisPool: JedisPool)(implicit val config: CacheConfig, mode: Mode[F])
    extends RedisCacheBase[F] {

  type JClient = Jedis

  protected def doRemoveAll(): F[Any] = mode.M.delay {
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
  def apply[F[_]: Mode](host: String, port: Int)(implicit config: CacheConfig): RedisCache[F] =
    apply(new JedisPool(host, port))

  /**
    * Create a cache that uses the given Jedis client pool
    * @param jedisPool a Jedis pool
    */
  def apply[F[_]: Mode](jedisPool: JedisPool)(implicit config: CacheConfig): RedisCache[F] =
    new RedisCache[F](jedisPool)

}

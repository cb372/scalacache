package scalacache.redis

import redis.clients.jedis._

import scala.language.higherKinds
import scalacache.{CacheConfig, Mode}
import scalacache.serialization.Codec

/**
  * Thin wrapper around Jedis
  */
class RedisCache[V](val jedisPool: JedisPool)(implicit val config: CacheConfig, val codec: Codec[V])
    extends RedisCacheBase[V] {

  type JClient = Jedis

  protected def doRemoveAll[F[_]]()(implicit mode: Mode[F]): F[Any] = mode.M.delay {
    val jedis = jedisPool.getResource()
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
  def apply[V](host: String, port: Int)(implicit config: CacheConfig, codec: Codec[V]): RedisCache[V] =
    apply(new JedisPool(host, port))

  /**
    * Create a cache that uses the given Jedis client pool
    * @param jedisPool a Jedis pool
    */
  def apply[V](jedisPool: JedisPool)(implicit config: CacheConfig, codec: Codec[V]): RedisCache[V] =
    new RedisCache[V](jedisPool)

}

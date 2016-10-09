package scalacache.redis

import redis.clients.jedis._

/**
 * Thin wrapper around Jedis
 * @param customClassloader a classloader to use when deserializing objects from the cache.
 *                          If you are using Play and `useLegacySerialization` is true, you should pass in `app.classloader`.
 * @param useLegacySerialization set this to true to use Jedis's serialization mechanism
 *                               to maintain compatibility with ScalaCache 0.7.x or earlier.
 */
class RedisCache(val jedisPool: JedisPool,
                 override val customClassloader: Option[ClassLoader] = None,
                 override val useLegacySerialization: Boolean = false)
    extends RedisCacheBase {

  type JClient = Jedis

  override def removeAll() = {
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
  def apply(host: String, port: Int): RedisCache = apply(new JedisPool(host, port))

  /**
   * Create a cache that uses the given Jedis client pool
   * @param jedisPool a Jedis pool
   * @param customClassloader a classloader to use when deserializing objects from the cache.
   *                          If you are using Play, you should pass in `app.classloader`.
   */
  def apply(jedisPool: JedisPool, customClassloader: Option[ClassLoader] = None): RedisCache =
    new RedisCache(jedisPool, customClassloader)

}


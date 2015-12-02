package scalacache.redis

import redis.clients.jedis._
import scala.concurrent.{ Future, ExecutionContext, blocking }

/**
 * Thin wrapper around Jedis
 * @param customClassloader a classloader to use when deserializing objects from the cache.
 *                          If you are using Play, you should pass in `app.classloader`.
 */
class RedisCache(jedisPool: JedisPool, override val customClassloader: Option[ClassLoader] = None)(implicit val execContext: ExecutionContext = ExecutionContext.global)
    extends RedisCacheBase {

  override def removeAll() = Future {
    blocking {
      withJedisCommands { jedis =>
        val jedis = jedisPool.getResource()
        try {
          jedis.flushDB()
        } finally {
          jedis.close()
        }
      }
    }
  }

  override def close(): Unit = {
    jedisPool.close()
  }

  override def withJedisCommands[T](f: BinaryJedisCommands => T): T = {
    val jedis = jedisPool.getResource()
    try {
      f(jedis)
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


package scalacache.redis

import redis.clients.jedis._
import scala.concurrent.{Future, ExecutionContext, blocking}

/**
  * Thin wrapper around Jedis
  */
class RedisCache(val jedisPool: JedisPool)(implicit val execContext: ExecutionContext = ExecutionContext.global)
    extends RedisCacheBase {

  type JClient = Jedis

  override def removeAll() = Future {
    blocking {
      val jedis = jedisPool.getResource()
      try {
        jedis.flushDB()
      } finally {
        jedis.close()
      }
    }
  }

}

object RedisCache {

  /**
    * Create a Redis client connecting to the given host and use it for caching
    */
  def apply(host: String, port: Int): RedisCache =
    apply(new JedisPool(host, port))

  /**
    * Create a cache that uses the given Jedis client pool
    * @param jedisPool a Jedis pool
    */
  def apply(jedisPool: JedisPool): RedisCache =
    new RedisCache(jedisPool)

}

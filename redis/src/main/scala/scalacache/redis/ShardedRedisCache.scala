package scalacache.redis

import redis.clients.jedis._
import scala.concurrent.{ Future, ExecutionContext, blocking }
import scala.collection.JavaConverters._

class ShardedRedisCache(jedisPool: ShardedJedisPool, override val customClassloader: Option[ClassLoader] = None)(implicit val execContext: ExecutionContext = ExecutionContext.global)
    extends RedisCacheBase {

  override def removeAll() = Future {
    blocking {
      val jedis = jedisPool.getResource()
      try {
        jedis.getAllShards.asScala.foreach(_.flushDB())
      } finally {
        jedis.close()
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

object ShardedRedisCache {

  /**
   * Create a sharded Redis client connecting to the given hosts and use it for caching
   */
  def apply(hosts: (String, Int)*): ShardedRedisCache = {
    val shards = hosts.map { case (host, port) => new JedisShardInfo(host, port) }
    val pool = new ShardedJedisPool(new JedisPoolConfig(), shards.asJava)
    apply(pool)
  }

  /**
   * Create a cache that uses the given ShardedJedis client pool
   * @param jedisPool a ShardedJedis pool
   * @param customClassloader a classloader to use when deserializing objects from the cache.
   *                          If you are using Play, you should pass in `app.classloader`.
   */
  def apply(jedisPool: ShardedJedisPool, customClassloader: Option[ClassLoader] = None): ShardedRedisCache =
    new ShardedRedisCache(jedisPool, customClassloader)

}

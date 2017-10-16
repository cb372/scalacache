package scalacache.redis

import redis.clients.jedis._
import scala.concurrent.{Future, ExecutionContext, blocking}
import scala.collection.JavaConverters._

/**
  * Thin wrapper around Jedis that works with sharded Redis.
  */
class ShardedRedisCache(val jedisPool: ShardedJedisPool)(implicit val execContext: ExecutionContext =
                                                           ExecutionContext.global)
    extends RedisCacheBase {

  type JClient = ShardedJedis

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

}

object ShardedRedisCache {

  /**
    * Create a sharded Redis client connecting to the given hosts and use it for caching
    */
  def apply(hosts: (String, Int)*): ShardedRedisCache = {
    val shards = hosts.map {
      case (host, port) => new JedisShardInfo(host, port)
    }
    val pool = new ShardedJedisPool(new JedisPoolConfig(), shards.asJava)
    apply(pool)
  }

  /**
    * Create a cache that uses the given ShardedJedis client pool
    * @param jedisPool a ShardedJedis pool
    */
  def apply(jedisPool: ShardedJedisPool): ShardedRedisCache =
    new ShardedRedisCache(jedisPool)

}

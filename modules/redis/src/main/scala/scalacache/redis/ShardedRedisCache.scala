package scalacache.redis

import redis.clients.jedis._
import scalacache.{CacheConfig, Mode}

import scala.collection.JavaConverters._
import scala.language.higherKinds

/**
  * Thin wrapper around Jedis that works with sharded Redis.
  */
class ShardedRedisCache[F[_]](val jedisPool: ShardedJedisPool)(implicit val config: CacheConfig, mode: Mode[F])
    extends RedisCacheBase[F] {

  type JClient = ShardedJedis

  protected def doRemoveAll(): F[Any] = mode.M.delay {
    val jedis = jedisPool.getResource
    try {
      jedis.getAllShards.asScala.foreach(_.flushDB())
    } finally {
      jedis.close()
    }
  }

}

object ShardedRedisCache {

  /**
    * Create a sharded Redis client connecting to the given hosts and use it for caching
    */
  def apply[F[_]: Mode](hosts: (String, Int)*)(implicit config: CacheConfig): ShardedRedisCache[F] = {
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
  def apply[F[_]: Mode](jedisPool: ShardedJedisPool)(implicit config: CacheConfig): ShardedRedisCache[F] =
    new ShardedRedisCache[F](jedisPool)

}

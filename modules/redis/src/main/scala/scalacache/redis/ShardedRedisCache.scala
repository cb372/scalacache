package scalacache.redis

import redis.clients.jedis._

import scala.collection.JavaConverters._
import scala.language.higherKinds
import scalacache.{CacheConfig, Mode}
import scalacache.serialization.Codec

/**
  * Thin wrapper around Jedis that works with sharded Redis.
  */
class ShardedRedisCache[V](val jedisPool: ShardedJedisPool)(implicit val config: CacheConfig, val codec: Codec[V])
    extends RedisCacheBase[V] {

  type JClient = ShardedJedis

  protected def doRemoveAll[F[_]]()(implicit mode: Mode[F]): F[Any] = mode.M.delay {
    val jedis = jedisPool.getResource()
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
  def apply[V](hosts: (String, Int)*)(implicit config: CacheConfig, codec: Codec[V]): ShardedRedisCache[V] = {
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
  def apply[V](jedisPool: ShardedJedisPool)(implicit config: CacheConfig, codec: Codec[V]): ShardedRedisCache[V] =
    new ShardedRedisCache[V](jedisPool)

}

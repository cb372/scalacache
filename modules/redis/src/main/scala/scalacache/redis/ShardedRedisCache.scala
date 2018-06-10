package scalacache.redis

import redis.clients.jedis._
import scalacache.{Async, CacheConfig}

import scala.collection.JavaConverters._
import scala.language.higherKinds

/**
  * Thin wrapper around Jedis that works with sharded Redis.
  */
class ShardedRedisCache[F[_]](val jedisPool: ShardedJedisPool)(implicit val config: CacheConfig, F: Async[F])
    extends RedisCacheBase[F] {

  override type JClient = ShardedJedis
  override type Underlying = ShardedJedisPool

  override final val underlying: Underlying = jedisPool

  protected override final def doRemoveAll(): F[Any] = F.delay {
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
  def apply[F[_]: Async](hosts: (String, Int)*)(implicit config: CacheConfig): ShardedRedisCache[F] = {
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
  def apply[F[_]: Async](jedisPool: ShardedJedisPool)(implicit config: CacheConfig): ShardedRedisCache[F] =
    new ShardedRedisCache[F](jedisPool)

}

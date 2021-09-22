package scalacache.redis

import redis.clients.jedis._

import scala.collection.JavaConverters._
import scala.language.higherKinds
import scalacache.CacheConfig
import scalacache.serialization.Codec
import cats.effect.{MonadCancel, Sync}
import org.apache.commons.pool2.impl.GenericObjectPoolConfig

/** Thin wrapper around Jedis that works with sharded Redis.
  */
class ShardedRedisCache[F[_]: Sync, V](val jedisPool: ShardedJedisPool)(implicit
    val config: CacheConfig,
    val codec: Codec[V]
) extends RedisCacheBase[F, V] {

  protected def F: Sync[F] = Sync[F]

  type JClient = ShardedJedis

  protected val doRemoveAll: F[Unit] = withJedis { jedis =>
    F.delay {
      jedis.getAllShards.asScala.foreach(_.flushDB())
    }
  }

}

object ShardedRedisCache {

  /** Create a sharded Redis client connecting to the given hosts and use it for caching
    */
  def apply[F[_]: Sync, V](
      hosts: (String, Int)*
  )(implicit config: CacheConfig, codec: Codec[V]): ShardedRedisCache[F, V] = {
    val shards = hosts.map { case (host, port) =>
      new JedisShardInfo(host, port)
    }
    val pool = new ShardedJedisPool(new GenericObjectPoolConfig[ShardedJedis], shards.asJava)
    apply(pool)
  }

  /** Create a cache that uses the given ShardedJedis client pool
    * @param jedisPool
    *   a ShardedJedis pool
    */
  def apply[F[_]: Sync, V](
      jedisPool: ShardedJedisPool
  )(implicit config: CacheConfig, codec: Codec[V]): ShardedRedisCache[F, V] =
    new ShardedRedisCache[F, V](jedisPool)

}

package scalacache.redis

import cats.effect.Sync
import redis.clients.jedis._
import scalacache.serialization.binary.{BinaryCodec, BinaryEncoder}
import org.apache.commons.pool2.impl.GenericObjectPoolConfig

import scala.collection.JavaConverters._
import scala.language.higherKinds

/** Thin wrapper around Jedis that works with sharded Redis.
  */
class ShardedRedisCache[F[_]: Sync, K, V](val jedisPool: ShardedJedisPool)(implicit
    val keyEncoder: BinaryEncoder[K],
    val codec: BinaryCodec[V]
) extends RedisCacheBase[F, K, V] {

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
  def apply[F[_]: Sync, K, V](
      hosts: (String, Int)*
  )(implicit keyEncoder: BinaryEncoder[K], codec: BinaryCodec[V]): ShardedRedisCache[F, K, V] = {
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
  def apply[F[_]: Sync, K, V](
      jedisPool: ShardedJedisPool
  )(implicit keyEncoder: BinaryEncoder[K], codec: BinaryCodec[V]): ShardedRedisCache[F, K, V] =
    new ShardedRedisCache[F, K, V](jedisPool)

}

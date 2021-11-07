package scalacache.redis

import cats.effect.{MonadCancel, Sync}
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import redis.clients.jedis._
import scalacache.serialization.binary.{BinaryCodec, BinaryEncoder}

import scala.collection.JavaConverters._

/** Thin wrapper around Jedis that works with Redis Sentinel.
  */
class SentinelRedisCache[F[_]: Sync, K, V](val jedisPool: JedisSentinelPool)(implicit
    val keyEncoder: BinaryEncoder[K],
    val codec: BinaryCodec[V]
) extends RedisCacheBase[F, K, V] {

  protected def F: Sync[F] = Sync[F]

  type JClient = Jedis

  protected def doRemoveAll: F[Unit] =
    withJedis { jedis =>
      F.delay(jedis.flushDB())
    }

}

object SentinelRedisCache {

  /** Create a `SentinelRedisCache` that uses a `JedisSentinelPool` with a default pool config.
    *
    * @param clusterName
    *   Name of the redis cluster
    * @param sentinels
    *   set of sentinels in format [host1:port, host2:port]
    * @param password
    *   password of the cluster
    */
  def apply[F[_]: Sync, K, V](clusterName: String, sentinels: Set[String], password: String)(implicit
      keyEncoder: BinaryEncoder[K],
      codec: BinaryCodec[V]
  ): SentinelRedisCache[F, K, V] =
    apply(new JedisSentinelPool(clusterName, sentinels.asJava, new GenericObjectPoolConfig[Jedis], password))

  /** Create a `SentinelRedisCache` that uses a `JedisSentinelPool` with a custom pool config.
    *
    * @param clusterName
    *   Name of the redis cluster
    * @param sentinels
    *   set of sentinels in format [host1:port, host2:port]
    * @param password
    *   password of the cluster
    * @param poolConfig
    *   config of the underlying pool
    */
  def apply[F[_]: Sync, K, V](
      clusterName: String,
      sentinels: Set[String],
      poolConfig: GenericObjectPoolConfig[Jedis],
      password: String
  )(implicit
      keyEncoder: BinaryEncoder[K],
      codec: BinaryCodec[V]
  ): SentinelRedisCache[F, K, V] =
    apply(new JedisSentinelPool(clusterName, sentinels.asJava, poolConfig, password))

  /** Create a `SentinelRedisCache` that uses the given JedisSentinelPool
    *
    * @param jedisSentinelPool
    *   a JedisSentinelPool
    */
  def apply[F[_]: Sync, K, V](
      jedisSentinelPool: JedisSentinelPool
  )(implicit keyEncoder: BinaryEncoder[K], codec: BinaryCodec[V]): SentinelRedisCache[F, K, V] =
    new SentinelRedisCache[F, K, V](jedisSentinelPool)

}

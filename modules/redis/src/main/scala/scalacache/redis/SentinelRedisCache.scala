package scalacache.redis

import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import redis.clients.jedis._

import scala.collection.JavaConverters._
import scalacache.CacheConfig
import scalacache.serialization.Codec
import cats.implicits._
import cats.effect.{MonadCancel, MonadCancelThrow, Sync}

/** Thin wrapper around Jedis that works with Redis Sentinel.
  */
class SentinelRedisCache[F[_]: Sync: MonadCancelThrow, V](val jedisPool: JedisSentinelPool)(implicit
    val config: CacheConfig,
    val codec: Codec[V]
) extends RedisCacheBase[F, V] {

  protected def F: Sync[F]                             = Sync[F]
  protected def MonadCancelThrowF: MonadCancelThrow[F] = MonadCancel[F, Throwable]

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
  def apply[F[_]: Sync: MonadCancelThrow, V](clusterName: String, sentinels: Set[String], password: String)(implicit
      config: CacheConfig,
      codec: Codec[V]
  ): SentinelRedisCache[F, V] =
    apply(new JedisSentinelPool(clusterName, sentinels.asJava, new GenericObjectPoolConfig, password))

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
  def apply[F[_]: Sync: MonadCancelThrow, V](
      clusterName: String,
      sentinels: Set[String],
      poolConfig: GenericObjectPoolConfig,
      password: String
  )(implicit
      config: CacheConfig,
      codec: Codec[V]
  ): SentinelRedisCache[F, V] =
    apply(new JedisSentinelPool(clusterName, sentinels.asJava, poolConfig, password))

  /** Create a `SentinelRedisCache` that uses the given JedisSentinelPool
    *
    * @param jedisSentinelPool
    *   a JedisSentinelPool
    */
  def apply[F[_]: Sync: MonadCancelThrow, V](
      jedisSentinelPool: JedisSentinelPool
  )(implicit config: CacheConfig, codec: Codec[V]): SentinelRedisCache[F, V] =
    new SentinelRedisCache[F, V](jedisSentinelPool)

}

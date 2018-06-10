package scalacache.redis

import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import redis.clients.jedis._
import scalacache.{Async, CacheConfig}

import scala.collection.JavaConverters._
import scala.language.higherKinds

/**
  * Thin wrapper around Jedis that works with Redis Sentinel.
  */
class SentinelRedisCache[F[_]](val jedisPool: JedisSentinelPool)(implicit val config: CacheConfig, F: Async[F])
    extends RedisCacheBase[F] {

  override type JClient = Jedis
  override type Underlying = JedisSentinelPool

  override val underlying: Underlying = jedisPool

  protected def doRemoveAll(): F[Any] = F.delay {
    val jedis = jedisPool.getResource
    try {
      jedis.flushDB()
    } finally {
      jedis.close()
    }
  }

}

object SentinelRedisCache {

  /**
    * Create a `SentinelRedisCache` that uses a `JedisSentinelPool` with a default pool config.
    *
    * @param clusterName Name of the redis cluster
    * @param sentinels set of sentinels in format [host1:port, host2:port]
    * @param password password of the cluster
    */
  def apply[F[_]: Async](clusterName: String, sentinels: Set[String], password: String)(
      implicit config: CacheConfig): SentinelRedisCache[F] =
    apply(new JedisSentinelPool(clusterName, sentinels.asJava, new GenericObjectPoolConfig, password))

  /**
    * Create a `SentinelRedisCache` that uses a `JedisSentinelPool` with a custom pool config.
    *
    * @param clusterName Name of the redis cluster
    * @param sentinels set of sentinels in format [host1:port, host2:port]
    * @param password password of the cluster
    * @param poolConfig config of the underlying pool
    */
  def apply[F[_]: Async](clusterName: String,
                         sentinels: Set[String],
                         poolConfig: GenericObjectPoolConfig,
                         password: String)(implicit config: CacheConfig): SentinelRedisCache[F] =
    apply(new JedisSentinelPool(clusterName, sentinels.asJava, poolConfig, password))

  /**
    * Create a `SentinelRedisCache` that uses the given JedisSentinelPool
    *
    * @param jedisSentinelPool a JedisSentinelPool
    */
  def apply[F[_]: Async](jedisSentinelPool: JedisSentinelPool)(implicit config: CacheConfig): SentinelRedisCache[F] =
    new SentinelRedisCache[F](jedisSentinelPool)

}

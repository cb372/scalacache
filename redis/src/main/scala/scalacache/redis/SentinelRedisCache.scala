package scalacache.redis

import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import redis.clients.jedis._

import scala.collection.JavaConverters._

/**
 * Thin wrapper around Jedis that works with Redis Sentinel.
 *
 * @param customClassloader a classloader to use when deserializing objects from the cache.
 *                          If you are using Play and `useLegacySerialization` is true, you should pass in `app.classloader`.
 * @param useLegacySerialization set this to true to use Jedis's serialization mechanism
 *                               to maintain compatibility with ScalaCache 0.7.x or earlier.
 */
class SentinelRedisCache(val jedisPool: JedisSentinelPool,
                         override val customClassloader: Option[ClassLoader] = None,
                         override val useLegacySerialization: Boolean = false)
    extends RedisCacheBase {

  type JClient = Jedis

  override def removeAll() = {
    val jedis = jedisPool.getResource()
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
  def apply(clusterName: String, sentinels: Set[String], password: String): SentinelRedisCache =
    apply(new JedisSentinelPool(clusterName, sentinels.asJava, new GenericObjectPoolConfig, password))

  /**
   * Create a `SentinelRedisCache` that uses a `JedisSentinelPool` with a custom pool config.
   *
   * @param clusterName Name of the redis cluster
   * @param sentinels set of sentinels in format [host1:port, host2:port]
   * @param password password of the cluster
   * @param poolConfig config of the underlying pool
   */
  def apply(clusterName: String, sentinels: Set[String], poolConfig: GenericObjectPoolConfig, password: String): SentinelRedisCache =
    apply(new JedisSentinelPool(clusterName, sentinels.asJava, poolConfig, password))

  /**
   * Create a `SentinelRedisCache` that uses the given JedisSentinelPool
   *
   * @param jedisSentinelPool a JedisSentinelPool
   * @param customClassloader a classloader to use when deserializing objects from the cache.
   *                          If you are using Play, you should pass in `app.classloader`.
   */
  def apply(jedisSentinelPool: JedisSentinelPool, customClassloader: Option[ClassLoader] = None): SentinelRedisCache =
    new SentinelRedisCache(jedisSentinelPool, customClassloader)

}


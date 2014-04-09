package cacheable.redis

import com.redis.RedisClient
import cacheable.{LoggingSupport, Cache}
import scala.concurrent.duration._
import com.typesafe.scalalogging.slf4j.StrictLogging

/**
 * Author: chris
 * Created: 11/16/13
 */
class RedisCache(client: RedisClient)
    extends Cache
    with RedisSerialization
    with LoggingSupport
    with StrictLogging {

  /**
   * Get the value corresponding to the given key from the cache
   * @param key cache key
   * @tparam V the type of the corresponding value
   * @return the value, if there is one
   */
  def get[V](key: String): Option[V] = {
    val result = client.get[V](key)
    logCacheHitOrMiss(key, result)
    result
  }

  /**
   * Insert the given key-value pair into the cache, with an optional Time To Live.
   * @param key cache key
   * @param value corresponding value
   * @param ttl Time To Live
   * @tparam V the type of the corresponding value
   */
  def put[V](key: String, value: V, ttl: Option[Duration]): Unit = ttl match {
    case None => client.set(key, value)
    case Some(Duration.Zero) => client.set(key, value)
    case Some(d) if d < 1.second => {
      logger.warn("Because Redis (pre 2.6.12) does not support sub-second expiry, TTL of $d will be rounded up to 1 second")
      client.setex(key, 1, value)
    }
    case Some(d) => client.setex(key, d.toSeconds.toInt, value)
  }

}

object RedisCache {

  /**
   * Create a Redis client connecting to the given host(s) and use it for caching
   */
  def apply(host: String, port: Int): RedisCache = apply(new RedisClient(host, port))

  /**
   * Create a cache that uses the given Redis client
   * @param client a Redis client
   */
  def apply(client: RedisClient): RedisCache = new RedisCache(client)

}


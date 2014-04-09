package cacheable.redis

import cacheable.{ LoggingSupport, Cache }
import scala.concurrent.duration._
import com.typesafe.scalalogging.slf4j.StrictLogging
import redis.clients.jedis.Jedis
import java.nio.charset.Charset

/**
 * Author: chris
 * Created: 11/16/13
 */
class RedisCache(client: Jedis)
    extends Cache
    with RedisSerialization
    with LoggingSupport
    with StrictLogging {

  private val utf8 = Charset.forName("UTF-8")

  /**
   * Get the value corresponding to the given key from the cache
   * @param key cache key
   * @tparam V the type of the corresponding value
   * @return the value, if there is one
   */
  def get[V](key: String): Option[V] = {
    val keyBytes = key.getBytes(utf8)
    val resultBytes = Option(client.get(keyBytes))
    val result = resultBytes.map(deserialize[V])
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
  def put[V](key: String, value: V, ttl: Option[Duration]): Unit = {
    val keyBytes = key.getBytes(utf8)
    val valueBytes = serialize(value)
    ttl match {
      case None => client.set(keyBytes, valueBytes)
      case Some(Duration.Zero) => client.set(keyBytes, valueBytes)
      case Some(d) if d < 1.second => {
        logger.warn("Because Redis (pre 2.6.12) does not support sub-second expiry, TTL of $d will be rounded up to 1 second")
        client.setex(keyBytes, 1, valueBytes)
      }
      case Some(d) => client.setex(keyBytes, d.toSeconds.toInt, valueBytes)
    }
  }

}

object RedisCache {

  /**
   * Create a Redis client connecting to the given host and use it for caching
   */
  def apply(host: String, port: Int): RedisCache = apply(new Jedis(host, port))

  /**
   * Create a cache that uses the given Jedis client
   * @param client a Jedis client
   */
  def apply(client: Jedis): RedisCache = new RedisCache(client)

}


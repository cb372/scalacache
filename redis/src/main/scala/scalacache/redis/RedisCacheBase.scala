package scalacache.redis

import redis.clients.jedis._
import scala.concurrent.{ ExecutionContext, Future, blocking }
import scalacache.{ LoggingSupport, Cache }
import scala.concurrent.duration._
import com.typesafe.scalalogging.StrictLogging

/**
 * Contains implementations of all methods that can be implemented independent of the type of Redis client.
 */
trait RedisCacheBase
    extends Cache
    with RedisSerialization
    with LoggingSupport
    with StrictLogging {

  import StringEnrichment.StringWithUtf8Bytes

  implicit val execContext: ExecutionContext

  protected def withJedisCommands[T](f: BinaryJedisCommands => T): T

  /**
   * Get the value corresponding to the given key from the cache
   * @param key cache key
   * @tparam V the type of the corresponding value
   * @return the value, if there is one
   */
  final override def get[V](key: String) = Future {
    blocking {
      withJedisCommands { jedis =>
        val resultBytes = Option(jedis.get(key.utf8bytes))
        val result = resultBytes.map(deserialize[V])
        logCacheHitOrMiss(key, result)
        result
      }
    }
  }

  /**
   * Insert the given key-value pair into the cache, with an optional Time To Live.
   * @param key cache key
   * @param value corresponding value
   * @param ttl Time To Live
   * @tparam V the type of the corresponding value
   */
  final override def put[V](key: String, value: V, ttl: Option[Duration]) = Future {
    blocking {
      withJedisCommands { jedis =>
        val keyBytes = key.utf8bytes
        val valueBytes = serialize(value)
        ttl match {
          case None => jedis.set(keyBytes, valueBytes)
          case Some(Duration.Zero) => jedis.set(keyBytes, valueBytes)
          case Some(d) if d < 1.second =>
            logger.warn("Because Redis (pre 2.6.12) does not support sub-second expiry, TTL of $d will be rounded up to 1 second")
            jedis.setex(keyBytes, 1, valueBytes)
          case Some(d) => jedis.setex(keyBytes, d.toSeconds.toInt, valueBytes)
        }
      }
    }
  }

  /**
   * Remove the given key and its associated value from the cache, if it exists.
   * If the key is not in the cache, do nothing.
   * @param key cache key
   */
  final override def remove(key: String) = Future {
    blocking {
      withJedisCommands { jedis =>
        jedis.del(key.utf8bytes)
      }
    }
  }

}

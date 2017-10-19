package scalacache.redis

import java.io.Closeable

import org.slf4j.LoggerFactory
import redis.clients.jedis._
import redis.clients.util.Pool

import scalacache.serialization.Codec
import scalacache.{AbstractCache, CacheConfig, Mode}
import scala.concurrent.duration._
import scala.language.higherKinds

/**
  * Contains implementations of all methods that can be implemented independent of the type of Redis client.
  * This is everything apart from `removeAll`, which needs to be implemented differently for sharded Redis.
  */
trait RedisCacheBase[V] extends AbstractCache[V] {

  override protected final val logger =
    LoggerFactory.getLogger(getClass.getName)

  import StringEnrichment.StringWithUtf8Bytes

  protected type JClient <: BinaryJedisCommands with Closeable

  protected def jedisPool: Pool[JClient]

  protected def config: CacheConfig

  protected def codec: Codec[V, Array[Byte]]

  protected def doGet[F[_]](key: String)(implicit mode: Mode[F]): F[Option[V]] = mode.M.delay {
    withJedisCommands { jedis =>
      val bytes = jedis.get(key.utf8bytes)
      val result = {
        if (bytes != null) {
          Some(codec.deserialize(bytes))
        } else None
      }
      logCacheHitOrMiss(key, result)
      result
    }
  }

  protected def doPut[F[_]](key: String, value: V, ttl: Option[Duration])(implicit mode: Mode[F]): F[Any] =
    mode.M.delay {
      withJedisCommands { jedis =>
        val keyBytes = key.utf8bytes
        val valueBytes = codec.serialize(value)
        ttl match {
          case None => jedis.set(keyBytes, valueBytes)
          case Some(Duration.Zero) => jedis.set(keyBytes, valueBytes)
          case Some(d) if d < 1.second =>
            if (logger.isWarnEnabled) {
              logger.warn(
                "Because Redis (pre 2.6.12) does not support sub-second expiry, TTL of $d will be rounded up to 1 second")
            }
            jedis.setex(keyBytes, 1, valueBytes)
          case Some(d) =>
            jedis.setex(keyBytes, d.toSeconds.toInt, valueBytes)
        }
      }
    }

  protected def doRemove[F[_]](key: String)(implicit mode: Mode[F]): F[Any] = mode.M.delay {
    withJedisCommands { jedis =>
      jedis.del(key.utf8bytes)
    }
  }

  def close[F[_]]()(implicit mode: Mode[F]): F[Any] = mode.M.delay(jedisPool.close())

  /**
    * Borrow a Jedis client from the pool, perform some operation and then return the client to the pool.
    *
    * @param f block that uses the Jedis client
    * @tparam T return type of the block
    * @return the result of executing the block
    */
  protected final def withJedisCommands[T](f: BinaryJedisCommands => T): T = {
    val jedis = jedisPool.getResource()
    try {
      f(jedis)
    } finally {
      jedis.close()
    }
  }

}

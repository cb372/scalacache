package scalacache.redis

import redis.clients.jedis.JedisCluster
import redis.clients.jedis.exceptions.JedisClusterException
import scalacache.logging.Logger
import scalacache.redis.StringEnrichment._
import scalacache.serialization.Codec
import scalacache.{AbstractCache, CacheConfig}

import scala.concurrent.duration.{Duration, _}
import cats.implicits._
import cats.effect.Sync

class RedisClusterCache[F[_]: Sync, V](val jedisCluster: JedisCluster)(implicit
    val config: CacheConfig,
    val codec: Codec[V]
) extends AbstractCache[F, V] {

  protected def F: Sync[F] = Sync[F]

  override protected final val logger = Logger.getLogger(getClass.getName)

  override protected def doGet(key: String): F[Option[V]] = F.defer {
    val bytes = jedisCluster.get(key.utf8bytes)
    val result: Codec.DecodingResult[Option[V]] = {
      if (bytes != null)
        codec.decode(bytes).right.map(Some(_))
      else
        Right(None)
    }

    result match {
      case Left(e) =>
        F.raiseError[Option[V]](e)
      case Right(maybeValue) =>
        logCacheHitOrMiss(key, maybeValue).as(maybeValue)
    }
  }

  override protected def doPut(key: String, value: V, ttl: Option[Duration]): F[Unit] = {
    val keyBytes   = key.utf8bytes
    val valueBytes = codec.encode(value)
    ttl match {
      case None                => F.delay(jedisCluster.set(keyBytes, valueBytes))
      case Some(Duration.Zero) => F.delay(jedisCluster.set(keyBytes, valueBytes))
      case Some(d) if d < 1.second =>
        logger.ifWarnEnabled {
          logger.warn(
            s"Because Redis (pre 2.6.12) does not support sub-second expiry, TTL of $d will be rounded up to 1 second"
          )
        } *> F.delay(jedisCluster.setex(keyBytes, 1, valueBytes))
      case Some(d) =>
        F.delay(jedisCluster.setex(keyBytes, d.toSeconds.toInt, valueBytes))
    }
  }

  override protected def doRemove(key: String): F[Unit] = F.delay {
    jedisCluster.del(key.utf8bytes)
  }

  @deprecated(
    "JedisCluster doesn't support this operation, scheduled to be removed with the next jedis major release",
    "0.28.0"
  )
  override protected def doRemoveAll: F[Unit] = F.raiseError {
    new JedisClusterException("No way to dispatch this command to Redis Cluster.")
  }

  override val close: F[Unit] = F.delay(jedisCluster.close())
}

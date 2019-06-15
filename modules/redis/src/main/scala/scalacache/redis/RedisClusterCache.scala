package scalacache.redis

import redis.clients.jedis.JedisCluster
import redis.clients.jedis.exceptions.JedisClusterException
import scalacache.logging.Logger
import scalacache.redis.StringEnrichment._
import scalacache.serialization.Codec
import scalacache.{AbstractCache, CacheConfig, Mode}

import scala.concurrent.duration.{Duration, _}
import scala.language.higherKinds

class RedisClusterCache[V](val jedisCluster: JedisCluster)(implicit val config: CacheConfig, val codec: Codec[V])
    extends AbstractCache[V] {

  override protected final val logger = Logger.getLogger(getClass.getName)

  override protected def doGet[F[_]](key: String)(implicit mode: Mode[F]): F[Option[V]] = mode.M.suspend {
    val bytes = jedisCluster.get(key.utf8bytes)
    val result: Codec.DecodingResult[Option[V]] = {
      if (bytes != null)
        codec.decode(bytes).right.map(Some(_))
      else
        Right(None)
    }
    result match {
      case Left(e) =>
        mode.M.raiseError(e)
      case Right(maybeValue) =>
        logCacheHitOrMiss(key, maybeValue)
        mode.M.pure(maybeValue)
    }
  }

  override protected def doPut[F[_]](key: String, value: V, ttl: Option[Duration])(implicit mode: Mode[F]): F[Any] = {
    mode.M.delay {
      val keyBytes   = key.utf8bytes
      val valueBytes = codec.encode(value)
      ttl match {
        case None                => jedisCluster.set(keyBytes, valueBytes)
        case Some(Duration.Zero) => jedisCluster.set(keyBytes, valueBytes)
        case Some(d) if d < 1.second =>
          if (logger.isWarnEnabled) {
            logger.warn(
              s"Because Redis (pre 2.6.12) does not support sub-second expiry, TTL of $d will be rounded up to 1 second"
            )
          }
          jedisCluster.setex(keyBytes, 1, valueBytes)
        case Some(d) =>
          jedisCluster.setex(keyBytes, d.toSeconds.toInt, valueBytes)
      }
    }
  }

  override protected def doRemove[F[_]](key: String)(implicit mode: Mode[F]): F[Any] = mode.M.delay {
    jedisCluster.del(key.utf8bytes)
  }

  @deprecated(
    "JedisCluster doesn't support this operation, scheduled to be removed with the next jedis major release",
    "0.28.0"
  )
  override protected def doRemoveAll[F[_]]()(implicit mode: Mode[F]): F[Any] = mode.M.raiseError {
    new JedisClusterException("No way to dispatch this command to Redis Cluster.")
  }

  override def close[F[_]]()(implicit mode: Mode[F]): F[Any] = mode.M.delay(jedisCluster.close())
}
